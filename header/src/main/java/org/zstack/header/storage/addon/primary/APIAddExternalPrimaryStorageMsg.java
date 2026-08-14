package org.zstack.header.storage.addon.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;

@RestRequest(
        path = "/primary-storage/addon",
        method = HttpMethod.POST,
        responseClass = APIAddPrimaryStorageEvent.class,
        parameterName = "params"
)
public class APIAddExternalPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
    @APIParam(maxLength = 255, emptyString = false)
    private String identity;

    @APIParam(validValues = {"Vhost", "iSCSI", "NVMEoF", "CBD", "file"})
    private String defaultOutputProtocol;

    @APIParam(required = false)
    private String config;

    @Override
    public String getType() {
        return PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public static APIAddExternalPrimaryStorageMsg __example__() {
        APIAddExternalPrimaryStorageMsg msg = new APIAddExternalPrimaryStorageMsg();
        msg.setIdentity("zbd");
        msg.setName("my primary storage");
        msg.setUrl("vendorname://user:password@host:port/pool");

        return msg;
    }

    public String getDefaultOutputProtocol() {
        return defaultOutputProtocol;
    }

    public void setDefaultOutputProtocol(String defaultOutputProtocol) {
        this.defaultOutputProtocol = defaultOutputProtocol;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}
