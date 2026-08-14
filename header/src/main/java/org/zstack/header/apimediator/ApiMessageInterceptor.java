package org.zstack.header.apimediator;

import org.zstack.header.message.APIMessage;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:37 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ApiMessageInterceptor {
    APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException;

    default int getPriority() {
        return 0;
    }
}
