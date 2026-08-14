package org.zstack.zwatch.api;

import org.zstack.header.rest.RestResponse;
import org.zstack.header.search.APIGetReply;
import org.zstack.zwatch.datatype.AuditData;
import org.zstack.zwatch.datatype.AuditDataV2;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "audits")
public class APIGetAuditDataReply extends APIGetReply {
    private List<AuditData> audits;

    public static APIGetAuditDataReply __example__() {
        APIGetAuditDataReply ret = new APIGetAuditDataReply();
        ret.setAudits(asList(AuditDataV2.__example__()));
        return ret;
    }

    public List<AuditData> getAudits() {
        return audits;
    }

    public void setAudits(List<AuditData> audits) {
        this.audits = audits;
    }
}
