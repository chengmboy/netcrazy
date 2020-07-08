package io.github.chengmboy.util;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 *
 * 线程的控制
 * */

@Slf4j
public class TaskTenantThreadPoolExecutor extends QueuedThreadPool implements TaskTenantExecutor {


    private short resCount = 2000;

    //资源占用map
    private ConcurrentHashMap<Short, AtomicInteger> tenantThreads = new ConcurrentHashMap<>(resCount);

    private int tenantMaxThread = 10;

    private TaskProducerThread taskProducerThread = new TaskProducerThread(this, tenantMaxThread);

    private BlockingQueue<Ptask> ptaskCaches = new ArrayBlockingQueue<>(500);

    private Task end = new Task(-2) {
        @Override
        public void run() {
        }
    };

    public TaskTenantThreadPoolExecutor(int maxThreads) {
        super(maxThreads);
        super.setName("t3pe" + super.hashCode());
    }

    @Override
    public void run(Task task) {
        Ptask ptask = ptaskCaches.poll();
        if (ptask == null) {
            ptask = new Ptask(task);
        } else {
            ptask.setTask(task);
        }
        super.execute(ptask);
    }


    // tenantThreads key 复用
    private short keysFor(Object o) {
//        log.debug("key[{}],hash[{}]",o,o.hashCode());
        return (short) (o.hashCode() % resCount);
    }

    @Data
    class Ptask implements Runnable {

        Task task;

        public Ptask(Task task) {
            this.task = task;
        }

        @Override
        public void run() {
            if (task.getTenantId() == -1) {
                task.run();
                return;
            }
            short key = keysFor(task.getTenantId());
            AtomicInteger threads = tenantThreads.get(key);
            if (threads == null) {
                threads = new AtomicInteger();
                tenantThreads.put(key, threads);
            }

            threads.incrementAndGet();
            log.debug("before run [{}] tenant[{}],key[{}],threads[{}]", task.getName(), task.getTenantId(), key, threads.get());
            task.run();
            log.debug("after run [{}] tenant[{}],key[{}],threads[{}]", task.getName(), task.getTenantId(), key, threads.get());
            threads.decrementAndGet();
            clear();
            ptaskCaches.offer(this);
        }

        public void clear() {
            task = null;
        }
    }


    @Override
    public boolean tryRun(Task task) {
        //由于线程池的线程keepalive所以很容易进行租户校验
        if (super.getThreads() > super.getMaxThreads() / 10) {
            log.debug("busy now _threadsStarted[{}],_maxThreads[{}]", super.getThreads(), super.getMaxThreads());
            return false;
        }
        log.debug("free do it");
        run(task);
        return true;
    }

    @Override
    public int getMaxThreads(long tenantId) {
        short key = keysFor(tenantId);
        AtomicInteger atomicInteger = tenantThreads.get(key);
        if (atomicInteger == null) {
            atomicInteger = new AtomicInteger();
            tenantThreads.put(key, atomicInteger);
        }
        return atomicInteger.get();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        taskProducerThread.start();
    }

    @Override
    protected void doStop() throws Exception {
        taskProducerThread.getTaskQueue().offer(end);
        super.doStop();
    }

    public TaskProducerThread taskProducerThread() {
        return taskProducerThread;
    }


    public class TaskProducerThread extends Task {

        private TaskQueueImpl taskQueue;

        private TaskTenantExecutor executor;

        private int tenantMaxThread = 10;

        private int nextTime = 3 * 1000;

        private AtomicInteger sum = new AtomicInteger();

        public TaskProducerThread(TaskTenantExecutor executor, int tenantMaxThread) {
            super(-1);
            this.taskQueue = new TaskQueueImpl();
            this.executor = executor;
            this.tenantMaxThread = tenantMaxThread;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    log.debug("start work");
                    Task task = taskQueue.take();
                    if (task == end) {
                        log.debug("TaskProducerThread shutdown");
                        break;
                    }
                    if (task.getTenantId() <= 0) {
                        log.error("illegal res key");
                    }

                    if (executor.tryRun(this)) {
                        //消费者线程
                        log.debug("eat what your kill");
                        Ptask t = ptaskCaches.poll();
                        if (t == null) {
                            t = new Ptask(task);
                        } else {
                            t.setTask(task);
                        }
                        t.run();
                        sum.incrementAndGet();
                        break;
                    }

                    /*
                     * 目前为生产者线程，必须尽快进入下一轮生产，为保证taskQueue的消费速度
                     * */
                    if (!executor.tryRun(task)) {
                        log.debug("pool do it");
                        /*
                         * 1. 租户未达到tenantMaxThread 允许执行executor.run
                         * 2. 租户达到tenantMaxThread task重新入队列等待下次执行
                         * */
                        long tenantId = task.getTenantId();

                        int activeThreads = executor.getMaxThreads(tenantId);
                        log.debug("tenant[{}],activeThreads[{}],maxThreads[{}]", task.getTenantId(), activeThreads, tenantMaxThread);
                        /*
                         * 这里是非原子的判断，所以不是准确的控制租户线程，这是为了可用性
                         * */
                        if (activeThreads < tenantMaxThread) {
                            log.debug("tenant[{}] ok do it", task.getTenantId());
                            executor.run(task);
                            sum.incrementAndGet();
                        } else {
                            log.debug("queue task [{}] ", task);
                            taskQueue.offer(task);
                        }
                    } else {
                        sum.incrementAndGet();
                    }

                } catch (InterruptedException ignored) {
                }
            }
        }

        public TaskQueueImpl getTaskQueue() {
            return taskQueue;
        }


        public class TaskQueueImpl extends DelayQueue<Task> implements TaskQueue<Task> {

            @Override
            public boolean offer(Task task) {
                // 检查租户是否还有剩余线程
                long tenantId = task.getTenantId();
                /*
                * 这里是非原子的判断，所以不是准确的控制租户线程，这是为了可用性
                * */
                int activeThreads = executor.getMaxThreads(tenantId);
                if (activeThreads >= tenantMaxThread) {
                    log.debug("res task over flow,set task[{}],delay[{}ms]", task, nextTime);
                    task.setDelayTime(System.currentTimeMillis() + nextTime);
                }
                log.debug("offer task tenant[{}],activeThreads[{}],maxThreads[{}]", task.getTenantId(), activeThreads, tenantMaxThread);
                return super.offer(task);
            }

            public void overflow(Task task) throws RejectedExecutionException {
                log.debug("over flow" + task);
            }
        }

        public void start() {
            executor.run(this);
        }

        public AtomicInteger getSum() {
            return sum;
        }

        @Override
        public String toString() {
            return "TaskProducerThread{" +
                    "taskQueue=" + taskQueue +
                    '}';
        }
    }


    public static void main(String[] args) throws Exception {
        TaskTenantThreadPoolExecutor ex = new TaskTenantThreadPoolExecutor(200);
        ex.start();
        TaskProducerThread.TaskQueueImpl taskQueue = ex.taskProducerThread().getTaskQueue();

/*
        class MyCallAbleTask extends CallableTask<Integer> {
            public MyCallAbleTask(long tenantId) {
                super(tenantId);
            }

            @Override
            public Integer call() throws Exception {
                log.debug(Thread.currentThread().getName() + " call");
                return Integer.MAX_VALUE;
            }
        }
        ;

        CallableTask<Integer> callableTask = new MyCallAbleTask(1);
        taskQueue.offer(callableTask);*/

        int len = 20000;
        byte[] bytes = new byte[len];
        System.out.println("task start " + bytes[0]);
        class MyRunnableTask extends Task {

            private int v;

            public MyRunnableTask(long tenantId, int v) {
                super(tenantId);
                this.v = v;
            }

            @SneakyThrows
            @Override
            public void run() {
                if (v != 19999) {
                    bytes[v] = 1;
                }

            }
        }


        long start = System.currentTimeMillis();
        for (int i = 0; i < len; i++) {
            Task task = new MyRunnableTask(1, i);
            taskQueue.offer(task);
        }
        while (true) {
            Thread.sleep(1000);
            AtomicInteger sum = ex.taskProducerThread().getSum();
            System.out.println("sum =" + sum);
            if (sum.get() == len) {
                System.out.println(System.currentTimeMillis() - start);
                break;
            }
        }

        boolean f = false;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != 1) {
                System.out.println("task failed " + bytes[i] + " i =" + i);
                f = true;
            }
        }
        if (!f) {
            System.out.println("task ok " + bytes[0]);
        }

        System.out.println(ex);
        ex.stop();


    }

    @Override
    public String toString() {
        return super.toString() + tenantThreads.toString() + " queueSize: " + taskProducerThread().getTaskQueue().size();
    }

}
