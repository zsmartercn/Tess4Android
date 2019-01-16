/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.zsmarter.ocr.camera;

import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraThreadPool {

    private static Timer timerFocus = null;

    private static final long cameraScanInterval = 2000;

    private static int poolCount = Runtime.getRuntime().availableProcessors();

    private static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(poolCount);

    /**
     * 给线程池添加任务
     * @param runnable 任务
     */
    public static void execute(Runnable runnable) {
        fixedThreadPool.execute(runnable);
    }

}
