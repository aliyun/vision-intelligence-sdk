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
package com.aliyun.ai.viapi.renderer;

public interface IRendererListener extends ISwitchCamera {

    void onSurfaceCreated();

    /**
     * @param viewWidth
     * @param viewHeight
     */
    void onSurfaceChanged(int viewWidth, int viewHeight);

    /**
     * @param yuv420sp
     * @param cameraTexId
     * @param textureWidth
     * @param textureHeight
     * @param mvpMatrix
     * @param texMatrix
     * @param timeStamp
     * @return
     */
    int onDrawFrame(byte[] yuv420sp, int cameraTexId, int textureWidth, int textureHeight, float[] mvpMatrix, float[] texMatrix, long timeStamp);

    void onSurfaceDestroy();
}
