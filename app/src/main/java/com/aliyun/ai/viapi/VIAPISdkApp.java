package com.aliyun.ai.viapi;

import android.app.Application;
import android.text.TextUtils;
import android.widget.Toast;

import com.aliyun.ai.viapi.core.VIAPICreateApi;
import com.aliyun.ai.viapi.core.VIAPIStatusCode;
import com.aliyun.ai.viapi.util.Logs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        int status = VIAPICreateApi.getInstance().getVIAPISdkCore().init(this, BuildConfig.DEBUG);
        Logs.i(TAG, "init = " + status + ", license path = " + VIAPICreateApi.getInstance().getVIAPISdkCore().getLicensePath());
        if (status != 0) {
            Toast.makeText(this, VIAPIStatusCode.getErrorMsg(status), Toast.LENGTH_LONG).show();
        } else {
//            VIAPICreateApi.getInstance().getVIAPISdkCore().replaceLicenseFromAssets();
            status = VIAPICreateApi.getInstance().getVIAPISdkCore().initLicense();
            Logs.i(TAG, "initLicense = " + status);
            if (status == 0) {
                String sdkExpireTime = VIAPICreateApi.getInstance().getVIAPISdkCore().getLicenseExpireTime();
                if (!TextUtils.isEmpty(sdkExpireTime)) {
                    Logs.i(TAG, "到期日 = " + sdkExpireTime + ", 距离到期天数 = " + licenseExpireDays(sdkExpireTime));
                }
            }
        }
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
}