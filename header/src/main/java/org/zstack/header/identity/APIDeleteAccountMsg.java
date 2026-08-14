package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/15/2015.
 */
@RestRequest(
        path = "/accounts/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAccountEvent.class
)
public class APIDeleteAccountMsg extends APIDeleteMessage implements AccountMessage {
    @APIParam(resourceType = AccountVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAccountUuid() {
        return uuid;
    }

    @Override
    public List<String> getDeletedResourceUuidList() {
        return list(getUuid());
    }

    public static APIDeleteAccountMsg __example__() {
        APIDeleteAccountMsg msg = new APIDeleteAccountMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
