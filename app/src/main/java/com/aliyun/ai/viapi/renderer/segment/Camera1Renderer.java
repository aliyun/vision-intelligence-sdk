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

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.aliyun.ai.viapi.gles.util.OpenGLUtil;
import com.aliyun.ai.viapi.renderer.IRendererListener;
import com.aliyun.ai.viapi.util.CameraHelper;

public class Camera1Renderer extends BaseRenderer implements CameraHelper.OnCameraPreviewFrameListener {
    private static final String TAG = "Camera1Renderer";
    private int mCameraWidth = CameraHelper.DEFAULT_PREVIEW_WIDTH;
    private int mCameraHeight = CameraHelper.DEFAULT_PREVIEW_HEIGHT;
    private final GLSurfaceView mGlSurfaceView;
    private final Activity mActivity;
    private final IRendererListener mIRendererListener;
    private final CameraHelper mCameraHelper;

    public Camera1Renderer(Activity activity, GLSurfaceView glSurfaceView, IRendererListener iRendererListener) {
        super(glSurfaceView, iRendererListener);
        mGlSurfaceView = glSurfaceView;
        mActivity = activity;
        mIRendererListener = iRendererListener;
        mCameraHelper = new CameraHelper();
        mCameraHelper.initCamera();
        mCameraHelper.setCameraPreviewFrameListener(this);
    }

    protected void openCamera(int cameraFace) {
        mCameraHelper.openCamera(mActivity, cameraFace, mCameraWidth, mCameraHeight);
        int[] wh = mCameraHelper.getCameraWH();
        mCameraWidth = wh[0];
        mCameraHeight = wh[1];
        if (mCameraWidth > 0 && mCameraHeight > 0) {
            mMvpMatrix = OpenGLUtil.changeMvpMatrixCrop(mViewWidth, mViewHeight, wh[1], wh[0]);
        }
        int cameraOrientation = mCameraHelper.getCameraOrientation();
        mIRendererListener.onSwitchCamera(cameraFace, cameraOrientation);
    }

    @Override
    protected void startPreview() {
        if (mCameraTexId <= 0 || mIsPreview) {
            return;
        }
        if (mSurfaceTexture == null) {
            mSurfaceTexture = new SurfaceTexture(mCameraTexId);
        }
        mCameraHelper.startPreview(mSurfaceTexture);
        this.mIsPreview = true;
    }

    @Override
    protected void closeCamera() {
        Log.d(TAG, "closeCamera. thread:" + Thread.currentThread().getName());
        mCameraHelper.closeCamera();
        mIsPreview = false;
        super.closeCamera();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isProcessFinished.compareAndSet(true, false)) {
            if (mYuv420sp == null || mYuv420sp.length != data.length) {
                mYuv420sp = new byte[data.length];
            }
            System.arraycopy(data, 0, mYuv420sp, 0, data.length);
            if (!mIsStopPreview) {
                mGlSurfaceView.requestRender();
            }
        } else {
//             Log.w(TAG, "上一帧数据还未处理完 => 丢帧处理");
        }
    }
}
