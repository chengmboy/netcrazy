package io.github.chengmboy.util;

public interface TaskQueue<E> {
    boolean offer(E t);
}
