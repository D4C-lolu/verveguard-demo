package com.interswitch.verveguarddemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.Executor;

@TestConfiguration
public class SyncAsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        return Runnable::run; // executes @Async calls on the calling thread
    }
}