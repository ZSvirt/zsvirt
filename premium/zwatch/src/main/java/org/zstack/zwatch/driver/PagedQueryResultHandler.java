package org.zstack.zwatch.driver;

/**
 * Created by lining on 2018/11/14.
 */
public interface PagedQueryResultHandler<T> {
    void handle(T result);
}
