package com.aliyun.ai.viapi.renderer.segment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.widget.Toast;

import com.aliyun.ai.viapi.HumanSegment;
import com.aliyun.ai.viapi.VIAPISdkApp;
import com.aliyun.ai.viapi.core.VIAPICreateApi;
import com.aliyun.ai.viapi.gles.util.BitmapUtils;
import com.aliyun.ai.viapi.gles.util.OpenGLUtil;
import com.aliyun.ai.viapi.renderer.IVIRendererUIStatus;
import com.aliyun.ai.viapi.ui.model.BaseVBItem;
import com.aliyun.ai.viapi.ui.model.VBCardType;
import com.aliyun.ai.viapi.ui.vb.VBConfRecord;
import com.aliyun.ai.viapi.util.AssetsProvider;
import com.aliyun.ai.viapi.util.Logs;
import com.aliyun.ai.viapi.util.TakePictureUtil;
import com.aliyun.ai.viapi.util.ThreadExecutor;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

/**
 * @author: created by hanbing
 * @date: 2020/11/29
 * @Description:
 */
public class VideoRenderer {
    public final static String TAG = VideoRenderer.class.getSimpleName();
    private final Context mContext;
    private final AtomicBoolean mInitSegment = new AtomicBoolean(false);
    private final AtomicBoolean mIsNeedTakePicture = new AtomicBoolean(false);
    private final TakePictureUtil mTakePictureUtil = new TakePictureUtil();
    private final HashMap<Integer, Bitmap> mBgAngleMap = new HashMap<>();
    private final HumanSegment mHumanSegment;
    private final BaseRenderer mCameraRenderer;
    private IVIRendererUIStatus mIVIRendererUIStatus;
    private final Object mInitLock = new Object();
    private int mSegmentTexId = OpenGLUtil.NO_TEXTURE;
    private int mTexHeight;
    private int mTexWeight;
    private int sep = 0;
    private long mAlgorithmTime;
    private long mLoadTextureTime;
    private ByteBuffer mDstBuffer;
    private Bitmap blendImageBg;
    private String mVBName;

    public VideoRenderer(Context context, BaseRenderer renderer) {
        mCameraRenderer = renderer;
        mContext = context;
        mHumanSegment = new HumanSegment();
        Logs.i(TAG, "create VIRenderer");
    }

    /**
     * 此方法比较耗时，需要在后台线程调用
     */
    @SuppressLint("CheckResult")
    public void initSegment() {
        Completable.complete().subscribeOn(Schedulers.single()).subscribe(this::initHumanSegment);
    }

    private void initHumanSegment() {
        if (mInitSegment.get()) {
            return;
        }
        createSelectedVBImageFromConf();
        if (TextUtils.isEmpty(mVBName) || VBCardType.NONE.getCardName().equals(mVBName)) {
            return;
        }
        ThreadExecutor.runOnMainThread(() -> {
            if (mIVIRendererUIStatus != null) {
                mIVIRendererUIStatus.showLoading();
            }
        });
//        if (!AssetsProvider.copySegmentModeFile(mContext)) {
//            Logs.e(TAG, "copySegmentModeFile fail ");
//            return;
//        }
        final Integer[] status = new Integer[1];
        String modelsPath = AssetsProvider.getVideoSegmentModelsPath(mContext);
        String licensePath = VIAPICreateApi.getInstance().getVIAPISdkCore().getLicenseFilePath();
        synchronized (mInitLock) {
            status[0] = mHumanSegment.nativeCheckLicense(licensePath);
            if (status[0] != 0) {
                Logs.e(TAG, "VIRenderer -> checkingAuth error =  " + status[0]);
                ThreadExecutor.runOnMainThread(() -> Toast.makeText(VIAPISdkApp.getContext(),
                        "checkingAuth failed status = " + status[0], Toast.LENGTH_LONG).show());
                return;
            }
            String expireTime = mHumanSegment.nativeGetLicenseExpireTime();
            Logs.i(TAG, "VIRenderer -> segment license expireTime =  " + expireTime);
            status[0] = mHumanSegment.nativeSegmentCreate();
            if (status[0] != 0) {
                Logs.e(TAG, "VIRenderer -> segment createHandle error =  " + status[0]);
                ThreadExecutor.runOnMainThread(() -> Toast.makeText(VIAPISdkApp.getContext(),
                        "segment createHandle failed status = " + status[0], Toast.LENGTH_LONG).show());
                return;
            }
            status[0] = mHumanSegment.nativeSegmentInit(modelsPath);
        }
        if (status[0] == 0) {
            mInitSegment.set(true);
            Logs.i(TAG, "VIRenderer -> initSegment ok!! ! ");
            ThreadExecutor.runOnMainThread(() -> {
                if (mIVIRendererUIStatus != null) {
                    mIVIRendererUIStatus.hideLoading();
                }
            });
        } else {
            mInitSegment.set(false);
            Logs.e(TAG, "VIRenderer -> segment initProcess error = " + status[0]);
            ThreadExecutor.runOnMainThread(() -> Toast.makeText(VIAPISdkApp.getContext(),
                    "segment initProcess failed status = " + status[0], Toast.LENGTH_LONG).show());
        }
    }

    public void segmentDestroy() {
        mInitSegment.set(false);
        sep = 0;
        mDstBuffer = null;
        synchronized (mInitLock) {
            mHumanSegment.nativeSegmentClear();
            mHumanSegment.nativeSegmentDestroy();
            Logs.i(TAG, "VIRenderer -> segmentDestroy ok!! ! ");
        }
    }

    public void releaseGL() {
        if (mSegmentTexId != OpenGLUtil.NO_TEXTURE) {
            OpenGLUtil.deleteTextures(new int[]{mSegmentTexId});
            mSegmentTexId = OpenGLUtil.NO_TEXTURE;
        }
    }

    public synchronized int processSegmentForBuffer(byte[] yuv420sp, int textureWidth, int textureHeight, int cameraFace, int angle) {
        if (!mInitSegment.get() || (blendImageBg == null)) {
            return -1;
        }

        if (mTexHeight != textureHeight || mTexWeight != textureWidth) {
            releaseGL();
            mTexHeight = textureHeight;
            mTexWeight = textureWidth;
            mDstBuffer = ByteBuffer.allocateDirect(textureWidth * textureHeight * 4);
        }
        if (mDstBuffer == null) {
            mDstBuffer = ByteBuffer.allocateDirect(textureWidth * textureHeight * 4);
        }
        mDstBuffer.clear();
        long algorithmTime = System.currentTimeMillis();
        synchronized (mInitLock) {
            mHumanSegment.nativeSegmentProcessBuffer(yuv420sp, textureWidth, textureHeight, angle, cameraFace, sep, mDstBuffer);
        }
        mAlgorithmTime = System.currentTimeMillis() - algorithmTime;
        if (sep == 0) {
            sep = 1;
        }
        long loadTextureTime = System.currentTimeMillis();
        takePicture(mDstBuffer, textureWidth, textureHeight);
        mSegmentTexId = OpenGLUtil.loadTexture(mDstBuffer, textureWidth, textureHeight, mSegmentTexId);
        mLoadTextureTime = System.currentTimeMillis() - loadTextureTime;
        return mSegmentTexId;
    }

    public Bitmap getBlendImageBgBitmap() {
        return blendImageBg;
    }

    /*
    获取算法时间
     */
    public long getAlgorithmTime() {
        return mAlgorithmTime;
    }

    /**
     * 获取加载到纹理的时间
     */
    public long getLoadTextureTime() {
        return mLoadTextureTime;
    }

    private void takePicture(ByteBuffer byteBuffer, final int texWidth, final int texHeight) {
        if (!mIsNeedTakePicture.get()) {
            return;
        }
        mIsNeedTakePicture.set(false);
        mTakePictureUtil.captureBySegmentOutput(byteBuffer, texWidth, texHeight, null);
    }

    public void takeSegmentPic() {
        if (mTakePictureUtil.getIsTakingPicture()) {
            return;
        }
        mIsNeedTakePicture.set(true);
        mTakePictureUtil.setStartTakePicture(true);
    }

    @SuppressLint("CheckResult")
    public void setSelectedVB(final BaseVBItem item, int angle) {
        Completable.complete().subscribeOn(Schedulers.single()).subscribe(() -> {
            Logs.i(TAG, "set BgImageName = " + item.getBgImageName());
            String name;
            if (item.getType() == VBCardType.USER_CUSTOM_PIC) {
                name = item.getBgImagePath();
            } else {
                name = item.getBgImageName();
            }
            if (!TextUtils.isEmpty(name)) {
                setSelectedVB(name, angle);
                VBConfRecord.saveUserSelectVbImage(mContext, TextUtils.isEmpty(item.getBgImagePath()) ? item.bgImageName : item.getBgImagePath());
            }
        });
    }

    @SuppressLint("CheckResult")
    public void setSelectedVB(final String selectedVBName, int angle) {
        boolean isChange = !selectedVBName.equals(mVBName);
        boolean isEmpty = TextUtils.isEmpty(selectedVBName);
        Completable.complete().subscribeOn(Schedulers.single()).subscribe(()
                -> VBConfRecord.saveUserSelectVbImage(mContext, selectedVBName));

        if (!isEmpty && isChange) {
            initSegment();
        }

        if (isEmpty || isChange) {
            mBgAngleMap.clear();
            mVBName = selectedVBName;
        }

        if (TextUtils.isEmpty(selectedVBName) || VBCardType.NONE.getCardName().equals(selectedVBName)) {
            blendImageBg = null;
        } else {
            Bitmap bitmap = createBlendImageFromName(selectedVBName);
            if (bitmap != null) {
                reSetVBImage(angle);
            } else {
                blendImageBg = null;
            }
        }
    }

    public void createSelectedVBImageFromConf() {
        String name = VBConfRecord.getUserSelectVbImage(mContext);
        if (TextUtils.isEmpty(mVBName)) {
            mVBName = name;
        }
        createBlendImageFromName(name);
    }

    private Bitmap createBlendImageFromName(String imageName) {
        blendImageBg = VBConfRecord.getBitmapFromName(mContext, imageName);
        return blendImageBg;
    }

    public void reSetVBImage(int angle) {
        if (null != blendImageBg) {
            Bitmap bitmap = mBgAngleMap.get(angle);
            if (bitmap == null) {
                if (angle == 90 || angle == 270) {
                    bitmap = BitmapUtils.scaleBitmap(blendImageBg, mTexHeight, mTexWeight);
                } else {
                    bitmap = BitmapUtils.scaleBitmap(blendImageBg, mTexWeight, mTexHeight);
                }
                bitmap = transformBlendImageBg(bitmap, angle);
                mBgAngleMap.put(angle, bitmap);
            }
            if (mCameraRenderer != null) {
                mCameraRenderer.setBlendImageBg(bitmap);
            }
        }
    }

    public Bitmap transformBlendImageBg(Bitmap bitmap, int angle) {
        return transformBlendImageBg(bitmap, angle, false);
    }

    public Bitmap transformBlendImageBg(Bitmap bitmap, int angle, boolean isHMirror) {
        if (isHMirror) {
            return BitmapUtils.mirrorRotateImage(bitmap, angle, false, false, false);
        } else {
            return BitmapUtils.mirrorRotateImage(bitmap, -angle, true, false, true);
        }
    }

    public void setIVIRendererUIStatus(IVIRendererUIStatus rendererUIStatus) {
        this.mIVIRendererUIStatus = rendererUIStatus;
    }
}
