package io.github.chengmboy.util;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public abstract class CallableTask<V> extends Task implements Callable<V> {


    private FutureTask<V> futureTask;

    public CallableTask(long tenantId) {
        super(tenantId);
        this.futureTask = new FutureTask<>(this);
    }

    @Override
    public void run() {
        futureTask.run();
    }

    public FutureTask<V> getFutureTask() {
        return futureTask;
    }
}
