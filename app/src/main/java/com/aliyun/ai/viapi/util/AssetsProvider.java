package com.aliyun.ai.viapi.util;

import android.content.Context;

import com.aliyun.ai.viapi.BuildConfig;
import com.aliyun.ai.viapi.VIAPISdkApp;

import java.io.File;

public class AssetsProvider {
    private static final String TAG = "AssetsProvider";
    private static final String RESOURCE_ROOT_PATH = "resource";
    private static final String MODELS_PARENT_PATH = "resource/models";

    private static final String SEGMENT_MODEL_VIDEO_CHILD_PATH = "video_seg";
    private static final String SEGMENT_MODEL_PHOTO_CHILD_PATH = "photo_seg";

    public static String getModelsAbsolutePath() {
        if (BuildConfig.DEBUG) {
            return VIAPISdkApp.getContext().getExternalFilesDir("").toString();
        } else {
            return VIAPISdkApp.getContext().getFilesDir().getAbsolutePath();
        }
    }

    public static String getModelsParentPath() {
        return MODELS_PARENT_PATH;
    }

    public static String getResourceRootPath() {
        return RESOURCE_ROOT_PATH;
    }

    public static String getVideoSegmentModelsPath(Context context) {
        String path = MODELS_PARENT_PATH + File.separator + SEGMENT_MODEL_VIDEO_CHILD_PATH;
        return getModelsAbsolutePath() + File.separator + path;
    }

    public static String getPhotoSegmentModelsPath(Context context) {
        String path = MODELS_PARENT_PATH + File.separator + SEGMENT_MODEL_PHOTO_CHILD_PATH;
        return getModelsAbsolutePath() + File.separator + path;
    }
}
