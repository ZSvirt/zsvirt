package org.zstack.expon.sdk.uss;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponQuery;
import org.zstack.expon.sdk.ExponQueryRequest;
import org.zstack.expon.sdk.ExponRestRequest;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/wds/uss",
        method = HttpMethod.GET,
        responseClass = QueryUssGatewayResponse.class
)
@ExponQuery(replyClass = QueryUssGatewayResponse.class, inventoryClass = UssGatewayModule.class)
public class QueryUssGatewayRequest extends ExponQueryRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
