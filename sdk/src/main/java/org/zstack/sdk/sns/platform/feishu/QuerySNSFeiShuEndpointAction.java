package org.zstack.sdk.sns.platform.feishu;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class QuerySNSFeiShuEndpointAction extends QueryAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.sns.platform.feishu.QuerySNSFeiShuEndpointResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }



    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        org.zstack.sdk.sns.platform.feishu.QuerySNSFeiShuEndpointResult value = res.getResult(org.zstack.sdk.sns.platform.feishu.QuerySNSFeiShuEndpointResult.class);
        ret.value = value == null ? new org.zstack.sdk.sns.platform.feishu.QuerySNSFeiShuEndpointResult() : value; 

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
        info.httpMethod = "GET";
        info.path = "/sns/application-endpoints/feishu";
        info.needSession = true;
        info.needPoll = false;
        info.parameterName = "";
        return info;
    }

}
