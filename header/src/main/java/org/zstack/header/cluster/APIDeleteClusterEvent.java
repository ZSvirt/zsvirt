package org.zstack.header.cluster;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * @apiResult
 * @example {
 * "org.zstack.header.cluster.APIDeleteClusterEvent": {
 * "success": true
 * }
 * }
 * @since 0.1.0
 */

@RestResponse
public class APIDeleteClusterEvent extends APIEvent {

    public APIDeleteClusterEvent() {
        super(null);
    }

    public APIDeleteClusterEvent(String apiId) {
        super(apiId);
    }

 
    public static APIDeleteClusterEvent __example__() {
        APIDeleteClusterEvent event = new APIDeleteClusterEvent();
        event.setSuccess(true);
        return event;
    }

}
