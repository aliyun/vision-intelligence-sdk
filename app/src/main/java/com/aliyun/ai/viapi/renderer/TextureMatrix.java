package com.aliyun.ai.viapi.renderer;

/**
 * @author: created by hanbing
 * @date: 2021/3/17
 * @Description:
 */
public class TextureMatrix {


    public static final float[] DINGDING_TEXTURE_MATRIX = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 1.0f
    };
    //    private static final float[] TEXTURE_MATRIX = {
//            0.0f, -1.0f, 0.0f, 0.0f,
//            1.0f, 0.0f, 0.0f, 0.0f,
//            0.0f, 0.0f, 1.0f, 0.0f,
//            0.0f, 1.0f, 0.0f, 1.0f
//    };
    //X 轴旋转 180 // 中心点需要在加一下//
    public static final float[] TEXTURE_MATRIX = {
            0.0f, 1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };
    //Y 轴旋转
     /*public static final float[] TEXTURE_MATRIX = {
             1.0f, 0.0f, 0.0f, 0.0f,
             0.0f, 0.0f, 1.0f, 0.0f,
             0.0f, 1.0f, 0.0f, 0.0f,
             0.0f, 0.0f, 0.0f, 1.0f
     };*/

    // X轴旋转 90度
    public static final float[] TEXTURE_MATRIX_90 = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -1.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };
    // X轴旋转 180度
    public static final float[] TEXTURE_MATRIX_180 = {
            -1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };
    // X轴旋转 270度
    public static final float[] TEXTURE_MATRIX_270 = {
            0.0f, 1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };
}
