package com.earlyai.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Base64;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@CapacitorPlugin(name = "AllFiles")
public class AllFilesPlugin extends Plugin {

    private boolean granted() {
        return Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager();
    }

    private byte[] readAll(File f) throws Exception {
        InputStream in = new FileInputStream(f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    private void writeAll(String path, byte[] data) throws Exception {
        File f = new File(path);
        File p = f.getParentFile();
        if (p != null && !p.exists()) p.mkdirs();
        FileOutputStream o = new FileOutputStream(f);
        o.write(data);
        o.close();
    }

    @PluginMethod
    public void check(PluginCall call) {
        JSObject o = new JSObject();
        o.put("granted", granted());
        call.resolve(o);
    }

    @PluginMethod
    public void request(PluginCall call) {
        try {
            Intent i;
            if (Build.VERSION.SDK_INT >= 30) {
                i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getContext().getPackageName()));
            } else {
                i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getContext().getPackageName()));
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void list(PluginCall call) {
        try {
            File d = new File(call.getString("path", "/storage/emulated/0"));
            File[] fs = d.listFiles();
            JSArray arr = new JSArray();
            if (fs != null) {
                Arrays.sort(fs, (x, y) -> x.getName().compareToIgnoreCase(y.getName()));
                for (File f : fs) {
                    JSObject e = new JSObject();
                    e.put("name", f.getName());
                    e.put("dir", f.isDirectory());
                    e.put("size", f.length());
                    arr.put(e);
                }
            }
            JSObject o = new JSObject();
            o.put("entries", arr);
            call.resolve(o);
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void readText(PluginCall call) {
        try {
            JSObject o = new JSObject();
            o.put("data", new String(readAll(new File(call.getString("path"))), StandardCharsets.UTF_8));
            call.resolve(o);
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void readBase64(PluginCall call) {
        try {
            JSObject o = new JSObject();
            o.put("data", Base64.encodeToString(readAll(new File(call.getString("path"))), Base64.NO_WRAP));
            call.resolve(o);
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void writeText(PluginCall call) {
        try {
            writeAll(call.getString("path"), call.getString("data", "").getBytes(StandardCharsets.UTF_8));
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void writeBase64(PluginCall call) {
        try {
            writeAll(call.getString("path"), Base64.decode(call.getString("data", ""), Base64.DEFAULT));
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }
}
