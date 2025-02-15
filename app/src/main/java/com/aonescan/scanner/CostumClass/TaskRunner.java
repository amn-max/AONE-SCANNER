package com.aonescan.scanner.CostumClass;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaskRunner {
    private static final Executor executor = new ThreadPoolExecutor(5, 128, 1, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
    private final Handler handler = new Handler(Looper.getMainLooper());

    public <R> void executeAsync(Callable<R> callable, Callback<R> callback) {
        executor.execute(() -> {
            final R result;
            try {
                result = callable.call();
                handler.post(() -> {
                    callback.onComplete(result);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }

    public interface Callback<R> {
        void onComplete(R result);
    }
}
