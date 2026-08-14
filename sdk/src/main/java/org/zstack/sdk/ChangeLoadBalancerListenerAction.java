package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class ChangeLoadBalancerListenerAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.ChangeLoadBalancerListenerResult value;

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

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,2147483647L}, noTrim = false)
    public java.lang.Integer connectionIdleTimeout;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {0L,10000000L}, noTrim = false)
    public java.lang.Integer maxConnection;

    @Param(required = false, validValues = {"weightroundrobin","roundrobin","leastconn","source"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String balancerAlgorithm;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String healthCheckTarget;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,2147483647L}, noTrim = false)
    public java.lang.Integer healthyThreshold;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,2147483647L}, noTrim = false)
    public java.lang.Integer unhealthyThreshold;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,2147483647L}, noTrim = false)
    public java.lang.Integer healthCheckInterval;

    @Param(required = false, validValues = {"tcp","udp","http"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String healthCheckProtocol;

    @Param(required = false, validValues = {"GET","HEAD"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String healthCheckMethod;

    @Param(required = false, validRegexValues = "^/[A-Za-z0-9-/.%?#&]*", maxLength = 80, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String healthCheckURI;

    @Param(required = false, maxLength = 80, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String healthCheckHttpCode;

    @Param(required = false, validValues = {"enable","disable"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String aclStatus;

    @Param(required = false, validValues = {"tls_cipher_policy_default","tls_cipher_policy_1_0","tls_cipher_policy_1_1","tls_cipher_policy_1_2","tls_cipher_policy_1_2_strict","tls_cipher_policy_1_2_strict_with_1_3"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String securityPolicyType;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,64L}, noTrim = false)
    public java.lang.Integer nbprocess;

    @Param(required = false, validValues = {"http-keep-alive","http-server-close","http-tunnel","httpclose","forceclose"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String httpMode;

    @Param(required = false, validValues = {"disable","iphash","insert","rewrite"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String sessionPersistence;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {30L,3600L}, noTrim = false)
    public java.lang.Integer sessionIdleTimeout;

    @Param(required = false, validRegexValues = "[A-Za-z0-9_-]+", maxLength = 20, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String cookieName;

    @Param(required = false, validValues = {"disable","enable"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String httpRedirectHttps;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1L,65535L}, noTrim = false)
    public java.lang.Integer redirectPort;

    @Param(required = false, validValues = {"301","302","303","307","308"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.Integer statusCode;

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
        
        org.zstack.sdk.ChangeLoadBalancerListenerResult value = res.getResult(org.zstack.sdk.ChangeLoadBalancerListenerResult.class);
        ret.value = value == null ? new org.zstack.sdk.ChangeLoadBalancerListenerResult() : value; 

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
        info.path = "/load-balancers/listeners/{uuid}/actions";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "changeLoadBalancerListener";
        return info;
    }

}
