package com.aliyun.ai.viapi.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.aliyun.ai.viapi.R;
import com.aliyun.ai.viapi.device.DeviceRotation;
import com.aliyun.ai.viapi.gles.util.OpenGLUtil;
import com.aliyun.ai.viapi.renderer.IRendererTimeListener;
import com.aliyun.ai.viapi.renderer.IVIRendererUIStatus;
import com.aliyun.ai.viapi.renderer.segment.Camera2Renderer;
import com.aliyun.ai.viapi.renderer.segment.VideoRenderer;
import com.aliyun.ai.viapi.renderer.segment.BaseRenderer;
import com.aliyun.ai.viapi.renderer.segment.Camera1Renderer;
import com.aliyun.ai.viapi.renderer.IRendererListener;
import com.aliyun.ai.viapi.ui.model.BaseVBItem;
import com.aliyun.ai.viapi.ui.model.VBCardType;
import com.aliyun.ai.viapi.ui.model.VBUserCustomCardModel;
import com.aliyun.ai.viapi.ui.model.VBViewModel;
import com.aliyun.ai.viapi.ui.vb.VBConst;
import com.aliyun.ai.viapi.ui.widget.GLImageView;
import com.aliyun.ai.viapi.ui.widget.VBPicturePickView;
import com.aliyun.ai.viapi.util.Camera2Helper;
import com.aliyun.ai.viapi.util.CameraHelper;
import com.aliyun.ai.viapi.util.FileUtil;
import com.aliyun.ai.viapi.util.Logs;
import com.aliyun.ai.viapi.util.PictureUtil;
import com.aliyun.ai.viapi.util.ScreenUtils;
import com.aliyun.ai.viapi.util.TakePictureUtil;
import com.aliyun.ai.viapi.util.ThreadExecutor;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * @author: created by hanbing
 * @date: 2021/3/9
 * @Description:
 */
public class HumanSegmentFragment extends Fragment implements IRendererListener, SensorEventListener,
        IRendererTimeListener, VBPicturePickView.OnCardItemClickListener, View.OnClickListener, IVIRendererUIStatus {
    private static final String TAG = HumanSegmentFragment.class.getSimpleName();
    public static final int PICK_PHOTO_REQUEST = 1000;
    private volatile AtomicBoolean mIsNeedTakePicture = new AtomicBoolean(false);
    private OnFragmentCallback mFragmentCallback;
    private BaseRenderer mBaseRenderer;
    private SensorManager mSensorManager;
    private VideoRenderer mVideoRenderer;
    private TextView mBaseFpsText;
    private LinearLayout mLoadingContainer;
    private TakePictureUtil mTakePictureUtil;
    private TakePictureUtil.TakePictureMode mTakePictureMode;
    private VBPicturePickView mVBPicturePickView;
    private VBViewModel mViewModel;
    private int mCameraFace = CameraHelper.FACE_FRONT;
    private int mCameraOrientation;
    private int mDeviceRotation = 0;
    private int mTextureWidth;
    private int mTextureHeight;
    private int mVideoAngle;
    private byte[] mYuv420sp;
    //--------------------------------------FPS（FPS相关定义）----------------------------------------
    private static final int NANO_IN_ONE_MILLI_SECOND = 1_000_000;
    private long mTexIdBeginTime, mTexIdEndTime, mOnScreenBeginTime, mOnScreenEndTime, mTotalRenderTime;
    private long mAlgorithmTime;
    private long mLoadTextureTime;


    private void initView(View view) {
        GLImageView glSurfaceView = view.findViewById(R.id.gl_surface);
        mBaseFpsText = view.findViewById(R.id.fu_base_fps_text);
        mVBPicturePickView = view.findViewById(R.id.background_list_container);
        mVBPicturePickView.setItemClickListener(this);
        glSurfaceView.setEGLContextClientVersion(OpenGLUtil.getSupportGLVersion(getContext()));
        // 手机系统大于等于5.0并且手机支持camera2
        if (Build.VERSION.SDK_INT >= 21 && Camera2Helper.hasCamera2(getContext())) {
            mBaseRenderer = new Camera2Renderer(getActivity(), glSurfaceView, this);
        } else {
            mBaseRenderer = new Camera1Renderer(getActivity(), glSurfaceView, this);
        }
        mBaseRenderer.setMarkFPSListener(this);
        mCameraFace = mBaseRenderer.getCameraFace();
        glSurfaceView.setViewRenderer(mBaseRenderer);
        view.findViewById(R.id.icon_show_debug).setOnClickListener(this);
        view.findViewById(R.id.icon_switch_camera).setOnClickListener(this);
        view.findViewById(R.id.take_blend_picture).setOnClickListener(this);
        view.findViewById(R.id.take_seg_in_pic).setOnClickListener(this);
        view.findViewById(R.id.take_seg_out_pic).setOnClickListener(this);
        view.findViewById(R.id.icon_select_vb).setOnClickListener(this);
        view.findViewById(R.id.icon_back).setOnClickListener(this);
        mLoadingContainer = view.findViewById(R.id.loading);
        mVideoRenderer = new VideoRenderer(getContext(), mBaseRenderer);
        mVideoRenderer.createSelectedVBImageFromConf();
        mVideoRenderer.setIVIRendererUIStatus(this);
    }

    private void setVBPicturePickViewVisibility(int visibility) {
        if (mVBPicturePickView == null) {
            return;
        }
        Animation animation = AnimationUtils.loadAnimation(getContext(), visibility == View.VISIBLE ? R.anim.anim_pop_enter : R.anim.anim_pop_exit);
        mVBPicturePickView.setAnimation(animation);
        mVBPicturePickView.setVisibility(visibility);
    }

    private void initViewModel() {
        mViewModel = new ViewModelProvider
                .AndroidViewModelFactory(getActivity().getApplication())
                .create(VBViewModel.class);
    }

    private void initViewModelObserver() {
        mViewModel.getBaseCardItemLiveData().observe(getViewLifecycleOwner(),
                baseVBItems -> mVBPicturePickView.setVBLists(baseVBItems));
        mViewModel.loadCardItems(getContext());
    }

    public HumanSegmentFragment() {
    }

    public static HumanSegmentFragment newInstance(String param1, String param2) {
        HumanSegmentFragment fragment = new HumanSegmentFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        ScreenUtils.setFullScreen(getActivity());
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mTakePictureUtil = new TakePictureUtil();
    }

    @Override
    public void onPause() {
        super.onPause();
        mBaseRenderer.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mBaseRenderer.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mVideoRenderer != null) {
            mVideoRenderer.segmentDestroy();
        }
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() instanceof OnFragmentCallback) {
            mFragmentCallback = (OnFragmentCallback) getActivity();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_human_segment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        initViewModel();
        initViewModelObserver();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            if (Math.abs(x) > 3 || Math.abs(y) > 3) {
                if (Math.abs(x) > Math.abs(y)) {
                    mDeviceRotation = x > 0 ? 0 : 180;
                } else {
                    mDeviceRotation = y > 0 ? 90 : 270;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onFpsChange(double fps) {
        mBaseFpsText.post(() -> mBaseFpsText.setText(String.format(getString(R.string.base_fps), mTextureWidth,
                mTextureHeight, (int) fps, (int) mTotalRenderTime, (int) mAlgorithmTime, (int) mLoadTextureTime)));
    }

    @Override
    public void onCompose2dTexIdBeginTime() {
        mTexIdBeginTime = System.nanoTime();
    }

    @Override
    public void onCompose2dTexIdEndTime() {
        mTexIdEndTime = System.nanoTime();
    }

    @Override
    public void onFrameOnScreenBeginTime() {
        mOnScreenBeginTime = System.nanoTime();
    }

    @Override
    public void onFrameOnScreenEndTime() {
        mOnScreenEndTime = System.nanoTime();
    }

    @Override
    public void onTotalRenderTime() {
        mTotalRenderTime = ((mTexIdEndTime - mTexIdBeginTime) + (mOnScreenEndTime - mOnScreenBeginTime)) / NANO_IN_ONE_MILLI_SECOND;
    }

    @Override
    public void onSurfaceCreated() {
        mVideoRenderer.initSegment();
    }

    @Override
    public void onSurfaceChanged(int viewWidth, int viewHeight) {

    }

    @Override
    public int onDrawFrame(byte[] yuv420sp, int cameraTexId, int textureWidth, int textureHeight, float[] mvpMatrix, float[] texMatrix, long timeStamp) {
        mTextureWidth = textureWidth;
        mTextureHeight = textureHeight;
        mYuv420sp = yuv420sp;
        int retTexId;
        if (null != mVideoRenderer.getBlendImageBgBitmap()) {
            retTexId = mVideoRenderer.processSegmentForBuffer(yuv420sp, textureWidth, textureHeight, mCameraFace, getRotationAngle());
            //算法时间
            mAlgorithmTime = mVideoRenderer.getAlgorithmTime();
            //加载纹理的时间
            mLoadTextureTime = mVideoRenderer.getLoadTextureTime();
        } else {
            retTexId = cameraTexId;
        }
        takePicture(retTexId, retTexId == cameraTexId, mvpMatrix, texMatrix, textureWidth, textureHeight);
        return retTexId > 0 ? retTexId : cameraTexId;
    }

    @Override
    public void onSurfaceDestroy() {
        mVideoRenderer.releaseGL();
    }

    @Override
    public void onSwitchCamera(int cameraFace, int cameraOrientation) {
        this.mCameraOrientation = cameraOrientation;
        if (this.mCameraFace != cameraFace) {
            this.mCameraFace = cameraFace;
        }
        mVideoRenderer.reSetVBImage(getRotationAngle(false));
    }

    public void takePicOnClick(TakePictureUtil.TakePictureMode mode) {
        if (mTakePictureUtil.getIsTakingPicture()) {
            return;
        }
        mTakePictureMode = mode;
        mIsNeedTakePicture.set(true);
        mTakePictureUtil.setStartTakePicture(true);
    }

    public void takeSegmentPicOnClick(TakePictureUtil.TakePictureMode mode) {
        mVideoRenderer.takeSegmentPic();
    }

    private void takePicture(int texId, boolean isOES, float[] mvpMatrix, float[] texMatrix, final int texWidth, final int texHeight) {
        if (!mIsNeedTakePicture.get()) {
            return;
        }
        mIsNeedTakePicture.set(false);
        if (mTakePictureMode == TakePictureUtil.TakePictureMode.JAVA_BLEND_MODE) {
            mTakePictureUtil.takePicture(texId, isOES, OpenGLUtil.IDENTITY_MATRIX, texMatrix, texHeight, texWidth, mVideoRenderer.getBlendImageBgBitmap());
        } else if (mTakePictureMode == TakePictureUtil.TakePictureMode.CAMERA_INPUT_SEG_MODE) {
            mTakePictureUtil.captureByCameraOutput(mYuv420sp, mTextureWidth, mTextureHeight, null);
        }
    }

    private int getRotationAngle(boolean isReset) {
        int rotationAngle = DeviceRotation.ROTATION_0.getAngle();
        if (mCameraOrientation == 270) {
            int angleIndex = mDeviceRotation / 90;
            Integer angle = DeviceRotation.getAngle(angleIndex);
            if (angle != null) {
                rotationAngle = angle;
            }
        } else if (mCameraOrientation == 90) {
            if (mDeviceRotation == 90) {
                rotationAngle = DeviceRotation.ROTATION_270.getAngle();
            } else if (mDeviceRotation == 270) {
                rotationAngle = DeviceRotation.ROTATION_90.getAngle();
            } else {
                int angleIndex = mDeviceRotation / 90;
                Integer angle = DeviceRotation.getAngle(angleIndex);
                if (angle != null) {
                    rotationAngle = angle;
                }
            }
        }

        if (mVideoAngle != rotationAngle && isReset) {
            mVideoAngle = rotationAngle;
            mVideoRenderer.reSetVBImage(rotationAngle);
        }
        return rotationAngle;
    }

    private int getRotationAngle() {
        return getRotationAngle(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (PICK_PHOTO_REQUEST == requestCode) {
                Uri uri = data.getData();
                final String imagePath = PictureUtil.getPathByUri(getActivity(), uri);
                if (TextUtils.isEmpty(imagePath)) {
                    return;
                }
                saveUserSelectPicture(imagePath);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean isMaxAddLimit() {
        int userSelectPicCount = mVBPicturePickView.getUserSelectCardCount();
        return userSelectPicCount >= VBConst.Const.USER_ADD_VB_PICTURE_MAX;
    }

    @SuppressLint("CheckResult")
    void deleteUserCardPicture(final VBUserCustomCardModel item) {
        Single.fromCallable(() -> {
            if (!TextUtils.isEmpty(item.bgImagePath)) {
                File imagePath = new File(item.bgImagePath);
                Logs.i(TAG, "delete BgImageName = " + item.bgImagePath
                        + ", getAbsolutePath() = " + imagePath.getAbsolutePath());
                if (imagePath.exists()) {
                    return imagePath.delete();
                }
            }
            return false;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(deleteSuccess ->
                                Toast.makeText(getContext(), getString(R.string.common_delete),
                                        Toast.LENGTH_SHORT).show(),
                        err -> Logs.e(TAG, "delete BgImageName e : " + err.toString()));
    }

    @SuppressLint("CheckResult")
    private void saveUserSelectPicture(final String imagePath) {
        if (!PictureUtil.IsSupportImageFile(imagePath)) {
            Toast.makeText(getContext(), R.string.vb_card_add_pic_format_no_support_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap srcBitmap = BitmapFactory.decodeFile(imagePath);
        if (srcBitmap == null) {
            Toast.makeText(getContext(), R.string.vb_load_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        Logs.i(TAG, "set BgImageName w " + srcBitmap.getWidth() + ", h = " + srcBitmap.getHeight());
        if (srcBitmap.getWidth() > VBConst.Const.BITMAP_WIDTH_MAX || srcBitmap.getHeight() > VBConst.Const.BITMAP_HEIGHT_MAX) {
            showTooBigPicPrompt();
            return;
        }
        if (srcBitmap.getWidth() < VBConst.Const.BITMAP_WIDTH_MIN && srcBitmap.getHeight() < VBConst.Const.BITMAP_HEIGHT_MIN) {
            showTooSmallPicPrompt();
        }
        Single.fromCallable(() -> {
            String extName = FileUtil.getFileExtension(imagePath);
            Logs.i(TAG, "set BgImageName extName = " + extName + ", imagePath = " + imagePath);
            String savePath = PictureUtil.getVBImageSavePath(getContext()) + PictureUtil.makePicFileName(extName);
            Logs.d(TAG, "set BgImageName savePath = " + savePath);
            if (FileUtil.copyFile(new File(imagePath), new File(savePath))) {
                return savePath;
            }
            return "";
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(filePath -> {
                    if (!TextUtils.isEmpty(filePath)) {
                        BaseVBItem userCardItem = mViewModel.createUserCardItem(filePath);
                        if (mVBPicturePickView.getUserSelectCardCount() == 0) {
                            mVBPicturePickView.addUserCard(mViewModel.createDivideLineCardItem(), false);
                        }
                        mVBPicturePickView.addUserCard(userCardItem);
                        mVideoRenderer.setSelectedVB(userCardItem, getRotationAngle(false));
                    }
                }, err -> Logs.e(TAG, "add BgImageName e : " + err.toString()));
    }

    private void showAddLimitPicPrompt() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setMessage(getContext().getString(R.string.vb_support_localresource_max_count))
                .setPositiveButton(getContext().getString(R.string.common_i_know), (dialog, which) -> dialog.dismiss());
        dialogBuilder.show();
    }

    private void showTooSmallPicPrompt() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setMessage(getContext().getString(R.string.vb_imgpix_lessthan_min))
                .setPositiveButton(getContext().getString(R.string.common_i_know),
                        (dialog, which) -> dialog.dismiss());
        dialogBuilder.show();
    }

    private void showTooBigPicPrompt() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setMessage(getContext().getString(R.string.vb_imgpix_outof_max))
                .setPositiveButton(getContext().getString(R.string.vb_rechoice), (dialog, which) -> {
                    dialog.dismiss();
                    openAlbumPicker();
                })
                .setNegativeButton(getContext().getString(R.string.common_cancel), (dialog, i) -> dialog.dismiss());
        dialogBuilder.show();
    }

    private void showCardRemovePrompt(final int pos, final VBUserCustomCardModel item) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setMessage(getContext().getString(R.string.vb_delete_question))
                .setPositiveButton(getContext().getString(R.string.vb_delete), (dialog, which) -> {
                    dialog.dismiss();
                    Logs.d(TAG, " delete card path = " + item.bgImagePath);
                    mVBPicturePickView.removeUserCard(pos);
                    if (mVBPicturePickView.getUserSelectCardCount() == 0) {
                        mVBPicturePickView.removeDivide();
                    }
                    deleteUserCardPicture(item);
                })
                .setNegativeButton(getContext().getString(R.string.common_cancel), (dialog, i) -> dialog.dismiss());
        dialogBuilder.show();
    }

    @Override
    public void onItemClick(BaseVBItem item) {
        if (item.getType() == VBCardType.NONE
                || item.getType() == VBCardType.INNER_PIC
                || item.getType() == VBCardType.USER_CUSTOM_PIC) {
            mVBPicturePickView.updateSelectedBg(item.getBgImageName());
            mVideoRenderer.setSelectedVB(item, getRotationAngle(false));
        } else if (item.getType() == VBCardType.ADD_MENU) {
            if (!isMaxAddLimit()) {
                openAlbumPicker();
            } else {
                showAddLimitPicPrompt();
            }
        }
    }

    private void openAlbumPicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_PHOTO_REQUEST);
    }

    @Override
    public void onRemove(int pos, VBUserCustomCardModel item) {
        showCardRemovePrompt(pos, item);
    }

    @Override
    public void onSetComplete() {
        setVBPicturePickViewVisibility(View.GONE);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.icon_back:
                FragmentManager fm = getActivity().getSupportFragmentManager();
                fm.popBackStack();
                break;
            case R.id.icon_show_debug:
                int visibility = mBaseFpsText.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
                mBaseRenderer.setNeedMarkTimeCost(visibility == View.VISIBLE);
                ThreadExecutor.runOnUiPostDelayed(() -> mBaseFpsText.setVisibility(visibility), 500);
                break;
            case R.id.icon_select_vb:
                setVBPicturePickViewVisibility(mVBPicturePickView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                break;
            case R.id.icon_switch_camera:
                mBaseRenderer.switchCamera();
                break;
            case R.id.take_blend_picture:
                takePicOnClick(TakePictureUtil.TakePictureMode.JAVA_BLEND_MODE);
                break;
            case R.id.take_seg_in_pic:
                takePicOnClick(TakePictureUtil.TakePictureMode.CAMERA_INPUT_SEG_MODE);
                break;

            case R.id.take_seg_out_pic:
                mVideoRenderer.takeSegmentPic();
                break;
        }
    }

    @Override
    public void showLoading() {
        mLoadingContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        mLoadingContainer.setVisibility(View.GONE);
    }
}