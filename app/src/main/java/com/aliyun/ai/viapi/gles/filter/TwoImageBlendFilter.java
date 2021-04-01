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
import com.aliyun.ai.viapi.gles.util.OpenGLUtil;

public class TwoImageBlendFilter extends Normal2DTexFilter {
    // 定义定点shader
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "uniform mat4 uTexMatrix2;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "attribute vec4 aTextureCoord2;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "varying vec2 vTextureCoord2;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = ( uTexMatrix * aTextureCoord).xy;\n" +
                    "    vTextureCoord2 = (uTexMatrix2 * aTextureCoord2).xy;\n" +
                    "}\n";

    public static final String BLEND_FRAGMENT_SHADER_2D =
            "varying highp vec2 vTextureCoord;\n" +
                    " varying highp vec2 vTextureCoord2;\n" +
                    " \n" +
                    " uniform sampler2D sTexture;\n" +
                    " uniform sampler2D sTexture2;\n" +
                    " \n" +
                    " void main()\n" +
                    " {\n" +
                    "     lowp vec4 c1 = texture2D(sTexture, vTextureCoord);\n" +
                    "\t lowp vec4 c2 = texture2D(sTexture2, vTextureCoord2);\n" +
                    "     \n" +
                    "     lowp vec4 outputColor;\n" +
                    "     \n" +
                    "     outputColor.r = c1.r + c2.r * c2.a * (1.0 - c1.a);\n" +
                    "\n" +
                    "     outputColor.g = c1.g + c2.g * c2.a * (1.0 - c1.a);\n" +
                    "     \n" +
                    "     outputColor.b = c1.b + c2.b * c2.a * (1.0 - c1.a);\n" +
                    "     \n" +
                    "     outputColor.a = c1.a + c2.a * (1.0 - c1.a);\n" +
                    "     \n" +
                    "     gl_FragColor = outputColor;\n" +
                    " }";

    private int maTextureCoordLoc2;
    private int mTexture2;
    private int mTexture2Id = OpenGLUtil.NO_TEXTURE;
    private int muTexMatrixLoc2;
    private float[] mTex2Matrix;

    public TwoImageBlendFilter() {
        super(VERTEX_SHADER, BLEND_FRAGMENT_SHADER_2D);
    }

    @Override
    protected void getLocations() {
        super.getLocations();
        maTextureCoordLoc2 = GLES20.glGetAttribLocation(mProgram, "aTextureCoord2");
        OpenGLUtil.checkLocation(maTextureCoordLoc2, "aTextureCoord2");
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc2);
        mTexture2 = GLES20.glGetUniformLocation(mProgram, "sTexture2");
        OpenGLUtil.checkLocation(mTexture2, "sTexture2");
        muTexMatrixLoc2 = GLES20.glGetUniformLocation(mProgram, "uTexMatrix2");
        OpenGLUtil.checkLocation(muTexMatrixLoc2, "uTexMatrix2");
    }

    @Override
    protected void onDrawArraysPre(float[] tex2Matrix) {
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc2);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture2Id);

        GLES20.glUniformMatrix4fv(muTexMatrixLoc2, 1, false, tex2Matrix, 0);
        OpenGLUtil.checkGLError("glUniformMatrix4fv");

        GLES20.glUniform1i(mTexture2, 3);

        GLES20.glVertexAttribPointer(maTextureCoordLoc2, 2,
                GLES20.GL_FLOAT, false, Base2DCoord.TEXTURE_COORD_STRIDE, mBase2DCoord.texCoordArray());
        OpenGLUtil.checkGLError("glVertexAttribPointer");
    }

    @Override
    public void setTex2Matrix(float[] tex2Matrix) {
        this.mTex2Matrix = tex2Matrix;
    }

    @Override
    public float[] getTex2Matrix() {
        return mTex2Matrix;
    }

    @Override
    public void setBitmap(final Bitmap bitmap) {
        if (bitmap != null && bitmap.isRecycled()) {
            return;
        }
        if (bitmap == null) {
            mTexture2Id = OpenGLUtil.NO_TEXTURE;
            return;
        }
        runOnDraw(() -> {
            if (mTexture2Id == OpenGLUtil.NO_TEXTURE) {
                if (bitmap.isRecycled()) {
                    return;
                }
                GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
                mTexture2Id = OpenGLUtil.loadTexture(bitmap, OpenGLUtil.NO_TEXTURE, false);
            } else {
                if (mTexture2Id != 0) {
                    GLES20.glDeleteTextures(1, new int[]{mTexture2Id}, 0);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
                    mTexture2Id = OpenGLUtil.loadTexture(bitmap, OpenGLUtil.NO_TEXTURE, false);
                }
            }
        });
    }

    @Override
    public void release() {
        super.release();
        if (mTexture2Id != 0) {
            GLES20.glDeleteTextures(1, new int[]{mTexture2Id}, 0);
            mTexture2Id = 0;
        }
    }
}
