package io.github.chengmboy.util;

public interface TaskTenantExecutor {

    void run(Task task);

    boolean tryRun(Task task);

    int getMaxThreads(long tenantId);

    TaskTenantThreadPoolExecutor.TaskProducerThread taskProducerThread();
}
