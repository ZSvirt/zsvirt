package org.zstack.header.message;

/**
 * Created by frank on 10/22/2015.
 */
public interface BeforePublishEventInterceptor {
    int orderOfBeforePublishEventInterceptor();

    void beforePublishEvent(Event evt);
}
