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
package com.aliyun.ai.viapi.gles.util;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.ExifInterface;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.util.Log;

import com.aliyun.ai.viapi.gles.filter.CameraTexFilter;
import com.aliyun.ai.viapi.gles.filter.Normal2DTexFilter;
import com.aliyun.ai.viapi.gles.filter.TwoImageBlendFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.opengles.GL10;

public class BitmapUtils {
    private static final String TAG = "BitmapUtil";

    public static Bitmap mirrorRotateImage(Bitmap bitmap, int angle, boolean isHMirror, boolean isVMirror, boolean firstRotate) {
        Matrix matrix = new Matrix();
        if (firstRotate) {
            matrix.postRotate(angle);
            if (isVMirror) {
                // 镜像垂直翻转
                matrix.postScale(1, -1);
            } else if (isHMirror) {
                // 镜像水平翻转
                matrix.postScale(-1, 1);
            }
        } else {
            if (isVMirror) {
                // 镜像垂直翻转
                matrix.postScale(1, -1);
            } else if (isHMirror) {
                // 镜像水平翻转
                matrix.postScale(-1, 1);
            }
            matrix.postRotate(angle);
        }
        Bitmap outBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return outBitmap;
    }

    public static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        int A, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                A = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;

                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }
                index++;
            }
        }
    }

    /**
     * 获取图片的方向
     *
     * @param path
     * @return
     */
    public static int getImageOrientation(String path) {
        int orientation = 0;
        int tagOrientation = 0;
        try {
            tagOrientation = new ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
        } catch (IOException e) {
            Log.e(TAG, "getPhotoOrientation: ", e);
        }
        if (tagOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            orientation = 90;
        } else if (tagOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            orientation = 180;
        } else if (tagOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            orientation = 270;
        }
        return orientation;
    }

    public static byte[] bitmap2RGBAByteArray(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        byte[] bytes = new byte[bitmap.getByteCount()];
        ByteBuffer rgbaBuffer = ByteBuffer.wrap(bytes);
        bitmap.copyPixelsToBuffer(rgbaBuffer);
        return bytes;
    }

    /**
     * 获取 Bitmap 的宽高
     *
     * @param path
     * @return
     */
    public static Point getBitmapSize(String path) {
        Point point = new Point();
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opt);
        point.x = opt.outWidth;
        point.y = opt.outHeight;
        return point;
    }

    /**
     * bitmap 转 NV21 数据
     *
     * @param inputWidth
     * @param inputHeight
     * @param scaled
     * @return
     */
    public static byte[] getNV21(int inputWidth, int inputHeight, Bitmap scaled) {
        int[] argb = new int[inputWidth * inputHeight];
        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);
        byte[] yuv = new byte[inputHeight * inputWidth + 2 * (int) Math.ceil((float) inputHeight / 2) * (int) Math.ceil((float) inputWidth / 2)];
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight);
        scaled.recycle();
        return yuv;
    }

    /**
     * Nv21转bitmap
     */
    public static Bitmap getBitmap(byte[] cameraNv21Byte, int previewWidth, int previewHeight) {
        Bitmap bitmap = null;
        try {
            //格式成YUV格式
            YuvImage yuvimage = new YuvImage(cameraNv21Byte, ImageFormat.NV21, previewWidth,
                    previewHeight, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, previewWidth,
                    previewHeight), 100, baos);
            bitmap = bytes2bitmap(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static Bitmap bytes2bitmap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    /**
     * byte 数组转byteBuffer
     *
     * @param byteArray
     */
    public static ByteBuffer byte2Byffer(byte[] byteArray) {

        //初始化一个和byte长度一样的buffer
        ByteBuffer buffer = ByteBuffer.allocate(byteArray.length);
        // 数组放到buffer中
        buffer.put(byteArray);
        //重置 limit 和postion 值 否则 buffer 读取数据不对
        buffer.flip();
        return buffer;
    }

    /**
     * @param texId
     * @param isOes
     * @param texMatrix
     * @param mvpMatrix
     * @param texWidth
     * @param texHeight
     * @param listener
     */
    public static void captureBitmap(int texId, boolean isOes, float[] texMatrix, float[] mvpMatrix, final int texWidth, final int texHeight, Bitmap bitmap, final OnCaptureBitmapListener listener) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texWidth, texHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        int[] frameBuffers = new int[1];
        GLES20.glGenFramebuffers(1, frameBuffers, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[0], 0);
        int[] viewport = new int[4];
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0);
        GLES20.glViewport(0, 0, texWidth, texHeight);
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (isOes) {
            new CameraTexFilter().drawFrame(texId, texMatrix, mvpMatrix, texWidth, texHeight);
        } else {
            //new Alpha2DTexFilter().drawFrame(texId, texMatrix, mvpMatrix);
            Normal2DTexFilter mNormal2DTexFilter = new TwoImageBlendFilter();
            mNormal2DTexFilter.setBitmap(bitmap);
            mNormal2DTexFilter.setTex2Matrix(texMatrix);
            mNormal2DTexFilter.drawFrame(texId, texMatrix, mvpMatrix, texWidth, texHeight);
        }
        final ByteBuffer buffer = ByteBuffer.allocateDirect(texWidth * texHeight * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glFinish();
        GLES20.glReadPixels(0, 0, texWidth, texHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buffer);
        OpenGLUtil.checkGLError("glReadPixels");
        buffer.rewind();
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDeleteTextures(1, textures, 0);
        GLES20.glDeleteFramebuffers(1, frameBuffers, 0);

        AsyncTask.execute(() -> {
            Bitmap bmp = Bitmap.createBitmap(texWidth, texHeight, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buffer);
            Matrix matrix = new Matrix();
            matrix.preScale(1f, -1f);
            Bitmap finalBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, false);
            bmp.recycle();
            if (listener != null) {
                listener.onPictureSaved(finalBmp);
            }
        });
    }

    /**
     * 通过相机纹理ID获取到对应的buffer
     */
    /**
     * @param texId
     * @param texMatrix
     * @param mvpMatrix
     * @param texWidth
     * @param texHeight
     */
    public static int getTexIdBuffer(int texId, float[] texMatrix, float[] mvpMatrix, final int texWidth, final int texHeight, ByteBuffer outBuffer) {
        long test, test1, test2, test3;
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texWidth, texHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        int[] frameBuffers = new int[1];
        GLES20.glGenFramebuffers(1, frameBuffers, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[0], 0);
        int[] viewport = new int[4];
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0);
        GLES20.glViewport(0, 0, texWidth, texHeight);
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        test2 = System.currentTimeMillis();
        new CameraTexFilter().drawFrame(texId, texMatrix, mvpMatrix, texWidth, texHeight);
        test3 = System.currentTimeMillis() - test2;
        Log.i("算法glReadPixel画图时间：", test3 + "");
        outBuffer.clear();
        outBuffer.order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glFinish();
        test = System.currentTimeMillis();
        GLES20.glReadPixels(0, 0, texWidth, texHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, outBuffer);
        test1 = System.currentTimeMillis() - test;
        Log.i("算法glReadPixels时间：", test1 + "");
        OpenGLUtil.checkGLError("glReadPixels");
        outBuffer.rewind();
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDeleteTextures(1, textures, 0);
        GLES20.glDeleteFramebuffers(1, frameBuffers, 0);
        return 0;
    }

    public static Bitmap GetRoundedCornerBitmap(Bitmap bitmap) {
        try {
            Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                    bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(),
                    bitmap.getHeight());
            final RectF rectF = new RectF(new Rect(0, 0, bitmap.getWidth(),
                    bitmap.getHeight()));
            final float roundPx = 90;
            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(Color.BLACK);
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

            final Rect src = new Rect(0, 0, bitmap.getWidth(),
                    bitmap.getHeight());

            canvas.drawBitmap(bitmap, src, rect, paint);
            return output;
        } catch (Exception e) {
            return bitmap;
        }
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, float w, float h) {
        float width = bitmap.getWidth();
        float height = bitmap.getHeight();
        float x = 0, y = 0, scaleWidth = width, scaleHeight = height;
        Bitmap newbmp;
        if (w > h) {
            // 比例宽度大于高度的情况
            float scale = w / h;
            float tempH = width / scale;
            if (height > tempH) {
                x = 0;
                y = (height - tempH) / 2;
                scaleWidth = width;
                scaleHeight = tempH;
            } else {
                scaleWidth = height * scale;
                x = (width - scaleWidth) / 2;
                y = 0;
            }
            Log.e(TAG, "scale:" + scale + " scaleWidth:" + scaleWidth + " scaleHeight:" + scaleHeight);
        } else if (w < h) {
            // 比例宽度小于高度的情况
            float scale = h / w;
            float tempW = height / scale;
            if (width > tempW) {
                y = 0;
                x = (width - tempW) / 2;
                scaleWidth = tempW;
                scaleHeight = height;
            } else {
                scaleHeight = width * scale;
                y = (height - scaleHeight) / 2;
                x = 0;
                scaleWidth = width;
            }

        } else {
            // 比例宽高相等的情况
            if (width > height) {
                x = (width - height) / 2;
                y = 0;
                scaleHeight = height;
                scaleWidth = height;
            } else {
                y = (height - width) / 2;
                x = 0;
                scaleHeight = width;
                scaleWidth = width;
            }
        }
        try {
            // createBitmap()方法中定义的参数x+width要小于或等于bitmap.getWidth()，y+height要小于或等于bitmap.getHeight()
            newbmp = Bitmap.createBitmap(bitmap, (int) x, (int) y, (int) scaleWidth, (int) scaleHeight, null, false);
            //bitmap.recycle();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return bitmap;
        }
        return newbmp;
    }

    public interface OnCaptureBitmapListener {
        /**
         * 读取图片完成
         *
         * @param bitmap
         */
        void onPictureSaved(Bitmap bitmap);
    }
}



