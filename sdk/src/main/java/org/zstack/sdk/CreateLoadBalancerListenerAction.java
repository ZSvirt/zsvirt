package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class CreateLoadBalancerListenerAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.CreateLoadBalancerListenerResult value;

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
    public java.lang.String loadBalancerUuid;

    @Param(required = true, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String name;

    @Param(required = false, maxLength = 2048, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String description;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,65535L}, noTrim = false)
    public java.lang.Integer instancePort;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,65535L}, noTrim = false)
    public int loadBalancerPort = 0;

    @Param(required = false, validValues = {"udp","tcp","http","https"}, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String protocol;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String certificateUuid;

    @Param(required = false, validValues = {"tcp","udp","http"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String healthCheckProtocol;

    @Param(required = false, validValues = {"GET","HEAD"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String healthCheckMethod;

    @Param(required = false, validRegexValues = "^/[A-Za-z0-9-/.%?#&]*", maxLength = 80, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String healthCheckURI;

    @Param(required = false, maxLength = 80, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String healthCheckHttpCode;

    @Param(required = false, validValues = {"enable","disable"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String aclStatus = "disable";

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.util.List aclUuids;

    @Param(required = false, validValues = {"white","black"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String aclType = "black";

    @Param(required = false, validValues = {"tls_cipher_policy_default","tls_cipher_policy_1_0","tls_cipher_policy_1_1","tls_cipher_policy_1_2","tls_cipher_policy_1_2_strict","tls_cipher_policy_1_2_strict_with_1_3"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String securityPolicyType;

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
        
        org.zstack.sdk.CreateLoadBalancerListenerResult value = res.getResult(org.zstack.sdk.CreateLoadBalancerListenerResult.class);
        ret.value = value == null ? new org.zstack.sdk.CreateLoadBalancerListenerResult() : value; 

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
        info.path = "/load-balancers/{loadBalancerUuid}/listeners";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
