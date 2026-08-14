package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class CalculateResourceSpendingAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.CalculateResourceSpendingResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = false, validValues = {"VM","cpu","memory","rootVolume","dataVolume","snapShot","gpu","pubIpVipBandwidth","pubIpVipBandwidthIn","pubIpVipBandwidthOut","pubIpVmNicBandwidth","pubIpVmNicBandwidthIn","pubIpVmNicBandwidthOut","bareMetal2Instance","all"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String resourceType;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String resourceUuid;

    @Param(required = false, validRegexValues = "(\\d{4})([0-1]\\d)([0-3]\\d)\\s([0-5]\\d):([0-5]\\d):([0-5]\\d)", nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String dateStart;

    @Param(required = false, validRegexValues = "(\\d{4})([0-1]\\d)([0-3]\\d)\\s([0-5]\\d):([0-5]\\d):([0-5]\\d)", nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String dateEnd;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,9223372036854775807L}, noTrim = false)
    public java.lang.Integer start;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,10000L}, noTrim = false)
    public java.lang.Integer limit;

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


    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        org.zstack.sdk.CalculateResourceSpendingResult value = res.getResult(org.zstack.sdk.CalculateResourceSpendingResult.class);
        ret.value = value == null ? new org.zstack.sdk.CalculateResourceSpendingResult() : value; 

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
        info.path = "/billings/resources/actions";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "calculateResourceSpending";
        return info;
    }

}
