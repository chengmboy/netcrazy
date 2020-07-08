package io.github.chengmboy.config;

import io.github.chengmboy.util.Task;
import io.github.chengmboy.util.TaskQueue;
import io.github.chengmboy.util.TaskTenantExecutor;
import io.github.chengmboy.util.TaskTenantThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskExecutorConfig {

    @Bean
    public TaskQueue<Task> taskQueue(TaskTenantExecutor taskTenantExecutor){
        return taskTenantExecutor.taskProducerThread().getTaskQueue();
    }

    @Bean
    public TaskTenantExecutor taskTenantExecutor() throws Exception {
        TaskTenantThreadPoolExecutor ex = new TaskTenantThreadPoolExecutor(200);
        ex.start();
        return ex;
    }
}
