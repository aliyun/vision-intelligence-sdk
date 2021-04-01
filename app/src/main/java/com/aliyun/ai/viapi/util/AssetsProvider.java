package com.aliyun.ai.viapi.util;

import android.content.Context;
import android.content.res.AssetManager;

import com.aliyun.ai.viapi.ui.vb.VBConfRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AssetsProvider {
    private static final String LICENSE_CHILD_PATH = "license";
    private static final String LICENSE_NAME = "damo-viapi.license";
    private static final String MODELS_CHILD_PATH = "models";
    private static final String SEGMENT_MODE_NAME = "seg_human_0.0.1.nn";
    private static final String TAG = "AssetsProvider";
    // 模型升级手动将下面丢版本号+1，触发mode文件重新拷贝
    private static final int SEGMENT_MODE_VERSION = 1;

    public static String getSegmentModelsFilePath(Context context) {
        String path = MODELS_CHILD_PATH + File.separator + SEGMENT_MODE_NAME;
        return context.getFilesDir().getAbsolutePath() + File.separator + path;
//        debug 时使用下面的路径
//        String path = MODELS_CHILD_PATH;
//        return context.getFilesDir().getAbsolutePath() + File.separator + path;
    }

    public static boolean copySegmentModeFile(Context context) {
        String filePath = context.getFilesDir().getAbsolutePath();
        File path = new File(filePath, MODELS_CHILD_PATH);
        // 拷贝assets/models 下文件
        // copyAssetsFiles(context, path, MODELS_CHILD_PATH, true);
        String segmentFile = path.getAbsolutePath() + File.separator + SEGMENT_MODE_NAME;
        //  检测是否需要拷贝模型文件到文件系统目录
        boolean isUpgrade = checkVersionUpgradeSegmentModeFile(context, segmentFile);
        Logs.i(TAG, "copySegmentModeFile : isUpgrade = " + isUpgrade);
        if (isUpgrade) {
            if (installModeFile(context, context.getFilesDir().getAbsolutePath(), MODELS_CHILD_PATH, SEGMENT_MODE_NAME, true)) {
                VBConfRecord.setVIAPISegmentModeVersion(context, SEGMENT_MODE_VERSION);
                Logs.i(TAG, "copySegmentModeFile : upgrade success!!  ");
            } else {
                return false;
            }
        } else {
            Logs.i(TAG, "copySegmentModeFile  : mode file already exist so do not copy it!!  ");
        }
        return true;
    }

    private static boolean checkVersionUpgradeSegmentModeFile(Context context, String modeFilePath) {
        if (!isExistFile(modeFilePath)) {
            return true;
        }
        int oldVer = VBConfRecord.getVIAPISegmentModeVersion(context);
        if (oldVer != SEGMENT_MODE_VERSION) {
            return true;
        }
        return false;
    }

    private static boolean isExistFile(String file) {
        try {
            return new File(file).exists();
        } catch (Exception e) {
            return false;
        }
    }

    private static void copyFile(File file, InputStream is) throws IOException {
        final FileOutputStream out = new FileOutputStream(file);
        byte buf[] = new byte[1024];
        int len;
        while ((len = is.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
        is.close();
    }


    public static boolean installModeFile(Context context, String destPath, String assetsPath, String filename, boolean isForce) {
        File dir = new File(destPath, assetsPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            File f = new File(dir, filename);
            if (f.exists()) {
                if (isForce) {
                    f.delete();
                    return copyAssetsFile(context, assetsPath + File.separator + filename, f);
                }
                return true;
            }
            return copyAssetsFile(context, assetsPath + File.separator + filename, f);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean copyAssetsFile(Context context, String assetsFilename, File file) {
        AssetManager manager = context.getAssets();
        InputStream is;
        try {
            is = manager.open(assetsFilename);
            try {
                copyFile(file, is);
            } catch (IOException e) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
