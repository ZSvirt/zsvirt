package org.zstack.core.config;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(fieldsTo = { "options" })
public class APIGetGlobalConfigOptionsReply extends APIReply {
    GlobalConfigOptions options;

    public GlobalConfigOptions getOptions() {
        return options;
    }

    public void setOptions(GlobalConfigOptions options) {
        this.options = options;
    }
}