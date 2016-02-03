package com.stirante.storeprefs.utils;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;

/**
 * Created by stirante
 */
public class SimpleDatabase {

    private static HashMap<String, Object> data;
    private static File f = new File(Environment.getExternalStorageDirectory() + "/Storeprefs/database.dat");

    public static synchronized void load() {
        if (!f.exists()) {
            data = new HashMap<>();
            return;
        }
        try {
            FileInputStream fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis);
            data = (HashMap<String, Object>) ois.readObject();
            ois.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (data == null) data = new HashMap<>();
    }

    public static synchronized void save() {
        f.getParentFile().mkdirs();
        try {
            FileOutputStream fos = new FileOutputStream(f);
            ObjectOutputStream ous = new ObjectOutputStream(fos);
            ous.writeObject(data);
            ous.flush();
            ous.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void put(String key, Object value) {
        data.put(key, value);
    }

    public static Object get(String key, Object def) {
        if (!contains(key)) data.put(key, def);
        return data.get(key);
    }

    public static Object get(String key) {
        return get(key, null);
    }

    public static boolean contains(String key) {
        return data.containsKey(key);
    }

    public static void saveAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                save();
            }
        }).start();
    }
}
