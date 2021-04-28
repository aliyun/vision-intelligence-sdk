/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aliyun.ai.viapi.renderer.segment;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.aliyun.ai.viapi.renderer.IRendererListener;
import com.aliyun.ai.viapi.renderer.ISwitchCamera;
import com.aliyun.ai.viapi.util.Camera2Helper;
import com.aliyun.ai.viapi.util.CameraHelper;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Renderer extends BaseRenderer implements Camera2Helper.OnCamera2FrameListener {
    private static final String TAG = "Camera2Renderer";
    private final Object mCameraLock = new Object();
    private final Activity mActivity;
    private final GLSurfaceView mGlSurfaceView;
    private final IRendererListener mIRendererListener;
    private final Camera2Helper mCameraHelper;

    public Camera2Renderer(Activity activity, GLSurfaceView glSurfaceView, IRendererListener onRendererStatusListener) {
        super(glSurfaceView, onRendererStatusListener);
        this.mActivity = activity;
        this.mGlSurfaceView = glSurfaceView;
        this.mIRendererListener = onRendererStatusListener;
        this.mCameraHelper = new Camera2Helper();
        this.mCameraHelper.initCamera(mActivity, mTaskHandler);
        this.mCameraHelper.setCameraPreviewFrameListener(this);
    }

    @Override
    protected void openCamera(int cameraFace) {
        if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            throw new RuntimeException("Camera Permission Denied");
        }
        mCameraHelper.openCamera(mActivity, cameraFace, mSurfaceTexture, new ISwitchCamera() {

            @Override
            public void onSwitchCamera(int cameraFace, int cameraOrientation) {
                mIRendererListener.onSwitchCamera(cameraFace, cameraOrientation);
            }
        });
    }

    @Override
    protected void closeCamera() {
        Log.d(TAG, "closeCamera. thread:" + Thread.currentThread().getName());
        mCameraHelper.closeCamera();
        mIsPreview = false;
        super.closeCamera();
    }

    @Override
    protected void startPreview() {
        if (mCameraTexId <= 0 || mIsPreview) {
            return;
        }
        if (mSurfaceTexture == null) {
            mSurfaceTexture = new SurfaceTexture(mCameraTexId);
        }
        mCameraHelper.startPreview(mSurfaceTexture, mCameraTexId);
        this.mIsPreview = true;
    }

    @Override
    public void onFrame(byte[] nv21data) {
        if (isProcessFinished.compareAndSet(true, false)) {
            if (mYuv420sp == null || mYuv420sp.length != nv21data.length) {
                mYuv420sp = new byte[nv21data.length];
            }
            System.arraycopy(nv21data, 0, mYuv420sp, 0, nv21data.length);
            if (!mIsStopPreview) {
                mGlSurfaceView.requestRender();
            }
        } else {
//             Log.w(TAG, "上一帧数据还未处理完 => 丢帧处理");
        }
    }

    public int getCameraFace() {
        synchronized (mCameraLock) {
            return CameraHelper.FACE_FRONT;
        }
    }
}