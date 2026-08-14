package org.zstack.header.rest;

import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 2/18/2016.
 */
public interface BeforeAsyncJsonPostInterceptor {
    void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long timeout);

    void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long timeout);
}
