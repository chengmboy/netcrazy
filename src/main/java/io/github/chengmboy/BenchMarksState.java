package io.github.chengmboy;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class BenchMarksState {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(200,200,
            60, TimeUnit.SECONDS,new LinkedBlockingQueue<>(Integer.MAX_VALUE));

    int num = 1_000;

}
