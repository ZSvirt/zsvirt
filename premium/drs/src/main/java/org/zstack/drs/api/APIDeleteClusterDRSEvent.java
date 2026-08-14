package org.zstack.drs.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by lining on 2019/12/12.
 */
@RestResponse
public class APIDeleteClusterDRSEvent extends APIEvent {
    public APIDeleteClusterDRSEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteClusterDRSEvent() {
        super(null);
    }
}
