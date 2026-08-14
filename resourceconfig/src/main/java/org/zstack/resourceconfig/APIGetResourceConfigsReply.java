package org.zstack.resourceconfig;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(fieldsTo = {"configs"})
public class APIGetResourceConfigsReply extends APIReply {
    private List<ResourceConfigStruct> configs;

    public List<ResourceConfigStruct> getConfigs() {
        return configs;
    }

    public void setConfigs(List<ResourceConfigStruct> configs) {
        this.configs = configs;
    }
}
