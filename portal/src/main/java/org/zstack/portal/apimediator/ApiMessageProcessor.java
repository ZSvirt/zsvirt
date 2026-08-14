package org.zstack.portal.apimediator;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.message.APIMessage;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:45 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ApiMessageProcessor {
    APIMessage process(APIMessage msg) throws ApiMessageInterceptionException;

    ApiMessageDescriptor getApiMessageDescriptor(APIMessage msg);

    List<String> getSupportApis();
}
