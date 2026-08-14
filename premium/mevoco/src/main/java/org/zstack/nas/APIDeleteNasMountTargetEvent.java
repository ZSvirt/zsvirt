package org.zstack.nas;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2018/3/9.
 */
@RestResponse
public class APIDeleteNasMountTargetEvent extends APIEvent {
    public APIDeleteNasMountTargetEvent() {
    }

    public APIDeleteNasMountTargetEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteNasMountTargetEvent __example__() {
        APIDeleteNasMountTargetEvent event = new APIDeleteNasMountTargetEvent();


        return event;
    }
}