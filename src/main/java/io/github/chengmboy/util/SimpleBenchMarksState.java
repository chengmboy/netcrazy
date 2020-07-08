package io.github.chengmboy.util;

import lombok.SneakyThrows;
import org.openjdk.jmh.annotations.*;


@State(Scope.Benchmark)
public class SimpleBenchMarksState {
    private TaskTenantThreadPoolExecutor executor ;

    private Task taskA;

    @Setup
    public final void before() throws Exception {
        executor = new TaskTenantThreadPoolExecutor(200);
        executor.start();

        class MyRunnableTask extends Task {
            public MyRunnableTask(long tenantId) {
                super(tenantId);
            }
            @SneakyThrows
            @Override
            public void run() {
            }
        }

        taskA = new MyRunnableTask(1);

    }

    @TearDown
    public final void after() throws Exception {
        if (executor != null) {
            executor.stop();
        }
    }

    public TaskTenantThreadPoolExecutor getExecutor() {
        return executor;
    }

    public TaskTenantThreadPoolExecutor.TaskProducerThread.TaskQueueImpl getTaskQueue(){
        return executor.taskProducerThread().getTaskQueue();
    }


    public Task getTaskA() {
        return taskA;
    }

}
