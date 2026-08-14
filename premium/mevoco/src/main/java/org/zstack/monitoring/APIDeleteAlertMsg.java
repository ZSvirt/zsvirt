package org.zstack.monitoring;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/6/18.
 */
@RestRequest(
        path = "/monitoring/alerts",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAlertEvent.class
)
@Deprecated
public class APIDeleteAlertMsg extends APIDeleteMessage {
    @APIParam(nonempty = true)
    private List<String> uuids;

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

    public static APIDeleteAlertMsg __example__() {
        APIDeleteAlertMsg msg = new APIDeleteAlertMsg();
        msg.uuids = asList(uuid(), uuid());
        return msg;
    }
}
