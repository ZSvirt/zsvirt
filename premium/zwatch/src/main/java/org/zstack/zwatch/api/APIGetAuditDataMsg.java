package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.core.Platform;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.zwatch.datatype.AuditType;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/zwatch/audits", method = HttpMethod.GET, responseClass = APIGetAuditDataReply.class)
public class APIGetAuditDataMsg extends APISyncCallMessage {
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    private Long startTime;
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    private Long endTime;
    @APIParam(numberRange = {0, Integer.MAX_VALUE}, required = false)
    private Integer limit = 100;
    @APIParam(required = false)
    private List<String> conditions;
    @APIParam(validValues = {"Login", "Resource"}, required = false)
    private AuditType auditType;

    public static APIGetAuditDataMsg __example__() {
        APIGetAuditDataMsg msg = new APIGetAuditDataMsg();
        msg.setStartTime(DocUtils.date);
        msg.setLimit(50);
        msg.setConditions(asList(String.format("%s=%s", VmNamespace.LabelNames.VMUuid.toString(), uuid(VmInstanceVO.class))));
        msg.setAuditType(AuditType.Resource);
        return msg;
    }

    @APINoSee
    private List<Label> labelList;

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public List<String> getConditions() {
        return conditions;
    }

    public void setConditions(List<String> conditions) {
        this.conditions = conditions;
    }

    public List<Label> getLabelList() {
        return labelList;
    }

    public void setLabelList(List<Label> labelList) {
        this.labelList = labelList;
    }

    public AuditType getAuditType() {
        return auditType;
    }

    public void setAuditType(AuditType auditType) {
        this.auditType = auditType;
    }
}
