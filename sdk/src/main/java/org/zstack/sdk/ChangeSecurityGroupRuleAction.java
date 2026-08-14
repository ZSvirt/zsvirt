package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class ChangeSecurityGroupRuleAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.ChangeSecurityGroupRuleResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = true, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String uuid;

    @Param(required = false, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String description;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String remoteSecurityGroupUuid;

    @Param(required = false, validValues = {"DROP","ACCEPT"}, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String action;

    @Param(required = false, validValues = {"Enabled","Disabled"}, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String state;

    @Param(required = false, maxLength = 4, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.Integer priority;

    @Param(required = false, validValues = {"ALL","TCP","UDP","ICMP"}, nonempty = true, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String protocol;

    @Param(required = false, maxLength = 1024, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String srcIpRange;

    @Param(required = false, maxLength = 1024, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String dstIpRange;

    @Param(required = false, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String dstPortRange;

    @Param(required = false)
    public java.util.List systemTags;

    @Param(required = false)
    public java.util.List userTags;

    @Param(required = false)
    public String sessionId;

    @Param(required = false)
    public String accessKeyId;

    @Param(required = false)
    public String accessKeySecret;

    @Param(required = false)
    public String requestIp;

    @NonAPIParam
    public long timeout = -1;

    @NonAPIParam
    public long pollingInterval = -1;


    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        org.zstack.sdk.ChangeSecurityGroupRuleResult value = res.getResult(org.zstack.sdk.ChangeSecurityGroupRuleResult.class);
        ret.value = value == null ? new org.zstack.sdk.ChangeSecurityGroupRuleResult() : value; 

        return ret;
    }

    public Result call() {
        ApiResult res = ZSClient.call(this);
        return makeResult(res);
    }

    public void call(final Completion<Result> completion) {
        ZSClient.call(this, new InternalCompletion() {
            @Override
            public void complete(ApiResult res) {
                completion.complete(makeResult(res));
            }
        });
    }

    protected Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    protected Map<String, Parameter> getNonAPIParameterMap() {
        return nonAPIParameterMap;
    }

    protected RestInfo getRestInfo() {
        RestInfo info = new RestInfo();
        info.httpMethod = "PUT";
        info.path = "/security-groups/rules/{uuid}/actions";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "changeSecurityGroupRule";
        return info;
    }

}
