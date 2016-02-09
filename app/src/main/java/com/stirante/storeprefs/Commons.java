package com.stirante.storeprefs;

/**
 * Created by stirante
 */
public class Commons {
    public static final String PLAYSTORE_PACKAGE = "com.android.vending";
    public static final String XPOSED_INSTALLER_PACKAGE = "de.robv.android.xposed.installer";
    public static final String MODULE_PACKAGE = "com.stirante.storeprefs";

    public static final String METADATA_MAINCLASS = "storeprefs_mainclass";

    public static final String PREFERENCE_DEBUG = "enable_spam";
    public static final String PREFERENCE_WARNING = "enable_warning";
    public static final String PREFERENCE_INTENTS = "enable_intents";
    public static final String PREFERENCE_RAPID = "disable_rapid";

    public static final String CLASS_MY_APPS_INSTALLED_ADAPTER = "com.google.android.finsky.activities.myapps.MyAppsInstalledAdapter";
    public static final String CLASS_DOCUMENT = "com.google.android.finsky.api.model.Document";
    public static final String CLASS_INSTALL_POLICIES = "com.google.android.finsky.installer.InstallPolicies";
    public static final String CLASS_PLAYSTORE_UI_ELEMENT_NODE = "com.google.android.finsky.layout.play.PlayStoreUiElementNode";
    public static final String CLASS_PACKAGE_STATE = "com.google.android.finsky.appstate.PackageStateRepository$PackageState";
    public static final String CLASS_LIGHT_PURCHASE_FLOW_ACTIVITY = "com.google.android.finsky.billing.lightpurchase.LightPurchaseFlowActivity";
    public static final String CLASS_DOWNLOAD_NETWORK_DIALOG_FRAGMENT = "com.google.android.finsky.billing.DownloadNetworkDialogFragment";
    public static final String CLASS_ALERT_DIALOG_BUILDER_COMPAT = "com.google.android.wallet.ui.common.AlertDialogBuilderCompat";
    public static final String CLASS_RAPID_AUTO_UPATE_POLICY = "com.google.android.finsky.autoupdate.RapidAutoUpdatePolicy";
    public static final String CLASS_AUTO_UPDATE_ENTRY = "com.google.android.finsky.autoupdate.AutoUpdateEntry";
    public static final String CLASS_FINSKY_APP = "com.google.android.finsky.FinskyApp";
    public static final String CLASS_APP_DETAILS_ACCOUNT = "com.google.android.finsky.activities.AppActionAnalyzer";
    public static final String CLASS_APP_STATES = "com.google.android.finsky.appstate.AppStates";

    public static final String ACTION_INSTALL = "com.stirante.storeprefs.INSTALL";
    public static final String ACTION_INSTALL_RESULT = "com.stirante.storeprefs.INSTALL_RESULT";
    public static final String ACTION_RESTORE = "com.stirante.storeprefs.RESTORE";

    public static final String DATA_DONT_UPDATE = "dontUpdate";
    public static final String DATA_RESTORE = "restore";

    public static final String ADD_FIELD_WARN_COMPATIBILITY = "warnCompatibility";
}
