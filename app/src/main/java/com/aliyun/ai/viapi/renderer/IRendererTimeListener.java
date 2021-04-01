package com.aliyun.ai.viapi.renderer;

public interface IRendererTimeListener {
    void onFpsChange(double fps);

    //图像分割合成2d纹理时间记录
    void onCompose2dTexIdBeginTime();

    void onCompose2dTexIdEndTime();

    //渲染到屏幕时间记录
    void onFrameOnScreenBeginTime();

    void onFrameOnScreenEndTime();

    //总时间的回调
    void onTotalRenderTime();
}
