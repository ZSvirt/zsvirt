package org.zstack.sdk.zwatch.monitorgroup.api;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class UpdateMetricRuleTemplateAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.zwatch.monitorgroup.api.UpdateMetricRuleTemplateResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String uuid;

    @Param(required = false, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String name;

    @Param(required = false, validValues = {"GreaterThanOrEqualTo","GreaterThan","LessThan","LessThanOrEqualTo"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String comparisonOperator;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,2147483647L}, noTrim = false)
    public java.lang.Integer period;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,9223372036854775807L}, noTrim = false)
    public java.lang.Double threshold;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,9223372036854775807L}, noTrim = false)
    public java.lang.Integer repeatInterval;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.util.List labels;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {-1L,2147483647L}, noTrim = false)
    public java.lang.Integer repeatCount = -1;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.Boolean enableRecovery = false;

    @Param(required = false, validValues = {"Emergent","Important","Normal"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String emergencyLevel = "Important";

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
        
        org.zstack.sdk.zwatch.monitorgroup.api.UpdateMetricRuleTemplateResult value = res.getResult(org.zstack.sdk.zwatch.monitorgroup.api.UpdateMetricRuleTemplateResult.class);
        ret.value = value == null ? new org.zstack.sdk.zwatch.monitorgroup.api.UpdateMetricRuleTemplateResult() : value; 

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
        info.path = "/zwatch/monitortemplates/metricrules/{uuid}/actions";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "updateMetricRuleTemplate";
        return info;
    }

}
