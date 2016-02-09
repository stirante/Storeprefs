package com.stirante.storeprefs;

import android.app.Application;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.stirante.storeprefs.utils.AppInfo;
import com.stirante.storeprefs.utils.SimpleDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.*;

/**
 * Created by stirante
 */
public class StorePrefsModule implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private final HashMap<String, Object> listeners = new HashMap<>();
    private boolean initialized = false;
    private XSharedPreferences prefs;
    private HashMap<String, Integer> dontUpdate;
    private boolean debug = false;
    private String installing = null;

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        XSharedPreferences enabled_modules = new XSharedPreferences(Commons.XPOSED_INSTALLER_PACKAGE, "enabled_modules");
        enabled_modules.makeWorldReadable();
        for (String s : enabled_modules.getAll().keySet()) {
            if (enabled_modules.getInt(s, 0) == 1) {
                listeners.put(s, null);
            }
        }
        prefs = new XSharedPreferences(Commons.MODULE_PACKAGE);
        prefs.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam packageParam) throws Throwable {
        if (packageParam.packageName.startsWith(Commons.PLAYSTORE_PACKAGE)) {
            prefs.makeWorldReadable();
            prefs.reload();
            findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    if (!initialized)
                        loadModules(context);
                    try {
                        hookMethods(packageParam, context);
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                        if (checkLuckyPatcher(context)) XposedBridge.log("LuckyPatcher");
                    }
                }
            });
        }
    }

    private void loadModules(Context ctx) {
        for (final String s : listeners.keySet()) {
            try {
                if (listeners.get(s) != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            callMethod(listeners.get(s), "init");
                        }
                    }).start();
                } else {
                    ApplicationInfo app = ctx.getPackageManager().getApplicationInfo(s, PackageManager.GET_META_DATA);
                    if (app.metaData.containsKey(Commons.METADATA_MAINCLASS)) {
                        PathClassLoader pathClassLoader = new dalvik.system.PathClassLoader(new File(app.publicSourceDir).getAbsolutePath(), ClassLoader.getSystemClassLoader());
                        try {
                            Class<?> clz = Class.forName(app.metaData.getString(Commons.METADATA_MAINCLASS), true, pathClassLoader);
                            final Object listener = clz.newInstance();
                            debug("Loaded class " + clz.toString());
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    callMethod(listener, "init");
                                }
                            }).start();
                            listeners.put(s, listener);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        initialized = true;
    }

    private void hookMethods(final XC_LoadPackage.LoadPackageParam packageParam, final Context ctx) throws Throwable {
        final ClassLoader loader = packageParam.classLoader;
        SimpleDatabase.load();
        debug = prefs.getBoolean(Commons.PREFERENCE_DEBUG, false);
        dontUpdate = (HashMap<String, Integer>) SimpleDatabase.get(Commons.DATA_DONT_UPDATE, new HashMap<String, Integer>());
        Class<?> adapterClass = findClass(Commons.CLASS_MY_APPS_INSTALLED_ADAPTER, loader);
        Class<?> docClass = findClass(Commons.CLASS_DOCUMENT, loader);
        Class<?> installPoliciesClass = findClass(Commons.CLASS_INSTALL_POLICIES, loader);
        findAndHookMethod(adapterClass, "access$300$46a91253", adapterClass, docClass, View.class, ViewGroup.class, findClass(Commons.CLASS_PLAYSTORE_UI_ELEMENT_NODE, loader), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View inflate = (View) param.getResult();
                final Object adapter = param.args[0];
                Object doc = param.args[1];
                if ((boolean) callMethod(doc, "hasDetails")) {
                    Object appDetails = callMethod(doc, "getAppDetails");
                    final String packageName = (String) getObjectField(appDetails, "packageName");
                    final int versionCode = (int) getObjectField(appDetails, "versionCode");
                    Object packageState = callMethod(getObjectField(getObjectField(adapter, "mAppStates"), "mPackageManager"), "get", packageName);
                    int localVersion = getIntField(packageState, "installedVersion");
                    if (localVersion < versionCode && (!dontUpdate.containsKey(packageName) || dontUpdate.get(packageName) < versionCode)) {
                        inflate.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                debug("Ignoring update for " + packageName + " since " + versionCode);
                                dontUpdate.put(packageName, versionCode);
                                SimpleDatabase.saveAsync();
                                callMethod(adapter, "notifyDataSetInvalidated");
                                Toast.makeText(ctx, "Adding to ignored updates", Toast.LENGTH_LONG).show();
                                return true;
                            }
                        });
                    }
                }
            }
        });
        findAndHookMethod(installPoliciesClass, "canUpdateApp", findClass(Commons.CLASS_PACKAGE_STATE, loader), docClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if ((boolean) param.getResult()) {
                    Object doc = param.args[1];
                    if ((boolean) callMethod(doc, "hasDetails")) {
                        Object appDetails = callMethod(doc, "getAppDetails");
                        String packageName = (String) getObjectField(appDetails, "packageName");
                        int versionCode = (int) getObjectField(appDetails, "versionCode");
                        if (dontUpdate.containsKey(packageName) && dontUpdate.get(packageName) >= versionCode) {
                            param.setResult(false);
                        }
                    }
                }
            }
        });
        //warn user
        if (prefs.getBoolean(Commons.PREFERENCE_WARNING, false)) {
            findAndHookMethod(Commons.CLASS_LIGHT_PURCHASE_FLOW_ACTIVITY, loader, "acquire", Bundle.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object warnCompatibility = getAdditionalInstanceField(param.thisObject, Commons.ADD_FIELD_WARN_COMPATIBILITY);
                    if (warnCompatibility != null && !((boolean) warnCompatibility)) return;
                    Object doc = getObjectField(param.thisObject, "mDoc");
                    if ((boolean) callMethod(doc, "hasDetails")) {
                        Object appDetails = callMethod(doc, "getAppDetails");
                        String packageName = (String) getObjectField(appDetails, "packageName");
                        String versionString = (String) getObjectField(appDetails, "versionString");
                        int versionCode = (int) getObjectField(appDetails, "versionCode");
                        if (!shouldUserUpdate(packageName, versionCode, versionString)) {
                            setAdditionalInstanceField(param.thisObject, Commons.ADD_FIELD_WARN_COMPATIBILITY, true);
                            Object dialog = newInstance(findClass(Commons.CLASS_DOWNLOAD_NETWORK_DIALOG_FRAGMENT, loader));
                            Bundle arguments = new Bundle();
                            arguments.putBoolean(Commons.ADD_FIELD_WARN_COMPATIBILITY, true);
                            callMethod(dialog, "setArguments", arguments);
                            callMethod(dialog, "show", callMethod(param.thisObject, "getSupportFragmentManager"), "LightPurchaseFlowActivity.errorDialog");
                            param.setResult(null);
                        }
                    }
                }
            });
            //change dialog appearance
            findAndHookMethod(Commons.CLASS_DOWNLOAD_NETWORK_DIALOG_FRAGMENT, loader, "onCreateDialog", Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    Bundle arguments = (Bundle) getObjectField(param.thisObject, "mArguments");
                    if (arguments.getBoolean(Commons.ADD_FIELD_WARN_COMPATIBILITY, false)) {
                        Context context = (Context) callMethod(param.thisObject, "getActivity");
                        Object builder = newInstance(findClass(Commons.CLASS_ALERT_DIALOG_BUILDER_COMPAT, loader), new Class[]{Context.class, byte.class}, context, (byte) 0);
                        Context myContext = context.createPackageContext(Commons.MODULE_PACKAGE, Context.CONTEXT_IGNORE_SECURITY);
                        callMethod(builder, "setTitle", new Class[]{CharSequence.class}, myContext.getResources().getString(R.string.title));
                        View content = LayoutInflater.from(myContext).inflate(R.layout.warning_install, null);
                        callMethod(builder, "setView", content);
                        callMethod(builder, "setPositiveButton", new Class[]{CharSequence.class, DialogInterface.OnClickListener.class}, myContext.getResources().getString(R.string.update), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Object activity = callMethod(param.thisObject, "getListener");
                                setAdditionalInstanceField(activity, Commons.ADD_FIELD_WARN_COMPATIBILITY, false);
                                callMethod(activity, "onDownloadOk", false, false);
                            }
                        });
                        callMethod(builder, "setNegativeButton", new Class[]{CharSequence.class, DialogInterface.OnClickListener.class}, myContext.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                callMethod(callMethod(param.thisObject, "getListener"), "onDownloadCancel");
                            }
                        });
                        Dialog dialog = (Dialog) callMethod(builder, "create");

                        param.setResult(dialog);
                    }
                }
            });
        }
        //disable auto update
        findAndHookMethod(installPoliciesClass, "getUpdateWarningsForDocument", docClass, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object warnings = param.getResult();
                Object doc = param.args[0];
                if ((boolean) callMethod(doc, "hasDetails")) {
                    Object appDetails = callMethod(doc, "getAppDetails");
                    String packageName = (String) getObjectField(appDetails, "packageName");
                    int versionCode = (int) getObjectField(appDetails, "versionCode");
                    if (!canAutoUpdate(packageName, versionCode))
                        setBooleanField(warnings, "autoUpdateDisabled", true);
                }
            }
        });
        if (prefs.getBoolean(Commons.PREFERENCE_RAPID, false)) {
            //disable rapid update
            findAndHookMethod(Commons.CLASS_RAPID_AUTO_UPATE_POLICY, loader, "apply", findClass(Commons.CLASS_AUTO_UPDATE_ENTRY, loader), XC_MethodReplacement.DO_NOTHING);
        }
        findAndHookMethod(Commons.CLASS_APP_STATES, loader, "getApp", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (installing != null && param.args[0].equals(installing)) {
                    try {
                        setIntField(getObjectField(param.getResult(), "packageManagerState"), "installedVersion", 1);
                        setIntField(getObjectField(param.getResult(), "installerData"), "desiredVersion", 1);
                    } catch (Throwable t) {
                        //shhhh...
                    }
                    installing = null;
                }
            }
        });
        IntentFilter iF = new IntentFilter(Commons.ACTION_INSTALL);
        ctx.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (prefs.getBoolean(Commons.PREFERENCE_INTENTS, false)) {
                    intent.setAction(Commons.ACTION_INSTALL_RESULT);
                    intent.putExtra("success", false);
                    context.sendBroadcast(intent);
                    return;
                }
                Object finsky = callStaticMethod(findClass(Commons.CLASS_FINSKY_APP, loader), "get");
                Object installer = getObjectField(finsky, "mInstaller");
                String packageName = intent.getExtras().getString("packageName");
                String title = intent.getExtras().getString("title");
                int versionCode = intent.getExtras().getInt("versionCode");
                callMethod(installer, "setVisibility", packageName, true, true, true);
                String account = (String) callStaticMethod(findClass(Commons.CLASS_APP_DETAILS_ACCOUNT, loader), "getAppDetailsAccount", packageName, callMethod(finsky, "getCurrentAccountName"), getObjectField(finsky, "mAppStates"), getObjectField(finsky, "mLibraries"));
                installing = packageName;//this is so stupid that i don't believe i'm actually doing this
                callMethod(installer, "requestInstall", packageName, versionCode, account, title, false, "storeprefs", 1, 0);
                debug("(Install intent) Installing " + packageName + " version code: " + versionCode);
                intent.setAction(Commons.ACTION_INSTALL_RESULT);
                intent.putExtra("success", true);
                context.sendBroadcast(intent);
            }
        }, iF);
        iF = new IntentFilter(Commons.ACTION_RESTORE);
        ctx.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (prefs.getBoolean(Commons.PREFERENCE_INTENTS, false)) {
                    intent.setAction(Commons.ACTION_INSTALL_RESULT);
                    intent.putExtra("success", false);
                    context.sendBroadcast(intent);
                    return;
                }
                List<AppInfo> appList = (List<AppInfo>) SimpleDatabase.get(Commons.DATA_RESTORE, new ArrayList<AppInfo>());
                Object finsky = callStaticMethod(findClass(Commons.CLASS_FINSKY_APP, loader), "get");
                Object appStates = getObjectField(finsky, "mAppStates");
                Object installer = getObjectField(finsky, "mInstaller");
                for (AppInfo app : appList) {
                    Object info = callMethod(appStates, "getApp", app.packageName);
                    if (info != null) {
                        Object packageState = getObjectField(info, "packageManagerState");
                        if (packageState != null && getIntField(packageState, "installedVersion") == app.versionCode) {
                            debug("Skipped " + app.packageName + " since exactly the same version is already installed");
                            continue;
                        }
                    }
                    callMethod(installer, "setVisibility", app.packageName, true, true, true);
                    String account = (String) callStaticMethod(findClass(Commons.CLASS_APP_DETAILS_ACCOUNT, loader), "getAppDetailsAccount", app.packageName, callMethod(finsky, "getCurrentAccountName"), getObjectField(finsky, "mAppStates"), getObjectField(finsky, "mLibraries"));
                    installing = app.packageName;//this is so stupid that i don't believe i'm actually doing this
                    callMethod(installer, "requestInstall", app.packageName, app.versionCode, account, app.title, false, "storeprefs", 1, 0);
                    debug("(Restore intent) Installing " + app.packageName + " version code: " + app.versionCode);
                }
                intent.setAction(Commons.ACTION_INSTALL_RESULT);
                intent.putExtra("success", true);
                context.sendBroadcast(intent);
            }
        }, iF);
    }

    private boolean shouldUserUpdate(String packageName, int versionCode, String versionString) {
        debug("User wants to update " + packageName + " version: " + versionString + " (code: " + versionCode + ")");
        for (Object listener : listeners.values()) {
            if (listener == null) continue;
            try {
                if (!((boolean) callMethod(listener, "shouldUserUpdate", packageName, versionCode, versionString)))
                    return false;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return true;
    }

    private boolean canAutoUpdate(String packageName, int versionCode) {
        debug("Store tries to auto update " + packageName + " version code: " + versionCode);
        for (Object listener : listeners.values()) {
            if (listener == null) continue;
            try {
                if (!((boolean) callMethod(listener, "canAutoUpdate", packageName, versionCode)))
                    return false;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return true;
    }

    private void debug(String string) {
        if (debug)
            XposedBridge.log("[Storeprefs] " + string);
    }

    private boolean checkLuckyPatcher(Context context) {
        return packageExists("com.dimonvideo.luckypatcher", context) || packageExists("com.chelpus.lackypatch", context) || packageExists("com.android.vending.billing.InAppBillingService.LUCK", context);
    }

    private boolean packageExists(final String packageName, Context context) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
            return info != null;
        } catch (Exception ignored) {
        }
        return false;
    }
}
