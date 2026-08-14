package org.zstack.ovf.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Qi Le on 2022/4/26
 */
@RestResponse
public class APIDeleteImagePackageEvent extends APIEvent {
    public APIDeleteImagePackageEvent() {
        super();
    }

    public APIDeleteImagePackageEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteImagePackageEvent __example__() {
        return new APIDeleteImagePackageEvent();
    }
}
