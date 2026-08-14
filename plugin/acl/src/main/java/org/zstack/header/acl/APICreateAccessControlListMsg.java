package org.zstack.header.acl;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-09
 **/
@TagResourceType(AccessControlListVO.class)
@RestRequest(
        path = "/access-control-lists",
        method = HttpMethod.POST,
        responseClass = APICreateAccessControlListEvent.class,
        parameterName = "params"
)
public class APICreateAccessControlListMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(validValues = {"4", "6"}, required = false)
    private Integer ipVersion;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public static APICreateAccessControlListMsg __example__() {
        APICreateAccessControlListMsg msg = new APICreateAccessControlListMsg();

        msg.setName("acl-group");
        msg.setIpVersion(4);

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateAccessControlListEvent)rsp).getInventory().getUuid() : "", AccessControlListVO.class);
    }
}
