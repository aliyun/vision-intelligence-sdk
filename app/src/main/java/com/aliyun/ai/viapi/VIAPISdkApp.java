package com.aliyun.ai.viapi;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.aliyun.ai.viapi.core.VIAPICreateApi;
import com.aliyun.ai.viapi.core.VIAPIStatusCode;
import com.aliyun.ai.viapi.util.AssetsProvider;
import com.aliyun.ai.viapi.util.AssetsUtils;
import com.aliyun.ai.viapi.util.Logs;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * @author: created by hanbing
 * @date: 2021/3/11
 * @Description:
 */
public class VIAPISdkApp extends Application {
    private static String TAG = "VIAPISdkApp";
    private static Application mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        initSDK();
    }

    public static Application getContext() {
        return mContext;
    }

    private void initSDK() {
        initModeData(this);
        int status = VIAPICreateApi.getInstance().getVIAPISdkCore().init(this, BuildConfig.DEBUG);
        if (status != 0) {
            Toast.makeText(this, VIAPIStatusCode.getErrorMsg(status), Toast.LENGTH_LONG).show();
        } else {
//            VIAPICreateApi.getInstance().getVIAPISdkCore().replaceLicenseFromAssets();
            status = VIAPICreateApi.getInstance().getVIAPISdkCore().initLicense();
            Logs.i(TAG, "initLicense = " + status);
            if (status == 0) {
                updateLicense();
            }
        }
    }

    public static void initModeData(Context context) {
        String childPath = AssetsProvider.getResourceRootPath();
        String rootPath = AssetsProvider.getModelsAbsolutePath();
        Single.fromCallable(() -> {
            try {
                AssetsUtils.copyAssetsFile(context.getAssets(), childPath, rootPath);
                Logs.i(TAG, "模型文件拷贝成功");
            } catch (IOException e) {
                Logs.e(TAG, "模型文件拷贝失败：" + e.toString());
                e.printStackTrace();
            }
            return 0;
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    private int licenseExpireDays(String sdkExpireTime) {
        int days = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date expireData;
        Date today = new Date();
        try {
            expireData = sdf.parse(sdkExpireTime);
            days = (int) ((expireData.getTime() - today.getTime()) / (1000 * 3600 * 24));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return days;
    }

    private void updateLicense() {
        String sdkExpireTime = VIAPICreateApi.getInstance().getVIAPISdkCore().getLicenseExpireTime();
        if (!TextUtils.isEmpty(sdkExpireTime)) {
            int expireDays = licenseExpireDays(sdkExpireTime);
            Logs.i(TAG, "到期日 = " + sdkExpireTime + ", 距离到期天数 = " + expireDays);
            if (expireDays < 30) {
                String licensePath = VIAPICreateApi.getInstance().getVIAPISdkCore().getLicensePath();
                String licenseFilePath = VIAPICreateApi.getInstance().getVIAPISdkCore().getLicenseFilePath();
                Logs.i(TAG, "licensePath = " + licensePath);
                Logs.i(TAG, "licenseFilePath = " + licenseFilePath);
                replaceLicense(licensePath, "新的license目录");
            }
        }
    }

    private void replaceLicense(String dstLicensePath, String newLicenseFile) {
        // TODO 用新的license替换旧的license文件
    }
}
