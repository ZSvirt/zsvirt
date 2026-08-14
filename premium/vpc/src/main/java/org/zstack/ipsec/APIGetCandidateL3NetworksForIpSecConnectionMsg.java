package org.zstack.ipsec;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.service.vip.VipVO;

/**
 * @author: shixin.ruan
 * @date: 2021-01-04
 **/
@RestRequest(
        path = "/ipsec/candidatesL3Networks",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateL3NetworksForIpSecConnectionReply.class
)
public class APIGetCandidateL3NetworksForIpSecConnectionMsg extends APIGetMessage {
    @APIParam(resourceType = IPsecConnectionVO.class, required = false)
    private String uuid;

    @APIParam(resourceType = L3NetworkVO.class, required = false)
    private String publicL3Uuid;

    @APIParam(resourceType = VipVO.class, required = false)
    private String vipUuid;

    public String getVipUuid(){
        return vipUuid;
    }

    public void setVipUuid(String vipUuid){ this.vipUuid = vipUuid; }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPublicL3Uuid() {
        return publicL3Uuid;
    }

    public void setPublicL3Uuid(String publicL3Uuid) {
        this.publicL3Uuid = publicL3Uuid;
    }

    public static APIGetCandidateL3NetworksForIpSecConnectionMsg __example__() {
        APIGetCandidateL3NetworksForIpSecConnectionMsg msg = new APIGetCandidateL3NetworksForIpSecConnectionMsg();

        msg.setUuid(uuid());

        return msg;
    }
}
