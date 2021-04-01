package com.aliyun.ai.viapi.util;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

public class FileUtil {
    private static String TAG = "FileUtil";

    public static String saveBitmap(Bitmap bitmap, String dir, String name) {
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        File bitmapFile = new File(dir, name);
        OutputStream fos = null;
        try {
            fos = new FileOutputStream(bitmapFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            return bitmapFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "saveBitmap: ", e);
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                    // ignored
                }
            }
        }
        return null;
    }

    public static String getFileExtension(String fileName) {
        String ext = "";
        int index = fileName.lastIndexOf(".");
        int pos = fileName.lastIndexOf("/");
        if (index > pos) {
            if (index > 0 && index < fileName.length()) {
                ext = fileName.substring(fileName.lastIndexOf(".") + 1);
            }
        }
        return ext;
    }

    public static String getFileNameNoExt(String filePath) {
        int start = filePath.lastIndexOf("/");
        int end = filePath.lastIndexOf(".");
        if (start != -1 && end != -1 && end > start + 1) {
            return filePath.substring(start + 1, end);
        } else {
            return "";
        }
    }

    public static boolean copyFile(File source, File target) {
        boolean isSuccess = true;
        FileInputStream fi = null;
        FileOutputStream fo = null;
        FileChannel in = null;
        FileChannel out = null;
        try {
            fi = new FileInputStream(source);
            fo = new FileOutputStream(target);
            in = fi.getChannel();
            out = fo.getChannel();
            in.transferTo(0, in.size(), out);
        } catch (IOException e) {
            e.printStackTrace();
            isSuccess = false;
        } finally {
            try {
                fi.close();
                in.close();
                fo.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
                isSuccess = false;
            }
        }
        return isSuccess;
    }
}
