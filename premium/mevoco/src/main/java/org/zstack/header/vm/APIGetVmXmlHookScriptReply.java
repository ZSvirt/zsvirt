package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = "all")
public class APIGetVmXmlHookScriptReply extends APIReply {

    private String userDefinedXmlHookScript;

    public String getUserDefinedXmlHookScript() {
        return userDefinedXmlHookScript;
    }

    public void setUserDefinedXmlHookScript(String userDefinedXmlHookScript) {
        this.userDefinedXmlHookScript = userDefinedXmlHookScript;
    }

}
