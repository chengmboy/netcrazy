package io.github.chengmboy.util;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@Measurement(iterations = 3, time = 3)
@Warmup(iterations = 3, time = 3)
@Fork(1)
@Threads(Threads.MAX)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class SimpleBenchMarks {

    @Benchmark
    public void simpleTenant(SimpleBenchMarksState state) {
        TaskTenantThreadPoolExecutor.TaskProducerThread.TaskQueueImpl taskQueue = state.getTaskQueue();
        taskQueue.offer(state.getTaskA());
    }
}
