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

import android.opengl.GLES20;

import com.aliyun.ai.viapi.gles.base.Base2DCoord;
import com.aliyun.ai.viapi.gles.util.OpenGLUtil;

public abstract class AbsTexFilter {

    protected int mProgram;
    protected int[] mFrameBuffers;
    protected int[] mFrameBufferTextures;
    protected static final int FRAME_BUFFER_NUM = 1;
    private int mTextureWidth;
    private int mTextureHeight;

    protected Base2DCoord mBase2DCoord;

    private int[] mOriginViewport = new int[4];


    public AbsTexFilter(String VERTEX_SHADER, String FRAGMENT_SHADER_2D) {
        mProgram = OpenGLUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_2D);
        mBase2DCoord = get2DCoord();
        getLocations();
    }

    public void updateVertexArray(float[] vertexArray) {
        mBase2DCoord.updateVertexArray(vertexArray);
    }

    public void updateTexCoordArray(float[] texCoordArray) {
        mBase2DCoord.updateTexCoordArray(texCoordArray);
    }

    public abstract int drawFrameOffScreen(int textureId, int width, int height, float[] texMatrix, float[] mpvMatrix);

    /**
     * Issues the draw call.  Does the full setup on every call.
     */
    public abstract void drawFrameOnScreen(int textureId, int width, int height, float[] texMatrix, float[] mvpMatrix);

    protected abstract Base2DCoord get2DCoord();

    protected abstract void getLocations();

    public abstract void drawFrame(int textureId, float[] texMatrix, float[] mvpMatrix, int width, int height);

    public void drawFrame(int textureId, float[] texMatrix, int width, int height) {
        drawFrame(textureId, texMatrix, OpenGLUtil.IDENTITY_MATRIX, width, height);
    }

    public void drawFrame(int textureId, float[] texMatrix, float[] mvpMatrix, int x, int y, int width, int height) {
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, mOriginViewport, 0);
        GLES20.glViewport(x, y, width, height);
        drawFrame(textureId, texMatrix, mvpMatrix, width, height);
        GLES20.glViewport(mOriginViewport[0], mOriginViewport[1], mOriginViewport[2], mOriginViewport[3]);
    }

    protected void initFrameBufferIfNeed(int width, int height) {
        boolean need = false;
        if (mTextureWidth != width || mTextureHeight != height) {
            mTextureWidth = width;
            mTextureHeight = height;
            destroyFrameBuffers();
            need = true;
        }
        if (mFrameBuffers == null || mFrameBufferTextures == null) {
            need = true;
        }
        if (need) {
            mFrameBuffers = new int[FRAME_BUFFER_NUM];
            mFrameBufferTextures = new int[FRAME_BUFFER_NUM];
            GLES20.glGenFramebuffers(FRAME_BUFFER_NUM, mFrameBuffers, 0);
            GLES20.glGenTextures(FRAME_BUFFER_NUM, mFrameBufferTextures, 0);
            for (int i = 0; i < FRAME_BUFFER_NUM; i++) {
                bindFrameBuffer(mFrameBufferTextures[i], mFrameBuffers[i], width, height);
            }
        }
    }

    /**
     * 纹理参数设置+buffer绑定
     * set texture params
     * and bind buffer
     */
    private void bindFrameBuffer(int textureId, int frameBuffer, int width, int height) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    /**
     * 销毁缓存帧
     */
    private void destroyFrameBuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(FRAME_BUFFER_NUM, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(FRAME_BUFFER_NUM, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    public void release() {
        destroyFrameBuffers();
        GLES20.glDeleteProgram(mProgram);
        mProgram = -1;
    }
}
