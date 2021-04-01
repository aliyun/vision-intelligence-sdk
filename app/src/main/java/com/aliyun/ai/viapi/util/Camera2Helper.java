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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.aliyun.ai.viapi.gles.util.OpenGLUtil;
import com.aliyun.ai.viapi.renderer.ISwitchCamera;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public final class Camera2Helper implements ImageReader.OnImageAvailableListener {
    private static final String TAG = "Camera2Helper";
    public static final boolean DEBUG = false;
    public static final int FACE_BACK = Camera.CameraInfo.CAMERA_FACING_BACK;
    public static final int FACE_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;
    public static final int FRONT_CAMERA_ORIENTATION = 270;
    public static final int BACK_CAMERA_ORIENTATION = 90;
    public static final int DEFAULT_PREVIEW_WIDTH = 1280;
    public static final int DEFAULT_PREVIEW_HEIGHT = 720;
    public static final int PREVIEW_BUFFER_SIZE = 3;
    public static final int FOCUS_TIME = 2000;
    private static int mBackCameraOrientation = BACK_CAMERA_ORIENTATION;
    private static int mFrontCameraOrientation = FRONT_CAMERA_ORIENTATION;

    private int mCameraFacing = FACE_FRONT;
    private int mCameraWidth;
    private int mCameraHeight;
    private int mCameraOrientation;
    private OnCamera2FrameListener mOnCameraListener;

    // camera2
    private CameraManager mCameraManager;
    private String mFrontCameraId;
    private String mBackCameraId;
    private CameraCharacteristics mFrontCameraCharacteristics;
    private CameraCharacteristics mBackCameraCharacteristics;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private ImageReader mImageReader;
    private int mViewWidth;
    private int mViewHeight;
    private float[] mMvpMatrix;
    private int mCameraTexId;
    private boolean mIsPreviewing;
    private byte[][] mYuvDataBufferArray;
    private byte[] mYuvDataBuffer;
    private byte[] mYDataBuffer;
    private byte[] mCameraNv21Byte;
    private int mYuvDataBufferPosition;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
            Log.v(TAG, "onCaptureSequenceCompleted() called with: session = [" + session + "], sequenceId = [" + sequenceId + "], frameNumber = [" + frameNumber + "]");
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null);
        }
    };

    public void initCamera(Activity activity) {
        mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] ids = mCameraManager.getCameraIdList();
            if (ids.length <= 0) {
                throw new RuntimeException("No camera");
            }

            for (String id : ids) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null) {
                    if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        mFrontCameraId = id;
                        Integer iFrontCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                        mFrontCameraOrientation = iFrontCameraOrientation == null ? FRONT_CAMERA_ORIENTATION : iFrontCameraOrientation;
                        mFrontCameraCharacteristics = characteristics;
                    } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        mBackCameraId = id;
                        Integer iBackCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                        mBackCameraOrientation = iBackCameraOrientation == null ? BACK_CAMERA_ORIENTATION : iBackCameraOrientation;
                        mBackCameraCharacteristics = characteristics;
                    }
                }
            }
        } catch (CameraAccessException | IllegalArgumentException e) {
            Log.e(TAG, "initCameraInfo: ", e);
        }
        mCameraOrientation = mCameraFacing == FACE_FRONT ? mFrontCameraOrientation : mBackCameraOrientation;
        Log.i(TAG, "initCameraInfo. frontCameraId:" + mFrontCameraId + ", frontCameraOrientation:"
                + mFrontCameraOrientation + ", backCameraId:" + mBackCameraId + ", mBackCameraOrientation:"
                + mBackCameraOrientation);
    }

    public void openCamera(Activity activity, int cameraFace, SurfaceTexture surfaceTexture, ISwitchCamera switchCamera) {
        openCamera(activity, cameraFace, DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT, surfaceTexture, switchCamera);
    }

    public void openCamera(Activity activity, int cameraFace, int cameraWidth, int cameraHeight, SurfaceTexture surfaceTexture, ISwitchCamera switchCamera) {
        mCameraWidth = cameraWidth;
        mCameraHeight = cameraHeight;
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            throw new RuntimeException("Camera Permission Denied");
        }
        if (mCameraDevice != null) {
            return;
        }
        try {
            String cameraId = cameraFace == FACE_FRONT ? mFrontCameraId : mBackCameraId;
            mCameraOrientation = cameraFace == FACE_FRONT ? mFrontCameraOrientation : mBackCameraOrientation;
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (streamConfigurationMap != null) {
                Size[] outputSizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
                Size size = chooseOptimalSize(outputSizes, mCameraWidth, mCameraHeight,
                        1920, 1080, new Size(mCameraWidth, mCameraHeight));
                mCameraWidth = size.getWidth();
                mCameraHeight = size.getHeight();
            }
            Log.i(TAG, "openCamera. facing:" + (mCameraFacing == FACE_FRONT ? "front" : "back")
                    + ", orientation:" + mCameraOrientation + ", previewWidth:" + mCameraWidth
                    + ", previewHeight:" + mCameraHeight + ", thread:" + Thread.currentThread().getName());
            mYuvDataBufferArray = new byte[PREVIEW_BUFFER_SIZE][mCameraWidth * mCameraHeight * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
            mImageReader = ImageReader.newInstance(mCameraWidth, mCameraHeight, ImageFormat.YUV_420_888, PREVIEW_BUFFER_SIZE);
            mImageReader.setOnImageAvailableListener(this, mainHandler);
            mCameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(TAG, "onCameraOpened: " + camera + ", thread:" + Thread.currentThread().getName());
                    mCameraDevice = camera;
                    if (mViewWidth > 0 && mViewHeight > 0) {
                        mMvpMatrix = OpenGLUtil.changeMvpMatrixCrop(mViewWidth, mViewHeight, mCameraHeight, mCameraWidth);
                    }
                    //1不同
                    if (switchCamera != null) {
                        switchCamera.onSwitchCamera(cameraFace, mCameraOrientation);
                    }
                    if (null != surfaceTexture) {
                        startPreview(surfaceTexture, mCameraTexId);
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.d(TAG, "onCameraDisconnected: " + camera);
                    camera.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(TAG, "onOpenCameraError: " + error);
                    camera.close();
                    mCameraDevice = null;
                }
            }, mainHandler);
        } catch (Exception e) {
            Log.e(TAG, "openCamera: ", e);
        }
    }

    public void startPreview(SurfaceTexture surfaceTexture, int cameraTexId) {
        mCameraTexId = cameraTexId;
        if (cameraTexId <= 0 || mCameraDevice == null) {
            return;
        }
        Log.i(TAG, "startPreview. cameraTexId:" + mCameraTexId + ", cameraDevice:" + mCameraDevice);
        surfaceTexture.setDefaultBufferSize(mCameraWidth, mCameraHeight);
        try {
            Range<Integer> rangeFps = getBestRange();
            Log.d(TAG, "startPreview. rangeFPS: " + rangeFps);
            CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            if (rangeFps != null) {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, rangeFps);
            }
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, false);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder.addTarget(previewSurface);
            Surface imageReaderSurface = mImageReader.getSurface();
            captureRequestBuilder.addTarget(imageReaderSurface);
            mCaptureRequestBuilder = captureRequestBuilder;
            List<Surface> surfaceList = Arrays.asList(previewSurface, imageReaderSurface);
            mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "onConfigured: " + session + ", thread:" + Thread.currentThread().getName());
                    mIsPreviewing = true;
                    mCameraCaptureSession = session;
                    CaptureRequest captureRequest = mCaptureRequestBuilder.build();
                    try {
                        // 设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                        session.setRepeatingRequest(captureRequest, mCaptureCallback, mainHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "setRepeatingRequest: ", e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "onConfigureFailed: " + session);
                    mIsPreviewing = false;
                }
            }, mainHandler);
        } catch (Exception e) {
            Log.e(TAG, "startPreview: ", e);
        }
    }

    private CameraCharacteristics getCurrentCameraInfo() {
        return mCameraFacing == FACE_FRONT ? mFrontCameraCharacteristics : mBackCameraCharacteristics;
    }

    private boolean isMeteringAreaAFSupported() {
        Integer masRegionsAF = getCurrentCameraInfo().get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        if (masRegionsAF != null) {
            return masRegionsAF >= 1;
        } else {
            return false;
        }
    }

    public void handleFocus(float rawX, float rawY, int areaSize) {
        if (mCameraCaptureSession == null) {
            return;
        }
        if (!isMeteringAreaAFSupported()) {
            Log.e(TAG, "handleFocus not supported");
            return;
        }

        CameraCharacteristics cameraCharacteristics = getCurrentCameraInfo();
        final Rect sensorArraySize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        //here I just flip x,y, but this needs to correspond with the sensor orientation (via SENSOR_ORIENTATION)
        final int y = (int) ((rawX / mViewWidth) * (float) sensorArraySize.height());
        final int x = (int) ((rawY / mViewHeight) * (float) sensorArraySize.width());
        final int halfTouchWidth = areaSize / 2;
        final int halfTouchHeight = areaSize / 2;
        MeteringRectangle focusAreaTouch = new MeteringRectangle(Math.max(x - halfTouchWidth, 0),
                Math.max(y - halfTouchHeight, 0),
                halfTouchWidth * 2,
                halfTouchHeight * 2,
                MeteringRectangle.METERING_WEIGHT_MAX - 1);

        try {
            mCameraCaptureSession.stopRepeating();
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            MeteringRectangle[] meteringRectangles = new MeteringRectangle[]{focusAreaTouch};
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, meteringRectangles);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "setExposureCompensation: ", e);
        }
    }

    public float getExposureCompensation() {
        CameraCharacteristics cameraCharacteristics = getCurrentCameraInfo();
        Range<Integer> range = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        int min = -1;
        int max = 1;
        if (range != null) {
            min = range.getLower();
            max = range.getUpper();
        }
        Integer progressI = mCaptureRequestBuilder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION);
        int progress = 0;
        if (progressI != null) {
            progress = progressI;
        }
        return (float) (progress - min) / (max - min);
    }

    public void setExposureCompensation(float value) {
        if (mCameraCaptureSession == null) {
            return;
        }
        CameraCharacteristics cameraCharacteristics = getCurrentCameraInfo();
        Range<Integer> range = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        if (range != null) {
            int min = range.getLower();
            int max = range.getUpper();
            int val = (int) (value * (max - min) + min);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, val);
            try {
                mCameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), mCaptureCallback, null);
            } catch (CameraAccessException e) {
                Log.e(TAG, "setExposureCompensation: ", e);
            }
        }
    }


    private Range<Integer> getBestRange() {
        Range<Integer> result = null;
        try {
            String cameraId = mCameraFacing == FACE_FRONT ? mFrontCameraId : mBackCameraId;
            CameraCharacteristics chars = mCameraManager.getCameraCharacteristics(cameraId);
            Range<Integer>[] ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            if (ranges != null) {
                for (Range<Integer> range : ranges) {
                    //帧率不能太低，大于10
                    if (range.getLower() < 10) {
                        continue;
                    }
                    if (result == null) {
                        result = range;
                    }
                    //FPS下限小于15，弱光时能保证足够曝光时间，提高亮度。range范围跨度越大越好，光源足够时FPS较高，预览更流畅，光源不够时FPS较低，亮度更好。
                    else if (range.getLower() <= 15 && (range.getUpper() - range.getLower()) > (result.getUpper() - result.getLower())) {
                        result = range;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getBestRange: ", e);
        }
        return result;
    }


    public int[] getCameraWH() {
        return new int[]{mCameraWidth, mCameraHeight};
    }

    public int getCameraOrientation() {
        return this.mCameraOrientation;
    }

    public void closeCamera() {
        Log.d(TAG, "closeCamera. thread:" + Thread.currentThread().getName());
        try {
            if (mCameraCaptureSession != null) {
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (Throwable e) {

        }
        mIsPreviewing = false;
    }

    public void setCameraFacing(int cameraFacing) {
        mCameraFacing = cameraFacing;
    }

    /**
     * 是否支持 Camera2
     *
     * @param context
     * @return
     */
    public static boolean hasCamera2(Context context) {
        if (context == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            assert manager != null;
            String[] idList = manager.getCameraIdList();
            boolean notNull = true;
            if (idList.length == 0) {
                notNull = false;
            } else {
                for (final String str : idList) {
                    if (str == null || str.trim().isEmpty()) {
                        notNull = false;
                        break;
                    }
                    final CameraCharacteristics characteristics = manager.getCameraCharacteristics(str);
                    Integer iSupportLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    if (iSupportLevel != null && iSupportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        notNull = false;
                        break;
                    }
                }
            }
            return notNull;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static int getCameraOrientation(int cameraFacing) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        int cameraId = -1;
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == cameraFacing) {
                cameraId = i;
                break;
            }
        }
        if (cameraId < 0) {
            // 没找到返回后置摄像头角度
            return 90;
        } else {
            return info.orientation;
        }
    }

    public static void setFocusModes(Camera.Parameters parameters) {
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        }
        Log.i(TAG, "setFocusModes: " + parameters.getFocusMode());
    }

    /**
     * 设置相机 FPS，选择尽可能大的范围
     *
     * @param parameters
     */
    public static void chooseFrameRate(Camera.Parameters parameters) {
        List<int[]> supportedPreviewFpsRanges = parameters.getSupportedPreviewFpsRange();
        if (DEBUG) {
            StringBuilder buffer = new StringBuilder();
            buffer.append('[');
            for (Iterator<int[]> it = supportedPreviewFpsRanges.iterator(); it.hasNext(); ) {
                buffer.append(Arrays.toString(it.next()));
                if (it.hasNext()) {
                    buffer.append(", ");
                }
            }
            buffer.append(']');
            Log.d(TAG, "chooseFrameRate: Supported FPS ranges " + buffer.toString());
        }
        // FPS下限小于 7，弱光时能保证足够曝光时间，提高亮度。
        // range 范围跨度越大越好，光源足够时FPS较高，预览更流畅，光源不够时FPS较低，亮度更好。
        int[] bestFrameRate = supportedPreviewFpsRanges.get(0);
        for (int[] fpsRange : supportedPreviewFpsRanges) {
            int thisMin = fpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
            int thisMax = fpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
            if (thisMin < 7000) {
                continue;
            }
            if (thisMin <= 15000 && thisMax - thisMin > bestFrameRate[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
                    - bestFrameRate[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]) {
                bestFrameRate = fpsRange;
            }
        }
        Log.i(TAG, "setPreviewFpsRange: [" + bestFrameRate[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + ", " + bestFrameRate[Camera.Parameters.PREVIEW_FPS_MAX_INDEX] + "]");
        parameters.setPreviewFpsRange(bestFrameRate[Camera.Parameters.PREVIEW_FPS_MIN_INDEX], bestFrameRate[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
    }

    public static int[] choosePreviewSize(Camera.Parameters parameters, int width, int height) {
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();

        for (Camera.Size size : supportedPreviewSizes) {
            if (size.width == width && size.height == height) {
                parameters.setPreviewSize(width, height);
                return new int[]{width, height};
            }
        }

        Log.e(TAG, "can not find a preview size that matches the provided width and height width = " + width + "height = " + height);
        Camera.Size ppsfv = parameters.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            parameters.setPreviewSize(ppsfv.width, ppsfv.height);
            return new int[]{ppsfv.width, ppsfv.height};
        }
        // else use whatever the default size is
        return new int[]{0, 0};
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                         int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        Comparator<Size> comparator = new Comparator<Size>() {
            @Override
            public int compare(Size lhs, Size rhs) {
                // We cast here to ensure the multiplications won't overflow
                return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                        (long) rhs.getWidth() * rhs.getHeight());
            }
        };
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, comparator);
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, comparator);
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * 设置视频防抖动
     *
     * @param parameters
     */
    public static void setVideoStabilization(Camera.Parameters parameters) {
        if (parameters.isVideoStabilizationSupported()) {
            if (!parameters.getVideoStabilization()) {
                parameters.setVideoStabilization(true);
                Log.i(TAG, "enable video stabilization");
            }
        } else {
            Log.i(TAG, "device does not support video stabilization");
        }
    }

    public static void setParameters(Camera camera, Camera.Parameters parameters) {
        if (camera != null && parameters != null) {
            try {
                camera.setParameters(parameters);
            } catch (Exception ex) {
                Log.w(TAG, "setParameters: ", ex);
            }
        }
    }

    private static void resetFocus(final Camera camera, final String focusMode) {
        ThreadExecutor.removeUiAllTasks();
        ThreadExecutor.runOnUiPostDelayed(() -> {
            try {
                camera.cancelAutoFocus();
                Camera.Parameters parameter = camera.getParameters();
                parameter.setFocusMode(focusMode);
                if (DEBUG) {
                    Log.d(TAG, "resetFocus focusMode:" + focusMode);
                }
                parameter.setFocusAreas(null);
                parameter.setMeteringAreas(null);
                setParameters(camera, parameter);
            } catch (Exception e) {
                if (DEBUG) {
                    Log.w(TAG, "resetFocus: ", e);
                }
            }
        }, FOCUS_TIME);
    }

    private static Rect calculateTapArea(float x, float y, int width, int height, int areaSize, int cameraFacing) {
        int centerX = (int) (x / width * 2000 - 1000);
        int centerY = (int) (y / height * 2000 - 1000);

        int top = clamp(centerX - areaSize / 2);
        int bottom = clamp(top + areaSize);
        int left = clamp(centerY - areaSize / 2);
        int right = clamp(left + areaSize);
        RectF rectF = new RectF(left, top, right, bottom);
        Matrix matrix = new Matrix();
        int flipX = cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT ? -1 : 1;
        matrix.setScale(flipX, -1);
        matrix.mapRect(rectF);
        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private static int clamp(int x) {
        return x > 1000 ? 1000 : (x < -1000 ? -1000 : x);
    }

    public static Map<String, String> getCameraParameters(Camera camera) {
        Map<String, String> result = new HashMap<>(64);
        try {
            Class camClass = camera.getClass();

            // Internally, Android goes into native code to retrieve this String
            // of values
            Method getNativeParams = camClass.getDeclaredMethod("native_getParameters");
            getNativeParams.setAccessible(true);

            // Boom. Here's the raw String from the hardware
            String rawParamsStr = (String) getNativeParams.invoke(camera);

            // But let's do better. Here's what Android uses to parse the
            // String into a usable Map -- a simple ';' StringSplitter, followed
            // by splitting on '='
            //
            // Taken from Camera.Parameters unflatten() method
            TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(';');
            splitter.setString(rawParamsStr);

            for (String kv : splitter) {
                int pos = kv.indexOf('=');
                if (pos == -1) {
                    continue;
                }
                String k = kv.substring(0, pos);
                String v = kv.substring(pos + 1);
                result.put(k, v);
            }

            // And voila, you have a map of ALL supported parameters
            return result;
        } catch (Exception ex) {
            Log.e(TAG, "ex:", ex);
        }

        // If there was any error, just return an empty Map
        Log.e(TAG, "Unable to retrieve parameters from Camera.");
        return result;
    }

    public void setCameraPreviewFrameListener(OnCamera2FrameListener mOnCameraListener) {
        this.mOnCameraListener = mOnCameraListener;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        // called on Camera1Renderer thread
        try (Image image = reader.acquireLatestImage()) {
            if (image == null) {
                return;
            }
            mYuvDataBuffer = mYuvDataBufferArray[mYuvDataBufferPosition];
            mYuvDataBufferPosition = ++mYuvDataBufferPosition % mYuvDataBufferArray.length;
            YUV420ToNV21(image);
            mCameraNv21Byte = mYuvDataBuffer;
            if (mOnCameraListener != null) {
                mOnCameraListener.onFrame(mCameraNv21Byte);
                //BitmapUtils.savePNG(BitmapUtils.getBitmap(mCameraNv21Byte,1280,720),"899999");
            }
            //mGlSurfaceView.requestRender();
        } catch (Exception e) {
            Log.e(TAG, "onImageAvailable: ", e);
        }
    }

    // call this method may cost 5+ms
    private void YUV420ToNV21(Image image) {
        Rect crop = image.getCropRect();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        if (mYDataBuffer == null) {
            mYDataBuffer = new byte[planes[0].getRowStride()];
        }
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;
                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;
                    break;
                default:
            }

            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(mYuvDataBuffer, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(mYDataBuffer, 0, length);
                    for (int col = 0; col < w; col++) {
                        mYuvDataBuffer[channelOffset] = mYDataBuffer[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
    }

    public interface OnCamera2FrameListener {
        void onFrame(byte[] nv21data);
    }

}
