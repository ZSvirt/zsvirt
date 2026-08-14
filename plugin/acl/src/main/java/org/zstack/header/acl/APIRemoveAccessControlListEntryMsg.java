package org.zstack.header.acl;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-09
 **/
@RestRequest(
        path = "/access-control-lists/{aclUuid}/ipentries/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveAccessControlListEntryEvent.class
)
public class APIRemoveAccessControlListEntryMsg extends APIDeleteMessage implements APIAuditor{
    @APIParam(resourceType = AccessControlListVO.class)
    private String aclUuid;
    @APIParam(resourceType = AccessControlListEntryVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public  String getAclUuid() {
        return aclUuid;
    }

    public void setAclUuid(String aclUuid) {
        this.aclUuid = aclUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIRemoveAccessControlListEntryMsg __example__() {
        APIRemoveAccessControlListEntryMsg msg = new APIRemoveAccessControlListEntryMsg();
        msg.setUuid(uuid());
        msg.setAclUuid(uuid());

        return msg;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIRemoveAccessControlListEntryMsg)msg).getUuid(), AccessControlListEntryVO.class);
    }
}
