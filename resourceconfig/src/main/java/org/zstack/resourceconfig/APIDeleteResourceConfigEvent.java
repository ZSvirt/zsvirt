package org.zstack.resourceconfig;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vo.ResourceVO;

@RestResponse
public class APIDeleteResourceConfigEvent extends APIEvent {
    public APIDeleteResourceConfigEvent() {
    }

    public APIDeleteResourceConfigEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteResourceConfigEvent __example__() {
        return new APIDeleteResourceConfigEvent(uuid(ResourceVO.class));
    }
}
