package org.zstack.message;

import org.zstack.header.message.APIEvent;

/**
 * Created by MaJin on 2020/10/21.
 */
public class APIRelayMessageEvent extends APIEvent {
    public APIRelayMessageEvent() {
        super();
    }

    public APIRelayMessageEvent(String apiId) {
        super(apiId);
    }
}
