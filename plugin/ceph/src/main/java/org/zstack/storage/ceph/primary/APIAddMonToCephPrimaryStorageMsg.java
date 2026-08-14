package org.zstack.storage.ceph.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.Collections;
import java.util.List;

/**
 * Created by frank on 8/6/2015.
 */
@RestRequest(
        path = "/primary-storage/ceph/{uuid}/mons",
        method = HttpMethod.POST,
        responseClass = APIAddMonToCephPrimaryStorageEvent.class,
        parameterName = "params"
)
public class APIAddMonToCephPrimaryStorageMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = CephPrimaryStorageVO.class)
    private String uuid;
    @APIParam(nonempty = true)
    @NoLogging(type = NoLogging.Type.Uri)
    private List<String> monUrls;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getMonUrls() {
        return monUrls;
    }

    public void setMonUrls(List<String> monUrls) {
        this.monUrls = monUrls;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }
 
    public static APIAddMonToCephPrimaryStorageMsg __example__() {
        APIAddMonToCephPrimaryStorageMsg msg = new APIAddMonToCephPrimaryStorageMsg();

        msg.setUuid(uuid());
        msg.setMonUrls(Collections.singletonList("10.0.1.3"));

        return msg;
    }
}
