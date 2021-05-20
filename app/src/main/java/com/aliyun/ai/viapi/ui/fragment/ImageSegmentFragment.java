package com.aliyun.ai.viapi.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.aliyun.ai.viapi.HumanPhotoSegment;
import com.aliyun.ai.viapi.R;
import com.aliyun.ai.viapi.core.VIAPIStatusCode;
import com.aliyun.ai.viapi.gles.util.BitmapUtils;
import com.aliyun.ai.viapi.gles.util.OpenGLUtil;
import com.aliyun.ai.viapi.gles.util.ToastUtil;
import com.aliyun.ai.viapi.renderer.segment.ImageRenderer;
import com.aliyun.ai.viapi.util.AssetsProvider;
import com.aliyun.ai.viapi.util.Logs;
import com.aliyun.ai.viapi.util.PictureUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * @author: created by chehongpeng
 * @date: 2021/4/2
 * @Description:
 */
public class ImageSegmentFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = ImageSegmentFragment.class.getSimpleName();
    private static final int CHOICE_PHOTO_ALBUM = 1;

    private ImageView mIvOriginal;
    private FrameLayout mFlResult;

    private Bitmap mOriginalBitmap;
    private Bitmap mSegmentBitmap;
    private HumanPhotoSegment mHumanSegment;
    private boolean isSegmenting;

    private volatile AtomicBoolean mInitSegment = new AtomicBoolean(false);

    public ImageSegmentFragment() {
    }

    public static ImageSegmentFragment newInstance(String param1, String param2) {
        ImageSegmentFragment fragment = new ImageSegmentFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image_segment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        initSegment();
    }

    private void initView(View view) {
        mIvOriginal = view.findViewById(R.id.iv_original);
        mFlResult = view.findViewById(R.id.fl_result);
        view.findViewById(R.id.btn_choice).setOnClickListener(this);
        view.findViewById(R.id.btn_seg).setOnClickListener(this);
    }

    /**
     * 此方法比较耗时，需要在后台线程调用
     */
    @SuppressLint("CheckResult")
    public void initSegment() {
        //实例化算法对象
        mHumanSegment = new HumanPhotoSegment();
        Single.fromCallable(() -> {
            String modelsPath = AssetsProvider.getPhotoSegmentModelsPath(getContext());
            int status = mHumanSegment.nativeSegmentCreate();
            if (status != 0) {
                return status;
            }
            status = mHumanSegment.nativeSegmentInit(modelsPath);
            if (status != 0) {
                return status;
            }
            mInitSegment.set(true);
            return status;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {
                    Toast.makeText(getContext(), getString(R.string.common_init_fail)
                                    + ",status = " + VIAPIStatusCode.getErrorMsg(status),
                            Toast.LENGTH_LONG).show();
                    Log.i(TAG, "initPhotoSegment status = " + status);

                }, err -> Logs.e(TAG, "ImageSegmentFragment initSegment: " + err.toString()));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //选择图片
            case R.id.btn_choice:
                choiceImg(CHOICE_PHOTO_ALBUM);
                break;
            //裁剪
            case R.id.btn_seg:
                if (mFlResult.getChildCount() > 0) {
                    mFlResult.removeAllViews();
                }
                aliSegImage(compressBitmap(mOriginalBitmap));
                break;
            default:
                break;
        }
    }

    private void choiceImg(int requestCode) {
        //打开系统相册
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, requestCode);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //用户操作完成，结果码返回是-1，即RESULT_OK
        switch (requestCode) {
            case CHOICE_PHOTO_ALBUM:
                if (resultCode == Activity.RESULT_OK) {
                    //获取选中文件的定位符
                    Uri uri = data.getData();
                    String imagePath = PictureUtil.getPathByUri(getContext(), uri);
                    int degree = PictureUtil.readPictureDegree(imagePath);
                    mOriginalBitmap = BitmapFactory.decodeFile(imagePath);
                    if (degree % 360 != 0) {
                        mOriginalBitmap = PictureUtil.toTurn(mOriginalBitmap, degree);
                    }
                    if (mOriginalBitmap != null) {
                        mIvOriginal.setImageBitmap(mOriginalBitmap);
                        mFlResult.removeAllViews();
                    }
                } else {
                    //操作错误或没有选择图片
                    Log.e(TAG, "operation error");
                }
                break;
        }
    }

    /**
     * 对图片进行尺寸压缩
     */
    private Bitmap compressBitmap(Bitmap bitmap) {
        float maxLength = Math.max(bitmap.getWidth(), bitmap.getHeight());
        float maxTextureSize = OpenGLUtil.getMaxTextureSize();
        Log.d(TAG, "width is [" + bitmap.getWidth() + "]" + ", height is [" + bitmap.getHeight() + "]");
        Log.d(TAG, "maxTextureSize: " + maxTextureSize);
        if (maxLength > maxTextureSize) {
            return BitmapUtils.compressBitmap(bitmap, maxTextureSize / maxLength);
        }
        return bitmap;
    }

    /**
     * 对传入的图片进行抠图处理
     */
    @SuppressLint("CheckResult")
    private void aliSegImage(Bitmap bitmap) {
        if (!mInitSegment.get()) {
            Toast.makeText(getContext(), "算法尚未初始化", Toast.LENGTH_LONG).show();
            return;
        }

        if (bitmap == null) {
            Toast.makeText(getContext(), "请选择图片", Toast.LENGTH_LONG).show();
            return;
        }

        if (isSegmenting) {
            ToastUtil.showToast(getContext(), "请稍等");
            return;
        }

        isSegmenting = true;

        Single.fromCallable(() -> {
            ByteBuffer originalBuffer = ByteBuffer.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4);
            ByteBuffer dstBuffer = ByteBuffer.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4);
            bitmap.copyPixelsToBuffer(originalBuffer);
            Log.d(TAG, "width is [" + bitmap.getWidth() + "]" + ", height is [" + bitmap.getHeight() + "]");
            int lastProcessResult = mHumanSegment.nativeSegmentProcess(originalBuffer.array(), bitmap.getWidth(), bitmap.getHeight(), 4, dstBuffer.array());
            Log.d(TAG, "lastProcessResult的值: " + lastProcessResult);
            return dstBuffer;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(byteBuffer -> {
//                    mSegmentBitmap = Bitmap.createBitmap(
//                            bitmap.getWidth(),
//                            bitmap.getHeight(),
//                            Bitmap.Config.ARGB_8888
//                    );
//                    mSegmentBitmap.copyPixelsFromBuffer(byteBuffer);
//                    mIvResult.setImageBitmap(mSegmentBitmap);
                    GLSurfaceView glImageView = new GLSurfaceView(getContext());
                    glImageView.setEGLContextClientVersion(OpenGLUtil.getSupportGLVersion(getContext()));
                    glImageView.setRenderer(new ImageRenderer(bitmap.getWidth(), bitmap.getHeight(), byteBuffer));
                    glImageView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                    mFlResult.addView(glImageView);
                    isSegmenting = false;
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHumanSegment != null) {
            mHumanSegment.nativeSegmentClear();
            mHumanSegment.nativeSegmentDestroy();
        }
    }
}
