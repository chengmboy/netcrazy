package io.github.chengmboy;

import io.github.chengmboy.util.Task;
import io.github.chengmboy.util.TaskQueue;
import io.github.chengmboy.util.TaskTenantExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.*;

@SpringBootApplication
@RestController
@Slf4j
public class NetCrazyApplication {

    @Autowired
    TaskQueue<Task> taskQueue;

    @Autowired
    TaskTenantExecutor taskTenantExecutor;

    ExecutorService poolExecutor = new ThreadPoolExecutor(10, 200,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(8182));

    int time2Block = 200;


    public static void main(String[] args) {
        SpringApplication.run(NetCrazyApplication.class, args);
    }


    @GetMapping
    public boolean offer(@RequestParam int tenantId) {
        Task task = new Task(tenantId) {
            @Override
            public void run() {
                try {
                    Thread.sleep(time2Block);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        return taskQueue.offer(task);
    }


    @GetMapping("block")
    public boolean block(@RequestParam int tenantId) {
        Task task = new Task(tenantId) {
            @Override
            public void run() {
                try {
                    Thread.sleep(time2Block);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        task.run();
        return true;
    }

    @GetMapping("pool")
    public boolean pool(@RequestParam int tenantId) {
        Task task = new Task(tenantId) {
            @Override
            public void run() {
                try {
                    Thread.sleep(time2Block);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        poolExecutor.execute(task);
        return true;
    }

    @GetMapping("sum")
    public String getSum() {
        new Thread(()->{
            while (true) {
                try {
                    Thread.sleep(1000);
                    System.out.println(taskTenantExecutor.toString() +" sum= "+ taskTenantExecutor.taskProducerThread().getSum().get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }).start();
        return taskTenantExecutor.toString() +" sum= "+ taskTenantExecutor.taskProducerThread().getSum().get();
    }
}
