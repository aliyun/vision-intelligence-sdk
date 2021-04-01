/*
 * Copyright [2017] [zhi]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aliyun.ai.viapi.util;

import android.os.Environment;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;
import static android.util.Log.WARN;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class Logs {
    private static final String sLogTag = "viapi";
    private static final String LOG_PREFIX = sLogTag;
    private static final int LOG_PREFIX_LENGTH = sLogTag.length();
    private static final int MAX_LOG_TAG_LENGTH = 23;

    public static String makeLogTag(String str) {
        if (str.length() > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            return LOG_PREFIX + str.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1);
        }

        return LOG_PREFIX + str;
    }

    public static String getTag() {

        Throwable ex = new Throwable();
        StringBuilder mStringBuilder = new StringBuilder();
        StackTraceElement[] stackElements = ex.getStackTrace();
        String pre = "";
        if (stackElements != null) {
            for (int i = 0; i < stackElements.length; i++) {
                String methodName = stackElements[i].getMethodName();
                // 和当前函数名字保持一致，混淆时要keep这个methodName
                if ("getTag".equals(pre)) {
                    mStringBuilder.append("viapi-log (" + stackElements[i].getFileName() + ":")
                            .append(stackElements[i].getLineNumber() + ")")
                            .append(stackElements[i].getMethodName());
                    break;
                }
                pre = methodName;
            }
        }
        String log = mStringBuilder.toString();
        return log;
    }

    /**
     * 获取当前位置，即文件名和行号行号，用来打日志时放在快速定位
     * @return
     */
    public static String getCurrentPos() {

        Throwable ex = new Throwable();
        StringBuilder mStringBuilder = new StringBuilder();
        StackTraceElement[] stackElements = ex.getStackTrace();
        String pre = "";
        if (stackElements != null) {
            for (int i = 0; i < stackElements.length; i++) {
                String methodName = stackElements[i].getMethodName();
                // 和当前函数名字保持一致，混淆时要keep这个methodName
                if ("getCurrentPos".equals(pre)) {
                    mStringBuilder.append(" (" + stackElements[i].getFileName() + ":")
                            .append(stackElements[i].getLineNumber() + ")");

                    break;
                }
                pre = methodName;
            }
        }

        return mStringBuilder.toString();
    }

    /**
     * Used to enable/disable logging that we don't want included in
     * production releases.
     * This should be retain to DEBUG for production releases, and VERBOSE for
     * internal builds.
     */
    private static final int MAX_ENABLED_LOG_LEVEL = VERBOSE;

    /**
     * Checks to see whether or not a log for the specified tag is loggable at the specified level.
     */
    public static boolean isLoggable(String tag, int level) {
        return MAX_ENABLED_LOG_LEVEL <= level
                && (Log.isLoggable(tag, level) || Log.isLoggable(sLogTag, level));
    }

    /**
     * Send a {@link Log#VERBOSE} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int v(String tag, String msg) {
        if (isLoggable(tag, VERBOSE)) {
            return Log.v(tag, msg);
        }
        return 0;
    }

    /**
     * Send a {@link Log#VERBOSE} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     * @param msg The message you would like logged.
     */
    public static int v(String tag, Throwable tr, String msg) {
        if (isLoggable(tag, VERBOSE)) {
            return Log.v(tag, msg, tr);
        }
        return 0;
    }

    /**
     * Send a {@link Log#DEBUG} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int d(String tag, String msg) {
        if (isLoggable(tag, Log.DEBUG)) {
            return Log.d(tag, msg);
        }
        return 0;
    }

    /**
     * Send a {@link Log#DEBUG} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     * @param msg The message you would like logged.
     */
    public static int d(String tag, Throwable tr, String msg) {
        if (isLoggable(tag, Log.DEBUG)) {
            return Log.d(tag, msg, tr);
        }
        return 0;
    }

    /**
     * Send a {@link Log#INFO} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int i(String tag, String msg) {
        if (isLoggable(tag, INFO)) {
            return Log.i(tag, msg);
        }
        return 0;
    }

    /**
     * Send a {@link Log#INFO} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     * @param msg The message you would like logged.
     */
    public static int i(String tag, Throwable tr, String msg) {
        if (isLoggable(tag, INFO)) {
            return Log.i(tag, msg, tr);
        }
        return 0;
    }

    /**
     * Send a {@link Log#WARN} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int w(String tag, String msg) {
        if (isLoggable(tag, WARN)) {
            return Log.w(tag, msg);
        }
        return 0;
    }

    /**
     * Send a {@link Log#WARN} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     * @param msg The message you would like logged.
     */
    public static int w(String tag, Throwable tr, String msg) {
        if (isLoggable(tag, WARN)) {
            return Log.w(tag, msg, tr);
        }
        return 0;
    }

    /**
     * Send a {@link Log#ERROR} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int e(String tag, String msg) {
        if (isLoggable(tag, ERROR)) {
            return Log.e(tag, msg);
        }
        return 0;
    }

    /**
     * Send a {@link Log#ERROR} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     * @param msg The message you would like logged.
     */
    public static int e(String tag, Throwable tr, String msg) {
        if (isLoggable(tag, ERROR)) {
            return Log.e(tag, msg, tr);
        }
        return 0;
    }

    /**
     * What a Terrible Failure: Report a condition that should never happen.
     * The error will always be logged at level ASSERT with the call stack.
     * Depending on system configuration, a report may be added to the
     * {@link android.os.DropBoxManager} and/or the process may be terminated
     * immediately with an error dialog.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int wtf(String tag, String msg) {
        return Log.wtf(tag, msg, new Error());
    }

    /**
     * What a Terrible Failure: Report a condition that should never happen.
     * The error will always be logged at level ASSERT with the call stack.
     * Depending on system configuration, a report may be added to the
     * {@link android.os.DropBoxManager} and/or the process may be terminated
     * immediately with an error dialog.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     * the class or activity where the log call occurs.
     * @param tr An exception to log
     * @param msg The message you would like logged.
     */
    public static int wtf(String tag, Throwable tr, String msg) {
        return Log.wtf(tag, msg, tr);
    }

    public static void write(String tag, String msg) {
        LogThread.sInstance.write(tag, msg);
    }

    static class LogThread extends Thread {
        private static LogThread sInstance = new LogThread();
        private static String LOG_FILE = "duer-log-tv.log";
        private final BlockingQueue<String> mQueue;

        private LogThread() {
            mQueue = new LinkedBlockingQueue<>();
            start();
        }

        void write(String tag, String msg) {
            mQueue.add(System.currentTimeMillis() + "/" + tag + "/" + msg);
        }

        @Override
        public void run() {
            final String sdPath = getSDPath();
            if (!TextUtils.isEmpty(sdPath)) {
                final File file = new File(sdPath, LOG_FILE);
                final FileWriter writer;
                try {
                    writer = new FileWriter(file);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.wtf("LogThread", e.getMessage());
                    return;
                }
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                while (true) {
                    String msg;
                    try {
                        msg = mQueue.take();
                    } catch (InterruptedException e) {
                        break;
                    }
                    if (msg != null) {
                        try {
                            writer.append(msg).append('\n').flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    writer.close();
                } catch (IOException ignored) {
                    // LEFT-DO-NOTHING
                }
            } else {
                e(sLogTag, "could not get sd card path");
            }
        }
    }

    public static String getSDPath() {
        String sdDir = "/sdcard/";
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory().toString();
        } else {
            Logs.e("logs", "sdCard is not exit");
        }
        return sdDir;
    }
}
