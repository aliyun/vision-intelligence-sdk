package com.aliyun.ai.viapi.device;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: created by hanbing
 * @date: 2020/12/4
 * @Description:
 */
public enum DeviceRotation {

    ROTATION_0(0, 0),
    ROTATION_90(1, 90),
    ROTATION_180(2, 180),
    ROTATION_270(3, 270);
    private static final String TAG = "DeviceRotation";
    int id;
    int angle;

    private static Map<Integer, Integer> id2angle = new HashMap<>();

    static {
        for (DeviceRotation e : DeviceRotation.values()) {
            id2angle.put(e.id, e.angle);
        }
    }

    private static boolean isIdLegal(int id) {
        if (id >= ROTATION_0.id && id <= ROTATION_270.id) {
            return true;
        } else {
            return false;
        }
    }

    public int getAngle() {
        return angle;
    }

    public static Integer getAngle(int id) {
        if (!isIdLegal(id)) {
            Log.e(TAG, "getAngle id is inlegal " + id);
            return null;
        }
        return id2angle.get(id);
    }

    DeviceRotation(int id, int angle) {
        this.id = id;
        this.angle = angle;
    }
}
