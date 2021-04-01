package com.aliyun.ai.viapi.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ThreadExecutor {
    private static Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static ScheduledExecutorService sScheduledExecutorService;

    /**
     * 主线程执行任务
     *
     * @param runnable
     */
    public static void runOnMainThread(Runnable runnable) {
        if (null == runnable) {
            return;
        }
        if (Looper.getMainLooper() == Looper.myLooper()) {
            runnable.run();
        } else {
            sMainHandler.post(runnable);
        }
    }

    /**
     * 在指定线程执行任务
     *
     * @param handler  指定线程的Handler
     * @param runnable 具体任务
     */
    public static void runOnThread(Handler handler, Runnable runnable) {
        if (null == handler || null == runnable) {
            return;
        }

        if (Looper.myLooper() == handler.getLooper()) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    public static boolean runOnUiPostDelayed(Runnable runnable, long delay) {
        if (runnable != null) {
            if (Looper.getMainLooper() == Looper.myLooper()) {
                runnable.run();
                return true;
            } else {
                return sMainHandler.postDelayed(runnable, delay);
            }
        } else {
            return false;
        }
    }

    public static void removeUiAllTasks() {
        sMainHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 获取主线程handler
     *
     * @return
     */
    public static Handler getMainHandler() {
        return sMainHandler;
    }

    public static ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return getScheduler().schedule(command, delay, unit);
    }

    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return getScheduler().scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    private static synchronized ScheduledExecutorService getScheduler() {
        if (null == sScheduledExecutorService) {
            sScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "viapi-executor");
                }
            });
        }
        return sScheduledExecutorService;
    }
}
