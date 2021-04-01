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

import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.aliyun.ai.viapi.gles.base.Base2DCoord;
import com.aliyun.ai.viapi.gles.base.NormalCoord;
import com.aliyun.ai.viapi.gles.util.OpenGLUtil;

import java.util.LinkedList;

import static android.opengl.GLES20.GL_FRAMEBUFFER;

public class Normal2DTexFilter extends AbsTexFilter {

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER_2D =
            "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    // 为blend新增
    private int mTexture;
    private final LinkedList<Runnable> runOnDraw;

    public Normal2DTexFilter() {
        this(VERTEX_SHADER, FRAGMENT_SHADER_2D);
    }

    public Normal2DTexFilter(String VERTEX_SHADER, String FRAGMENT_SHADER_2D) {
        super(VERTEX_SHADER, FRAGMENT_SHADER_2D);
        runOnDraw = new LinkedList<>();
    }

    @Override
    public int drawFrameOffScreen(int textureId, int width, int height, float[] texMatrix, float[] mvpMatrix) {

        OpenGLUtil.checkGLError("draw start");

        initFrameBufferIfNeed(width, height);
        OpenGLUtil.checkGLError("initFrameBufferIfNeed");

        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]);
        OpenGLUtil.checkGLError("glBindFramebuffer");

        // Select the program.
        GLES20.glUseProgram(mProgram);
        OpenGLUtil.checkGLError("glUseProgram");
        // new add for blend
        runPendingOnDrawTasks();

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        OpenGLUtil.checkGLError("glBindTexture");
        // 为blend新增
        GLES20.glUniform1i(mTexture, 0);

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        OpenGLUtil.checkGLError("glUniformMatrix4fv");

        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        OpenGLUtil.checkGLError("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        OpenGLUtil.checkGLError("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, mBase2DCoord.COORD_SIZE_PER_VERTEX,
                GLES20.GL_FLOAT, false, mBase2DCoord.VERTEX_STRIDE, mBase2DCoord.vertexArray());
        OpenGLUtil.checkGLError("glVertexAttribPointer");

        // 新增为blend
        onDrawArraysPre(getTex2Matrix());

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        OpenGLUtil.checkGLError("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, mBase2DCoord.TEXTURE_COORD_STRIDE, mBase2DCoord.texCoordArray());
        OpenGLUtil.checkGLError("glVertexAttribPointer");

        GLES20.glViewport(0, 0, width, height);

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mBase2DCoord.vertexCount());
        OpenGLUtil.checkGLError("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glUseProgram(0);
        return mFrameBufferTextures[0];
    }

    @Override
    public void drawFrameOnScreen(int textureId, int width, int height, float[] texMatrix, float[] mvpMatrix) {
        OpenGLUtil.checkGLError("draw start");

        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        // Select the program.
        GLES20.glUseProgram(mProgram);
        OpenGLUtil.checkGLError("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        OpenGLUtil.checkGLError("glUniformMatrix4fv");

        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        OpenGLUtil.checkGLError("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        OpenGLUtil.checkGLError("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, mBase2DCoord.COORD_SIZE_PER_VERTEX,
                GLES20.GL_FLOAT, false, mBase2DCoord.VERTEX_STRIDE, mBase2DCoord.vertexArray());
        OpenGLUtil.checkGLError("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        OpenGLUtil.checkGLError("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, mBase2DCoord.TEXTURE_COORD_STRIDE, mBase2DCoord.texCoordArray());
        OpenGLUtil.checkGLError("glVertexAttribPointer");

        GLES20.glViewport(0, 0, width, height);


        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mBase2DCoord.vertexCount());
        OpenGLUtil.checkGLError("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
    }

    @Override
    protected Base2DCoord get2DCoord() {
        return new NormalCoord();
    }

    @Override
    protected void getLocations() {
        maPositionLoc = GLES20.glGetAttribLocation(mProgram, "aPosition");
        OpenGLUtil.checkLocation(maPositionLoc, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        OpenGLUtil.checkLocation(maTextureCoordLoc, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        OpenGLUtil.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgram, "uTexMatrix");
        OpenGLUtil.checkLocation(muTexMatrixLoc, "uTexMatrix");
        // 为blend新增
        mTexture = GLES20.glGetUniformLocation(mProgram, "sTexture");
        OpenGLUtil.checkLocation(mTexture, "sTexture");
    }

    @Override
    public void drawFrame(int textureId, float[] texMatrix, float[] mvpMatrix, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        OpenGLUtil.checkGLError("draw start");
        GLES20.glUseProgram(mProgram);
        OpenGLUtil.checkGLError("glUseProgram");
        // new add for blend
        runPendingOnDrawTasks();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        // 为blend新增
        GLES20.glUniform1i(mTexture, 0);

        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        OpenGLUtil.checkGLError("glUniformMatrix4fv");

        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        OpenGLUtil.checkGLError("glUniformMatrix4fv");

        GLES20.glEnableVertexAttribArray(maPositionLoc);
        OpenGLUtil.checkGLError("glEnableVertexAttribArray");

        GLES20.glVertexAttribPointer(maPositionLoc, Base2DCoord.COORD_SIZE_PER_VERTEX,
                GLES20.GL_FLOAT, false, Base2DCoord.VERTEX_STRIDE, mBase2DCoord.vertexArray());
        OpenGLUtil.checkGLError("glVertexAttribPointer");

        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        OpenGLUtil.checkGLError("glEnableVertexAttribArray");

        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, Base2DCoord.TEXTURE_COORD_STRIDE, mBase2DCoord.texCoordArray());
        OpenGLUtil.checkGLError("glVertexAttribPointer");

        // 新增为blend
        onDrawArraysPre(getTex2Matrix());

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mBase2DCoord.vertexCount());
        OpenGLUtil.checkGLError("glDrawArrays");

        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
    }

    protected void runPendingOnDrawTasks() {
        synchronized (runOnDraw) {
            while (!runOnDraw.isEmpty()) {
                runOnDraw.removeFirst().run();
            }
        }
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (runOnDraw) {
            runOnDraw.addLast(runnable);
        }
    }

    protected void onDrawArraysPre(float[] tex2Matrix) {
    }

    public void setBitmap(final Bitmap bitmap) {
    }

    public void setTex2Matrix(float[] tex2Matrix) {
    }

    public float[] getTex2Matrix() {
        return null;
    }
}
