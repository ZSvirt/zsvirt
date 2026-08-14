package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class CreateSnmpAgentAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.CreateSnmpAgentResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = true, validValues = {"v2c","v3"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String version;

    @Param(required = false, maxLength = 32, minLength = 1, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String readCommunity;

    @Param(required = false, maxLength = 32, minLength = 1, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String userName;

    @Param(required = false, validValues = {"MD5","SHA","SHA224","SHA256","SHA384","SHA512"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String authAlgorithm;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String authPassword;

    @Param(required = false, validValues = {"DES","AES128","AES192","AES256","3DES"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String privacyAlgorithm;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String privacyPassword;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, numberRange = {1024L,65535L}, noTrim = false)
    public int port = 0;

    @Param(required = false)
    public java.lang.String resourceUuid;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.util.List tagUuids;

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
        
        org.zstack.sdk.CreateSnmpAgentResult value = res.getResult(org.zstack.sdk.CreateSnmpAgentResult.class);
        ret.value = value == null ? new org.zstack.sdk.CreateSnmpAgentResult() : value; 

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
        info.httpMethod = "POST";
        info.path = "/snmp/agent";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
