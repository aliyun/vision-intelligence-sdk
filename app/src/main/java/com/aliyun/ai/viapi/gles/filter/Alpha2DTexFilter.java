package com.aliyun.ai.viapi.gles.filter;

/**
 * @author: created by hanbing
 * @date: 2020/12/1
 * @Description:
 */
public class Alpha2DTexFilter extends Normal2DTexFilter {
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
                    "    gl_FragColor = vec4(texture2D(sTexture, vTextureCoord).rgb, 1.0);\n" +
                    "}\n";


    public Alpha2DTexFilter() {
        super(VERTEX_SHADER, FRAGMENT_SHADER_2D);
    }
}
