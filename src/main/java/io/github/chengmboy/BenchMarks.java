package io.github.chengmboy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.openjdk.jmh.annotations.*;

@Measurement(iterations = 3, time = 3)
@Warmup(iterations = 3, time = 3)
@Fork(1)
@Threads(Threads.MAX)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BenchMarks {

    @Benchmark
    public void doBlockJobWithThread(BenchMarksState state) throws InterruptedException {
        int num = state.num;
        ThreadPoolExecutor executor = state.executor;
        CountDownLatch latch = new CountDownLatch(num);
        for (int i = 0; i < num; i++) {
            executor.execute(()->{
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                latch.countDown();
            });
        }
       latch.await();
    }

    @Benchmark
    public void doBlockJobWithLightThread(BenchMarksState state) throws InterruptedException {
        int num = state.num;
        CountDownLatch latch = new CountDownLatch(num);
        for (int i = 0; i < num; i++) {
            LockSupport.park();
            LightThread.INSTANCE.start(latch::countDown);
        }
        latch.await();
    }
}
