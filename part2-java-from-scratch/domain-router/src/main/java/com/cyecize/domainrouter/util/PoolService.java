package com.cyecize.domainrouter.util;

import com.cyecize.domainrouter.constants.General;
import com.cyecize.ioc.annotations.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class PoolService {

    private final ThreadPoolExecutor pool;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final AtomicInteger runningTasks = new AtomicInteger();

    public PoolService() {
        this.pool = this.getPool();
        this.initScheduledTasks();
    }

    public Future<?> submit(Runnable task) {
        return this.pool.submit(() -> {
            this.runningTasks.incrementAndGet();

            final Thread currentThread = Thread.currentThread();
            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(
                    () -> {
                        log.error("Stuck on: {}", currentThread.getName());
                    }, 5, 5, TimeUnit.MINUTES
            );

            task.run();

            scheduler.shutdownNow();
            this.runningTasks.decrementAndGet();
        });
    }

    private ThreadPoolExecutor getPool() {
        int poolSize = General.DEFAULT_THREAD_POOL_SIZE;

        if (System.getenv().containsKey(General.ENV_VAR_POOL_SIZE_NAME)) {
            poolSize = Integer.parseInt(System.getenv(General.ENV_VAR_POOL_SIZE_NAME).trim());
        }

        return new ThreadPoolExecutor(
                General.MIN_THREAD_POOL_SIZE,
                poolSize,
                1L,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>()
        );
    }

    private void initScheduledTasks() {
        this.scheduler.scheduleAtFixedRate(() -> {
            if (this.runningTasks.get() > 0) {
                log.info("Currently running {} tasks", this.runningTasks);
            }
        }, 0, 30, TimeUnit.MINUTES);
    }
}
