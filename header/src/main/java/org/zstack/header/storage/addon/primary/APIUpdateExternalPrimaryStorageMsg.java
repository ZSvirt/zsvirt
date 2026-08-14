package org.zstack.header.storage.addon.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIUpdatePrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

@RestRequest(
        path = "/primary-storage/addon/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateExternalPrimaryStorageEvent.class
)
public class APIUpdateExternalPrimaryStorageMsg extends APIUpdatePrimaryStorageMsg implements PrimaryStorageMessage {
    @APIParam(required = false)
    private String config;

    @APIParam(required = false, maxLength = 255, validValues = {"Vhost", "Scsi", "Nvme", "CBD", "file"})
    private String defaultProtocol;

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getDefaultProtocol() {
        return defaultProtocol;
    }

    public void setDefaultProtocol(String defaultProtocol) {
        this.defaultProtocol = defaultProtocol;
    }

    public static APIUpdateExternalPrimaryStorageMsg __example__() {
        APIUpdateExternalPrimaryStorageMsg msg = new APIUpdateExternalPrimaryStorageMsg();
        msg.setUuid(uuid());
        msg.setName("My Primary Storage");
        msg.setDescription("New description");
        msg.setDefaultProtocol("Vhost");
        msg.setConfig("{\"pools\":[{\"name\":\"pool1\",\"aliasName\":\"pool-high\"}]}");
        return msg;
    }
}
