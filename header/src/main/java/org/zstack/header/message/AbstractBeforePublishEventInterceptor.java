package org.zstack.header.message;

/**
 * Created by frank on 10/22/2015.
 */
public abstract class AbstractBeforePublishEventInterceptor implements BeforePublishEventInterceptor {
    public int orderOfBeforePublishEventInterceptor() {
        return 0;
    }
}
