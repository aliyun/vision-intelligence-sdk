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
package com.aliyun.ai.viapi.util;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.widget.Toast;

import com.aliyun.ai.viapi.VIAPISdkApp;
import com.aliyun.ai.viapi.gles.util.BitmapUtils;
import com.aliyun.ai.viapi.gles.util.OpenGLUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.opengles.GL10;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

/**
 * @author: created by hanbing
 * @date: 2020/12/1
 * @Description:
 */
public class TakePictureUtil {
    private static final String TAG = "TakePictureUtil";
    public static final String APP_SDCARD_PATH = VIAPISdkApp.getContext().getExternalFilesDir("").toString();
    public static final String IMAGE_FORMAT_JPG = ".jpg";
    private volatile AtomicBoolean mIsTakingPicture = new AtomicBoolean(false);

    public static String getCurrentDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        return df.format(new Date());
    }

    public boolean getIsTakingPicture() {
        return mIsTakingPicture.get();
    }

    public void setStartTakePicture(boolean isStart) {
        mIsTakingPicture.set(isStart);
    }

    public void takePicture(int texId, boolean isOES, float[] mvpMatrix, float[] texMatrix, final int texWidth, final int texHeight, Bitmap bitmap) {
        BitmapUtils.captureBitmap(texId, isOES, texMatrix, mvpMatrix, texWidth, texHeight, bitmap, mOnBitmapListener);
    }

    @SuppressLint("CheckResult")
    public void captureBitmap(GLSurfaceView glSurfaceView, final BitmapUtils.OnCaptureBitmapListener listener) {
        final int width = glSurfaceView.getWidth();
        final int height = glSurfaceView.getHeight();

        glSurfaceView.queueEvent(() -> {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4 * 2);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            GLES20.glFinish();
            GLES20.glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buffer);
            OpenGLUtil.checkGLError("glReadPixels");
            buffer.rewind();

            Completable.complete().subscribeOn(Schedulers.io()).subscribe(() -> {
                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(buffer);
                Matrix matrix = new Matrix();
                matrix.preScale(1f, -1f);
                Bitmap outBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, false);
                bmp.recycle();
                if (listener != null) {
                    listener.onPictureSaved(outBitmap);
                } else {
                    if (mOnBitmapListener != null) {
                        mOnBitmapListener.onPictureSaved(outBitmap);
                    }
                }
            });
        });
    }

    /**
     * 保存相机输出数据
     *
     * @param cameraNv21Byte
     * @param listener
     */
    @SuppressLint("CheckResult")
    public void captureByCameraOutput(byte[] cameraNv21Byte, int cameraWidth, int cameraHeight, final BitmapUtils.OnCaptureBitmapListener listener) {
        Completable.complete().subscribeOn(Schedulers.io()).subscribe(() -> {
            Bitmap outBitmap = BitmapUtils.getBitmap(cameraNv21Byte, cameraWidth, cameraHeight);
            if (listener != null) {
                listener.onPictureSaved(outBitmap);
            } else {
                if (mOnBitmapListener != null) {
                    mOnBitmapListener.onPictureSaved(outBitmap);
                }
            }
        });
    }

    /**
     * 传给算法之前的数据
     */
    @SuppressLint("CheckResult")
    public void captureByCameraInputSeg(Bitmap outBitmap, final BitmapUtils.OnCaptureBitmapListener listener) {
        Completable.complete().subscribeOn(Schedulers.io()).subscribe(() -> {
            if (listener != null) {
                listener.onPictureSaved(outBitmap);
            } else {
                if (mOnBitmapListener != null) {
                    mOnBitmapListener.onPictureSaved(outBitmap);
                }
            }
        });
    }

    /**
     * 保存算法输出数据
     *
     * @param rgba
     * @param listener
     */
    @SuppressLint("CheckResult")
    public void captureBySegmentOutput(ByteBuffer rgba, int cameraWidth, int cameraHeight, final BitmapUtils.OnCaptureBitmapListener listener) {
        Completable.complete().subscribeOn(Schedulers.io()).subscribe(() -> {
            Bitmap bitmap = Bitmap.createBitmap(
                    cameraWidth,
                    cameraHeight,
                    Bitmap.Config.ARGB_8888
            );
            bitmap.copyPixelsFromBuffer(rgba);

            if (listener != null) {
                listener.onPictureSaved(bitmap);
            } else {
                if (mOnBitmapListener != null) {
                    mOnBitmapListener.onPictureSaved(bitmap);
                }
            }
        });
    }

    private BitmapUtils.OnCaptureBitmapListener mOnBitmapListener = bitmap -> {

        final String filePath = FileUtil.saveBitmap(bitmap, APP_SDCARD_PATH, "viapi-" + getCurrentDate() + IMAGE_FORMAT_JPG);
        Log.d(TAG, "mOnBitmapListener: " + filePath);
        if (filePath != null) {
            ThreadExecutor.runOnMainThread(() -> Toast.makeText(VIAPISdkApp.getContext(), "保存成功: " + APP_SDCARD_PATH, Toast.LENGTH_LONG).show());
        }
        mIsTakingPicture.set(false);
    };

    /**
     * CAMERA_OUTPUT_MODE 指相机原始输出数据
     */
    public enum TakePictureMode {
        // 上层图层融合、算法输入buffer、算法输出buffer
        JAVA_BLEND_MODE, CAMERA_INPUT_SEG_MODE, CAMERA_SEG_OUT_MODE
    }

}
