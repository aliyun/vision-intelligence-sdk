package com.aliyun.ai.viapi.ui.widget;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.aliyun.ai.viapi.renderer.segment.BaseRenderer;

/**
 * @author: created by hanbing
 * @date: 2020/11/27
 * @Description:
 */
public class GLImageView extends GLSurfaceView {

    public GLImageView(Context context) {
        this(context, null);
    }

    public GLImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setViewRenderer(BaseRenderer mRenderer) {
        super.setRenderer(mRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
    }
}
