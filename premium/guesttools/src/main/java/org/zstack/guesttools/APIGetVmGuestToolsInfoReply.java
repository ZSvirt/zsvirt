package org.zstack.guesttools;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import static org.zstack.utils.CollectionDSL.*;

import java.util.Map;

/**
 * Created by GuoYi on 2019-09-17.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetVmGuestToolsInfoReply extends APIReply {
    private String version;
    private String status;
    private Map<String, String> features;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, String> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, String> features) {
        this.features = features;
    }
    
    @SuppressWarnings("unchecked")
    public static APIGetVmGuestToolsInfoReply __example__() {
        APIGetVmGuestToolsInfoReply reply = new APIGetVmGuestToolsInfoReply();
        reply.setVersion("1.0.0");
        reply.setStatus(GuestToolsAgentStatus.RUNNING.toString());
        reply.setFeatures(map(e("pvpanic", "enable")));
        return reply;
    }
}
