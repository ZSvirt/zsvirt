package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class ChangeIPsecConnectionAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.ChangeIPsecConnectionResult value;

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

    @Param(required = true, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String peerAddress;

    @Param(required = false, validValues = {"psk","certs"}, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String authMode = "psk";

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String authKey;

    @Param(required = false, validValues = {"ip","name","fqdn"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String idType = "ip";

    @Param(required = false, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String localId;

    @Param(required = false, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String remoteId;

    @Param(required = false, validValues = {"ike","ikev1","ikev2"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ikeVersion = "ike";

    @Param(required = false, validValues = {"md5","sha1","sha256","sha384","sha512"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ikeAuthAlgorithm = "sha256";

    @Param(required = false, validValues = {"3des","aes-128","aes-192","aes-256"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String ikeEncryptionAlgorithm = "aes-256";

    @Param(required = false)
    public int ikeDhGroup = 2;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {60L,604800L}, noTrim = false)
    public int ikeLifeTime = 86400;

    @Param(required = false, validValues = {"md5","sha1","sha256","sha384","sha512"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String policyAuthAlgorithm = "sha256";

    @Param(required = false, validValues = {"3des","aes-128","aes-192","aes-256"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String policyEncryptionAlgorithm = "aes-256";

    @Param(required = false, validValues = {"none","dh-group2","dh-group5","dh-group14","dh-group15","dh-group16","dh-group17","dh-group18","dh-group19","dh-group20","dh-group21","dh-group22","dh-group23","dh-group24","dh-group25","dh-group26"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String pfs = "dh-group14";

    @Param(required = false, validValues = {"tunnel","transport"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String policyMode = "tunnel";

    @Param(required = false, validValues = {"esp","ah","ah-esp"}, maxLength = 32, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String transformProtocol = "esp";

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {30L,604800L}, noTrim = false)
    public int lifeTime = 3600;

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
        
        org.zstack.sdk.ChangeIPsecConnectionResult value = res.getResult(org.zstack.sdk.ChangeIPsecConnectionResult.class);
        ret.value = value == null ? new org.zstack.sdk.ChangeIPsecConnectionResult() : value; 

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
        info.path = "/ipsec/config/{uuid}";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "changeIPsecConnection";
        return info;
    }

}
