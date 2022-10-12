package com.cyecize.domainrouter.util;

import com.cyecize.domainrouter.constants.General;
import com.cyecize.ioc.annotations.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class PoolService {

    private final ExecutorService pool;

    public PoolService() {
        this.pool = this.getPool();
    }

    public Future<?> submit(Runnable task) {
        return this.pool.submit(task);
    }

    private ExecutorService getPool() {
        int poolSize = General.DEFAULT_THREAD_POOL_SIZE;

        if (System.getenv().containsKey(General.ENV_VAR_POOL_SIZE_NAME)) {
            poolSize = Integer.parseInt(System.getenv(General.ENV_VAR_POOL_SIZE_NAME).trim());
        }

        return Executors.newFixedThreadPool(poolSize);
    }
}
