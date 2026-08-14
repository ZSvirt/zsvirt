package org.zstack.zwatch.migratedb;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.zwatch.ZWatchConstants;
import org.zstack.zwatch.datatype.AuditData;
import org.zstack.zwatch.datatype.AuditDataV2;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = AuditsVO.class, collectionValueOfMethod = "valueOf1")
public class AuditsInventory implements Serializable {
    private long id;
    private long createTime;
    private String apiName;
    private String clientBrowser;
    private String clientIp;
    private long duration;
    private String error;
    private String operator;
    private String requestDump;
    private String resourceUuid;
    private String requestUuid;
    private String operatorAccountUuid;
    private String responseDump;
    private Boolean success;
    private String signedText;
    private String resourceType;
    private String resourceName;

    protected AuditsInventory(AuditsVO vo) {
        this.setId(vo.getId());
        this.setCreateTime(vo.getCreateTime());
        this.setApiName(vo.getApiName());
        this.setClientBrowser(vo.getClientBrowser());
        this.setClientIp(vo.getClientIp());
        this.setDuration(vo.getDuration());
        this.setError(vo.getError());
        this.setOperator(vo.getOperator());
        this.setRequestDump(vo.getRequestDump());
        this.setResourceUuid(vo.getResourceUuid());
        this.setResponseDump(vo.getResponseDump());
        this.setSuccess(vo.getSuccess());
        this.setRequestUuid(vo.getRequestUuid());
        this.setOperatorAccountUuid(vo.getOperatorAccountUuid());
        this.setSignedText(vo.getSignedText());
        this.setResourceType(vo.getResourceType());
        this.setResourceName(vo.getResourceName());
    }

    public static AuditsInventory valueOf(AuditsVO vo) {
        return new AuditsInventory(vo);
    }

    public static List<AuditsInventory> valueOf1(Collection<AuditsVO> vos) {
        List<AuditsInventory> invs = new ArrayList<AuditsInventory>(vos.size());
        for (AuditsVO vo : vos) {
            invs.add(AuditsInventory.valueOf(vo));
        }
        return invs;
    }

    public AuditsInventory() {
    }

    public long getId() {
        return id;
    }

    public void setId(long $paramName) {
        id = $paramName;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long $paramName) {
        createTime = $paramName;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String $paramName) {
        apiName = $paramName;
    }

    public String getClientBrowser() {
        return clientBrowser;
    }

    public void setClientBrowser(String $paramName) {
        clientBrowser = $paramName;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String $paramName) {
        clientIp = $paramName;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long $paramName) {
        duration = $paramName;
    }

    public String getError() {
        return error;
    }

    public void setError(String $paramName) {
        error = $paramName;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String $paramName) {
        operator = $paramName;
    }

    public String getRequestDump() {
        return requestDump;
    }

    public void setRequestDump(String $paramName) {
        requestDump = $paramName;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String $paramName) {
        resourceUuid = $paramName;
    }

    public String getResponseDump() {
        return responseDump;
    }

    public void setResponseDump(String $paramName) {
        responseDump = $paramName;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean $paramName) {
        success = $paramName;
    }

    public String getSignedText() {
        return signedText;
    }

    public void setSignedText(String signedText) {
        this.signedText = signedText;
    }

    public static AuditData toAuditData(AuditsVO vo) {
        JsonObject resDumpObj = JsonParser.parseString(vo.getResponseDump()).getAsJsonObject();
        JsonObject reqDumpObj = JsonParser.parseString(vo.getRequestDump()).getAsJsonObject();
        String reqSessionId = "";
        String opAccountId = "";
        if (reqDumpObj.has("session")) {
            reqSessionId = reqDumpObj.getAsJsonObject("session").has("uuid") ? reqDumpObj.getAsJsonObject("session").get("uuid").getAsString() : "";
            opAccountId = reqDumpObj.getAsJsonObject("session").has("userUuid") ? reqDumpObj.getAsJsonObject("session").get("userUuid").getAsString() : "";
        }
        String resUuid = resDumpObj.has("id") ? resDumpObj.get("id").getAsString() : "";
        AuditDataV2 data = new AuditDataV2();
        data.setId(vo.getId());
        data.setResourceUuid(vo.getResourceUuid());
        data.setResourceType(vo.getResourceType());
        data.setClientIp(vo.getClientIp());
        data.setClientBrowser(vo.getClientBrowser());
        data.setApiName(vo.getApiName());
        data.setOperatorAccountUuid(opAccountId);
        data.setDuration(vo.getDuration());
        data.setRequestUuid(vo.getRequestUuid());
        data.setResponseUuid(resUuid);
        data.setSessionUuid(reqSessionId);
        data.setResponseDump(vo.getResponseDump());
        data.setRequestDump(vo.getRequestDump());
        data.setOperator(vo.getOperator());
        data.setSuccess(vo.getSuccess() ? ZWatchConstants.AUDIT_SUCCESS: ZWatchConstants.AUDIT_FAILED);
        data.setError(vo.getError());
        data.setOperatorAccountUuid(vo.getOperatorAccountUuid());
        data.setTime(vo.getCreateTime());
        data.setResourceName(vo.getResourceName());
        return data;
    }

    public String getRequestUuid() {
        return requestUuid;
    }

    public void setRequestUuid(String requestUuid) {
        this.requestUuid = requestUuid;
    }

    public String getOperatorAccountUuid() {
        return operatorAccountUuid;
    }

    public void setOperatorAccountUuid(String operatorAccountUuid) {
        this.operatorAccountUuid = operatorAccountUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
}
