package org.zstack.expon.sdk.pool;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;


@ExponRestRequest(
        path = "/failure_domain/black_list/clean",
        responseClass = ClearFailureDomainBlacklistResponse.class,
        method = HttpMethod.PUT,
        sync = false
)
public class ClearFailureDomainBlacklistRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();
    @Param
    private String poolUuid;

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
