package org.zstack.sdk;

/**
 * Created by xing5 on 2016/12/11.
 */
public interface Completion<T> {
    void complete(T ret);
}
