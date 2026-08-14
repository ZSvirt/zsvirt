package org.zstack.externalbackup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by MaJin on 2019/12/4.
 */
@RestResponse
public class APIDeleteExternalBackupEvent extends APIEvent {
    public APIDeleteExternalBackupEvent() {
        super();
    }

    public APIDeleteExternalBackupEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteExternalBackupEvent __example__() {
        return new APIDeleteExternalBackupEvent(uuid(ExternalBackupVO.class));
    }
}
