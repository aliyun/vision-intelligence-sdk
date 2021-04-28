package com.aliyun.ai.viapi.util;

import android.content.res.AssetManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class AssetsUtils {

    private static boolean isAssetsDir(AssetManager assets, String path) {
        try {
            String[] files = assets.list(path);
            return files != null && files.length > 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void copyAssetsFile(AssetManager assets, String path, String destDir) throws IOException {
        if (isAssetsDir(assets, path)) {
            File dir = new File(destDir + File.separator + path);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new RuntimeException("mkdir " + dir.toString() + " fail");
            }
            for (String s : assets.list(path)) {
                copyAssetsFile(assets, path + "/" + s, destDir);
            }
        } else {
            InputStream input = assets.open(path);
            File dest = new File(destDir, path);
            FileUtil.copyToFile(input, dest);
        }
    }
}
