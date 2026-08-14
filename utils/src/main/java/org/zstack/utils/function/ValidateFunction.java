package org.zstack.utils.function;

public interface ValidateFunction<T> {
    void validate(T arg) throws Exception;
}
