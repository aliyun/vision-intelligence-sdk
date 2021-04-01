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
package com.aliyun.ai.viapi.gles.base;

import com.aliyun.ai.viapi.gles.util.OpenGLUtil;

import java.nio.FloatBuffer;

public class Base2DCoord {

    public static final int SIZEOF_FLOAT = 4;
    public static final int COORD_SIZE_PER_VERTEX = 2;
    public static final int TEXTURE_COORD_STRIDE = COORD_SIZE_PER_VERTEX * SIZEOF_FLOAT;
    public static final int VERTEX_STRIDE = COORD_SIZE_PER_VERTEX * SIZEOF_FLOAT;


    private FloatBuffer mTexCoordArray;
    private FloatBuffer mVertexArray;
    private int mVertexCount;

    public Base2DCoord() {
    }

    public Base2DCoord(float[] vertexArray, float[] texCoordArray) {
        updateVertexArray(vertexArray);
        updateTexCoordArray(texCoordArray);
    }

    public Base2DCoord(float[] vertexArray) {
        updateVertexArray(vertexArray);
    }

    public void updateVertexArray(float[] vertexArray) {
        mVertexArray = OpenGLUtil.createFloatBuffer(vertexArray);
        mVertexCount = vertexArray.length / COORD_SIZE_PER_VERTEX;
    }

    public void updateTexCoordArray(float[] coordArray) {
        mTexCoordArray = OpenGLUtil.createFloatBuffer(coordArray);
    }


    public FloatBuffer vertexArray() {
        return mVertexArray;
    }


    public FloatBuffer texCoordArray() {
        return mTexCoordArray;
    }


    public int vertexCount() {
        return mVertexCount;
    }

}
