package org.zstack.header.message;

/**
 * Created by frank on 10/18/2015.
 */
public abstract class AbstractBeforeSendMessageInterceptor implements BeforeSendMessageInterceptor {
    public int orderOfBeforeSendMessageInterceptor() {
        return 0;
    }
}
