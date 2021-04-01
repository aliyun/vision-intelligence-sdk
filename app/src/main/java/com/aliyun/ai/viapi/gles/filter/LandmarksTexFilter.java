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
package com.aliyun.ai.viapi.gles.filter;

import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.aliyun.ai.viapi.gles.base.Base2DCoord;
import com.aliyun.ai.viapi.gles.util.OpenGLUtil;

public class LandmarksTexFilter extends AbsTexFilter {

    private static final String vertexShaderCode =

            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "uniform float uPointSize;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  gl_PointSize = uPointSize;" +
                    "}";

    private static final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private static final float[] POINT_COLOR = {1.0f, 0f, 0f, 1.0f};
    private static final float POINT_SIZE = 6.0f;

    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;
    private int mPointSizeHandle;

    public LandmarksTexFilter() {
        super(vertexShaderCode, fragmentShaderCode);
    }

    @Override
    public int drawFrameOffScreen(int textureId, int width, int height, float[] texMatrix, float[] mvpMatrix) {
        return 0;
    }

    @Override
    public void drawFrameOnScreen(int textureId, int width, int height, float[] texMatrix, float[] mvpMatrix) {

    }

    @Override
    protected Base2DCoord get2DCoord() {
        return new Base2DCoord(new float[75 * 2]);
    }

    @Override
    protected void getLocations() {
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        OpenGLUtil.checkGLError("vPosition");
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        OpenGLUtil.checkGLError("vColor");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        OpenGLUtil.checkGLError("glGetUniformLocation");
        mPointSizeHandle = GLES20.glGetUniformLocation(mProgram, "uPointSize");
        OpenGLUtil.checkGLError("uPointSize");
    }

    @Override
    public void drawFrame(int textureId, float[] texMatrix, float[] mvpMatrix, int width, int height) {
        GLES20.glUseProgram(mProgram);
        GLES20.glEnableVertexAttribArray(mPositionHandle);


        GLES20.glVertexAttribPointer(
                mPositionHandle, Base2DCoord.COORD_SIZE_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                Base2DCoord.VERTEX_STRIDE, mBase2DCoord.vertexArray());

        // 设置颜色
        GLES20.glUniform4fv(mColorHandle, 1, POINT_COLOR, 0);

        // 投影与视图变换矩阵
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glUniform1f(mPointSizeHandle, POINT_SIZE);

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mBase2DCoord.vertexCount());
        // 禁用定点handle
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glUseProgram(0);
    }

    public void drawFrame(int x, int y, int width, int height) {
        drawFrame(0, null, mMvpMatrix, x, y, width, height);
    }

    private final float[] mMvpMatrix = new float[16];
    private int mCameraType;
    private int mCameraOrientation;
    private int mCameraWidth;
    private int mCameraHeight;

    public void refresh(float[] landmarksData, int cameraWidth, int cameraHeight, int cameraOrientation, int cameraType, float[] mvpMatrix) {
        if (mCameraWidth != cameraWidth || mCameraHeight != cameraHeight || mCameraOrientation != cameraOrientation || mCameraType != cameraType) {
            float[] orthoMtx = new float[16];
            Matrix.orthoM(orthoMtx, 0, 0, cameraWidth, 0, cameraHeight, -1, 1);
            float[] rotateMtx = new float[16];
            Matrix.setRotateM(rotateMtx, 0, 360 - cameraOrientation, 0.0f, 0.0f, 1.0f);
            if (cameraType == Camera.CameraInfo.CAMERA_FACING_BACK) {
                Matrix.rotateM(rotateMtx, 0, 180, 1.0f, 0.0f, 0.0f);
            }
            float[] temp = new float[16];
            Matrix.multiplyMM(temp, 0, rotateMtx, 0, orthoMtx, 0);
            Matrix.multiplyMM(mMvpMatrix, 0, mvpMatrix, 0, temp, 0);

            mCameraWidth = cameraWidth;
            mCameraHeight = cameraHeight;
            mCameraOrientation = cameraOrientation;
            mCameraType = cameraType;
        }

        updateVertexArray(landmarksData);
    }
}
