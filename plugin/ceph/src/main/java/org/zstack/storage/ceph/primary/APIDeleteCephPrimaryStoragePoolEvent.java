package org.zstack.storage.ceph.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/2/28.
 */
@RestResponse
public class APIDeleteCephPrimaryStoragePoolEvent extends APIEvent {
    public APIDeleteCephPrimaryStoragePoolEvent() {
    }

    public APIDeleteCephPrimaryStoragePoolEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteCephPrimaryStoragePoolEvent __example__() {
        APIDeleteCephPrimaryStoragePoolEvent msg = new APIDeleteCephPrimaryStoragePoolEvent();
        return msg;
    }
    
}