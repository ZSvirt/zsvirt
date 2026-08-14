package org.zstack.header.core;


import java.util.function.Consumer;

public interface SafeConsumer<T> extends Consumer<T> {
    @ExceptionSafe
    default void safeAccept(T t) {
        accept(t);
    }
}
