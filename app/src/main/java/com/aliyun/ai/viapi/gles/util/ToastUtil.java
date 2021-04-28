package com.aliyun.ai.viapi.gles.util;

import android.content.Context;
import android.widget.Toast;

/**
 * @author: created by chehongpeng
 * @date: 2021/4/27
 * @Description:
 */
public class ToastUtil {

    private static Toast toast;

    public static void showToast(Context context, String msg) {
        if (toast == null) {
            toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        } else {
            toast.setText(msg);
        }
        toast.show();
    }

}
