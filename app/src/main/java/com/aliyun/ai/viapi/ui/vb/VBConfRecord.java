package com.aliyun.ai.viapi.ui.vb;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.aliyun.ai.viapi.ui.model.VBCardType;
import com.aliyun.ai.viapi.util.Logs;
import com.aliyun.ai.viapi.util.PreferenceUtils;

import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * @author: created by hanbing
 * @date: 2021/3/11
 * @Description:
 */
public class VBConfRecord extends PreferenceUtils {
    private final static String TAG = "VBConfRecord";
    private final static String HUMAN_SEGMENT_VB_IMAGE_NAME = "human_segment_vb_image_name";
    private final static String VIAPI_SEGMENT_MODE_VERSION_KEY = "viapi_segment_mode_version_key";

    public static void saveUserSelectVbImage(Context context, String virtualBgName) {
        if (virtualBgName == null) {
            virtualBgName = "";
        }
        put(context, HUMAN_SEGMENT_VB_IMAGE_NAME, virtualBgName);
    }

    public static String getUserSelectVbImage(Context context) {
        return (String) get(context, HUMAN_SEGMENT_VB_IMAGE_NAME, VBCardType.NONE.getCardName());
    }

    @Nullable
    public static Bitmap getBitmapFromName(Context context, String name) {
        Bitmap bitmap = null;
        if (TextUtils.isEmpty(name) || VBCardType.NONE.getCardName().equals(name)) {
            return null;
        }
        if (isLocalImagePath(name)) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(name);
                bitmap = BitmapFactory.decodeStream(fis);
            } catch (FileNotFoundException e) {
                Logs.e(TAG, "getSelectedVB = " + e.toString());
            }
        } else {
            try {
                Resources res = context.getResources();
                int id = res.getIdentifier(name, "drawable", context.getPackageName());
                bitmap = BitmapFactory.decodeResource(res, id);
            } catch (Exception e) {
                Logs.e(TAG, "donot find  drawable name : " + name);
            }
        }
        return bitmap;
    }

    public static boolean isLocalImagePath(final String imageName) {
        if (imageName == null) {
            return false;
        }
        return (imageName.startsWith("/"));
    }

    public static int getVIAPISegmentModeVersion(Context context) {
        return (int) get(context, VIAPI_SEGMENT_MODE_VERSION_KEY, 0);
    }

    public static void setVIAPISegmentModeVersion(Context context, int version) {
        put(context, VIAPI_SEGMENT_MODE_VERSION_KEY, version);
    }

}
