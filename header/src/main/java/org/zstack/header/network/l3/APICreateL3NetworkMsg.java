package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 * @api create l3Network
 * @category l3Network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l3.APICreateL3NetworkMsg": {
 * "name": "GuestNetwork",
 * "description": "Test",
 * "type": "L3BasicNetwork",
 * "l2NetworkUuid": "2f5e0755584d41dabb73c7dcbee2fe29",
 * "session": {
 * "uuid": "ec153083110d4b508bebb8750b62a393"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l3.APICreateL3NetworkMsg": {
 * "name": "GuestNetwork",
 * "description": "Test",
 * "type": "L3BasicNetwork",
 * "l2NetworkUuid": "2f5e0755584d41dabb73c7dcbee2fe29",
 * "session": {
 * "uuid": "ec153083110d4b508bebb8750b62a393"
 * },
 * "timeout": 1800000,
 * "id": "d0248f2636af4d4cb93667f55e67b560",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APICreateL3NetworkEvent`
 * @since 0.1.0
 */
@TagResourceType(L3NetworkVO.class)
@RestRequest(
        path = "/l3-networks",
        method = HttpMethod.POST,
        responseClass = APICreateL3NetworkEvent.class,
        parameterName = "params"
)
public class APICreateL3NetworkMsg extends APICreateMessage implements APIAuditor {
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;
    /**
     * @desc l3Network type
     */
    private String type = L3NetworkConstant.L3_BASIC_NETWORK_TYPE;
    /**
     * @desc uuid of l2Network the l3Network is being created on
     */
    @APIParam(resourceType = L2NetworkVO.class)
    private String l2NetworkUuid;

    @APIParam(required = false, validValues = {"Public", "Private", "System"})
    private String category = L3NetworkCategory.Private.toString();

    @APIParam(required = false, validValues = {"4", "6"})
    private Integer ipVersion;

    private boolean system;

    private String dnsDomain;

    @APIParam(required = false)
    private Boolean enableIPAM = Boolean.TRUE;

    public String getDnsDomain() {
        return dnsDomain;
    }

    public void setDnsDomain(String domain) {
        this.dnsDomain = domain;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public Boolean getEnableIPAM() {
        return enableIPAM;
    }

    public void setEnableIPAM(Boolean enableIPAM) {
        this.enableIPAM = enableIPAM;
    }

    public static APICreateL3NetworkMsg __example__() {
        APICreateL3NetworkMsg msg = new APICreateL3NetworkMsg();

        msg.setName("Test-L3Network");
        msg.setL2NetworkUuid(uuid());
        msg.setCategory(L3NetworkCategory.Private.toString());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateL3NetworkEvent) rsp).getInventory().getUuid() : "", L3NetworkVO.class);
    }
}
