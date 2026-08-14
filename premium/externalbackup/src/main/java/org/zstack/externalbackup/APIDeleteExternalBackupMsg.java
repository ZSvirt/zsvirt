package org.zstack.externalbackup;

import org.springframework.http.HttpMethod;
import org.zstack.core.Platform;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by MaJin on 2019/12/4.
 */
@RestRequest(
        path = "/externalbackup/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteExternalBackupEvent.class
)
public class APIDeleteExternalBackupMsg extends APIDeleteMessage implements ExternalBackupMessage {
    @APIParam(resourceType = ExternalBackupVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getExternalBackupUuid() {
        return uuid;
    }

    public static APIDeleteExternalBackupMsg __example__() {
        APIDeleteExternalBackupMsg msg = new APIDeleteExternalBackupMsg();
        msg.uuid = uuid(ExternalBackupVO.class);
        return msg;
    }
}
