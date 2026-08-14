package org.zstack.header.identity.login;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "procedures")
public class APIGetLoginProceduresReply extends APIReply {
    List<LoginAuthenticationProcedureDesc> procedures;

    public List<LoginAuthenticationProcedureDesc> getProcedures() {
        return procedures;
    }

    public void setProcedures(List<LoginAuthenticationProcedureDesc> procedures) {
        this.procedures = procedures;
    }
}
