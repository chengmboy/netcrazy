package io.github.chengmboy.util;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
public abstract class Task implements Runnable,Delayed{

    protected long tenantId;

    protected long delayTime=0;

    protected String name = UUID.randomUUID().toString();

    private Task() {
    }
    public Task(long tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return delayTime==0?delayTime:
                unit.convert(delayTime-System.currentTimeMillis(),unit);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return ((Task) o).delayTime>delayTime?1:0;
    }
}
