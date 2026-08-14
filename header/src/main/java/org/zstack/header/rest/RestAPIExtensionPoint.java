package org.zstack.header.rest;

import org.zstack.header.message.Message;

/**
 * Created by mingjian.deng on 2020/3/19.
 */
public interface RestAPIExtensionPoint {
    void afterAPIRequest(final Message method);
    void beforeAPIResponse(final Message msg);

    void beforeRestResponse(String method, int statusCode);
    void afterRestRequest(String method);
}
