package org.zstack.rest;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;

/**
 * Created by xing5 on 2016/12/8.
 */
public interface AsyncRestApiStore {
    void save(RequestData data);

    RequestData complete(APIEvent evt);

    AsyncRestQueryResult query(String uuid);
}
