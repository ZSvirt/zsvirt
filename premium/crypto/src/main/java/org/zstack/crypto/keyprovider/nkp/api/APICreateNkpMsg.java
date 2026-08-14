package org.zstack.crypto.keyprovider.nkp.api;

import org.springframework.http.HttpMethod;
import org.zstack.crypto.keyprovider.api.APICreateKeyProviderMsg;
import org.zstack.header.keyprovider.KeyProviderType;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.NkpKdf;
import org.zstack.header.keyprovider.NkpSaltPolicy;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

@TagResourceType(KeyProviderVO.class)
@RestRequest(
        path = "/key-providers/nkp",
        method = HttpMethod.POST,
        responseClass = APICreateNkpEvent.class,
        parameterName = "params"
)
public class APICreateNkpMsg extends APICreateKeyProviderMsg implements APIAuditor {
    @APIParam(required = false, maxLength = 64, validEnums = {NkpKdf.class})
    private String kdf = NkpKdf.HKDF_SHA256.toString();

    @APIParam(required = false, maxLength = 64, validEnums = {NkpSaltPolicy.class})
    private String saltPolicy = NkpSaltPolicy.PROVIDER_NAME.toString();

    public String getKdf() {
        return kdf;
    }

    public void setKdf(String kdf) {
        this.kdf = kdf;
    }

    public String getSaltPolicy() {
        return saltPolicy;
    }

    public void setSaltPolicy(String saltPolicy) {
        this.saltPolicy = saltPolicy;
    }

    @Override
    public String getType() {
        return KeyProviderType.NKP.name();
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String uuid = "";
        if (rsp.isSuccess()) {
            APICreateNkpEvent evt = (APICreateNkpEvent) rsp;
            if (evt.getInventory() != null) {
                uuid = evt.getInventory().getUuid();
            }
        }

        return new Result(uuid, KeyProviderVO.class);
    }

    public static APICreateNkpMsg __example__() {
        APICreateNkpMsg msg = new APICreateNkpMsg();
        msg.setName("kp-nkp");
        msg.setDescription("example");
        msg.setKdf(NkpKdf.HKDF_SHA256.toString());
        msg.setSaltPolicy(NkpSaltPolicy.PROVIDER_NAME.toString());
        return msg;
    }
}
