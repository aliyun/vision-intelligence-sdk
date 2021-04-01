package com.aliyun.ai.viapi.renderer;

/**
 * @author: created by hanbing
 * @date: 2020/11/29
 * @Description:
 */
public interface ISwitchCamera {
    /**
     * @param cameraFace      FACE_BACK = 0, FACE_FRONT = 1
     * @param cameraOrientation
     */
    void onSwitchCamera(int cameraFace, int cameraOrientation);
}
