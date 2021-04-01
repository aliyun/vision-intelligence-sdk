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

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.aliyun.ai.viapi.gles.filter.CameraTexFilter;
import com.aliyun.ai.viapi.gles.filter.Normal2DTexFilter;
import com.aliyun.ai.viapi.gles.filter.TwoImageBlendFilter;
import com.aliyun.ai.viapi.gles.util.OpenGLUtil;
import com.aliyun.ai.viapi.renderer.IRendererListener;
import com.aliyun.ai.viapi.renderer.IRendererTimeListener;
import com.aliyun.ai.viapi.renderer.TextureMatrix;
import com.aliyun.ai.viapi.util.CameraHelper;
import com.aliyun.ai.viapi.util.Logs;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class BaseRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "BaseRenderer";
    private int mCameraFace = CameraHelper.FACE_FRONT;
    private final int mTextureWidth = CameraHelper.DEFAULT_PREVIEW_WIDTH;
    private final int mmTextureHeight = CameraHelper.DEFAULT_PREVIEW_HEIGHT;
    private final int[] mFBOTextures = new int[1];
    private int m2dTexId;
    private final float[] mTexMatrix = Arrays.copyOf(TextureMatrix.TEXTURE_MATRIX, TextureMatrix.TEXTURE_MATRIX.length);
    private final float[] mTex2Matrix = Arrays.copyOf(OpenGLUtil.IDENTITY_MATRIX, OpenGLUtil.IDENTITY_MATRIX.length);
    private final IRendererListener mIRendererListener;
    private Normal2DTexFilter mTwoImageBlendFilter;
    private Normal2DTexFilter mNormal2DTexFilter;
    private CameraTexFilter mCameraTexFilter;
    protected Handler mTaskHandler;
    protected int mViewWidth;
    protected int mViewHeight;
    protected float[] mMvpMatrix;
    protected int mCameraTexId;
    protected byte[] mYuv420sp;
    private final GLSurfaceView mGlSurfaceView;
    protected SurfaceTexture mSurfaceTexture;
    protected volatile AtomicBoolean isProcessFinished = new AtomicBoolean(true);
    protected volatile boolean mIsStopPreview;
    protected boolean mIsPreview;
    private final Object mCameraLock = new Object();
    //--------------------------------------FPS（FPS相关定义）----------------------------------------
    private static final int NANO_IN_ONE_NANO_SECOND = 1_000_000_000;
    private static final int TIME = 10;
    private int mCurrentFrameCnt = 0;
    private long mLastOneHundredFrameTimeStamp = 0;
    private boolean mNeedMarkTimeCost = false;
    private IRendererTimeListener markFPSListener;

    protected BaseRenderer(GLSurfaceView glSurfaceView, IRendererListener iRendererListener) {
        mGlSurfaceView = glSurfaceView;
        mIRendererListener = iRendererListener;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        OpenGLUtil.printVersionInfo();
        mNormal2DTexFilter = new Normal2DTexFilter();
        mTwoImageBlendFilter = new TwoImageBlendFilter();
        mCameraTexFilter = new CameraTexFilter();
        mCameraTexId = OpenGLUtil.createTextureObject(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mIRendererListener.onSurfaceCreated();
        mTaskHandler.post(() -> {
            openCamera(mCameraFace);
            startPreview();
            isProcessFinished.set(true);
        });
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        if (mViewWidth != width || mViewHeight != height) {
            mMvpMatrix = OpenGLUtil.changeMvpMatrixCrop(width, height, mmTextureHeight, mTextureWidth);
        }
        mTwoImageBlendFilter.setTex2Matrix(mTex2Matrix);
        mViewWidth = width;
        mViewHeight = height;
        mIRendererListener.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        if (mTwoImageBlendFilter == null || mSurfaceTexture == null) {
            return;
        }
        //帧率时间统计
        markFPSAndRenderTime();
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        try {
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(mTexMatrix);
            mSurfaceTexture.getTransformMatrix(mTex2Matrix);
        } catch (Exception e) {
            Logs.e(TAG, "onDrawFrame e: " + e.toString());
        }
        if (!mIsStopPreview && mIsPreview) {
            if (mYuv420sp != null && mIsPreview) {
                markFPSListener.onCompose2dTexIdBeginTime();

                m2dTexId = mIRendererListener.onDrawFrame(mYuv420sp, mCameraTexId,
                        mTextureWidth, mmTextureHeight, mMvpMatrix, mTexMatrix, mSurfaceTexture.getTimestamp());

                markFPSListener.onCompose2dTexIdEndTime();
            }
            if (m2dTexId > 0 && (m2dTexId != mCameraTexId)) {
                markFPSListener.onFrameOnScreenBeginTime();
                //上屏渲染
                //mTwoImageBlendFilter.drawFrame(m2dTexId, mRenderRotatedImage ? OpenGLUtil.IDENTITY_MATRIX : mTexMatrix, mMvpMatrix, mViewWidth, mViewHeight);
                //离屏渲染
                mFBOTextures[0] = mTwoImageBlendFilter.drawFrameOffScreen(m2dTexId, mViewWidth, mViewHeight, mTexMatrix, mMvpMatrix);
                mNormal2DTexFilter.drawFrame(mFBOTextures[0], OpenGLUtil.IDENTITY_MATRIX, OpenGLUtil.IDENTITY_MATRIX, mViewWidth, mViewHeight);

                markFPSListener.onFrameOnScreenEndTime();
            } else if (mCameraTexId > 0) {
                markFPSListener.onFrameOnScreenBeginTime();
                //上屏原OES渲染
                mCameraTexFilter.drawFrame(mCameraTexId, mTexMatrix, mMvpMatrix, mViewWidth, mViewHeight);
                //离屏2D渲染
                //mFrameBufferTexturesNative[0] = mCameraTexFilter.drawFrameOffScreen(mCameraTexId, mViewWidth, mViewHeight, mTexMatrix, OpenGLUtil.IDENTITY_MATRIX);
                //mNormal2DTexFilterFramebuffer.drawFrame(mFrameBufferTexturesNative[0], OpenGLUtil.IDENTITY_MATRIX, mMvpMatrix);
                //mNormal2DTexFilterFramebuffer.drawFrame(id, OpenGLUtil.IDENTITY_MATRIX, mMvpMatrix,mViewWidth,mViewHeight);
                markFPSListener.onFrameOnScreenEndTime();
            }
        }
        isProcessFinished.set(true);
        if (!mIsStopPreview) {
            mGlSurfaceView.requestRender();
        }
        markFPSListener.onTotalRenderTime();
    }

    public void onPause() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        mGlSurfaceView.queueEvent(() -> {
            release();
            countDownLatch.countDown();
        });
        try {
            countDownLatch.await(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // ignored
        }
        mGlSurfaceView.onPause();
        mTaskHandler.post(() -> closeCamera());
        stopTaskThread();
    }

    public void onResume() {
        startTaskThread();
        mTaskHandler.post(() -> {
            openCamera(mCameraFace);
            startPreview();
        });
        mGlSurfaceView.onResume();
    }

    private void release() {
        if (mCameraTexId != 0) {
            GLES20.glDeleteTextures(1, new int[]{mCameraTexId}, 0);
            mCameraTexId = 0;
        }
        if (mTwoImageBlendFilter != null) {
            mTwoImageBlendFilter.release();
            mTwoImageBlendFilter = null;
        }
        if (mCameraTexFilter != null) {
            mCameraTexFilter.release();
            mCameraTexFilter = null;
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        m2dTexId = -1;
        mIRendererListener.onSurfaceDestroy();
    }

    private void startTaskThread() {
        HandlerThread backgroundThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        backgroundThread.start();
        mTaskHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopTaskThread() {
        if (mTaskHandler != null) {
            mTaskHandler.getLooper().quitSafely();
            mTaskHandler = null;
        }
    }

    public void switchCamera() {
        if (mTaskHandler == null) {
            return;
        }
        mTaskHandler.post(() -> {
            mIsStopPreview = true;
            synchronized (mCameraLock) {
                boolean isFront = mCameraFace == CameraHelper.FACE_FRONT;
                mCameraFace = isFront ? CameraHelper.FACE_BACK : CameraHelper.FACE_FRONT;
            }
            closeCamera();
            openCamera(mCameraFace);
            startPreview();
            mIsStopPreview = false;
        });
    }

    public int getCameraFace() {
        synchronized (mCameraLock) {
            return mCameraFace;
        }
    }

    protected void openCamera(int cameraFacing) {
    }

    protected void startPreview() {
    }

    public void setBlendImageBg(Bitmap bitmap) {
        if (mTwoImageBlendFilter != null) {
            mTwoImageBlendFilter.setBitmap(bitmap);
        }
    }

    public void setMarkFPSListener(IRendererTimeListener markFPSListener) {
        this.markFPSListener = markFPSListener;
    }

    public void setNeedMarkTimeCost(boolean isMask) {
        this.mNeedMarkTimeCost = isMask;
    }

    protected void closeCamera() {
        mYuv420sp = null;
    }

    public void markFPSAndRenderTime() {
        if (!mNeedMarkTimeCost) {
            return;
        }
        if (++mCurrentFrameCnt == TIME) {
            mCurrentFrameCnt = 0;
            double fps = ((float) TIME * NANO_IN_ONE_NANO_SECOND / (System.nanoTime() - mLastOneHundredFrameTimeStamp));
            mLastOneHundredFrameTimeStamp = System.nanoTime();
//            Logs.d(TAG, "markFPSAndRenderTime: fps" + fps);
            if (markFPSListener != null) {
                markFPSListener.onFpsChange(fps);
            }
        }
    }
}
