package org.zstack.header.rest;

import org.zstack.header.message.APIMessage;

public interface RESTApiFacade {
    RestAPIResponse send(APIMessage msg);

    RestAPIResponse call(APIMessage msg);

    RestAPIResponse getResult(String uuid);
}
