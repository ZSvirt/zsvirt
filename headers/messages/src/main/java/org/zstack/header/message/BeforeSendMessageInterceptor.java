package org.zstack.header.message;

/**
 * Created by frank on 10/18/2015.
 */
public interface BeforeSendMessageInterceptor {
    int orderOfBeforeSendMessageInterceptor();

    void beforeSendMessage(Message msg);
}
