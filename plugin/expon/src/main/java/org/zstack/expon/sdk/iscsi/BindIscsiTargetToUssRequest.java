package org.zstack.expon.sdk.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExponRestRequest(
        path = "/block/iscsi/gateways/{id}/add_nodes",
        method = HttpMethod.PUT,
        responseClass = BindIscsiTargetToUssResponse.class
)
public class BindIscsiTargetToUssRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();
    @Param
    private String id;
    @Param
    private List<IscsiUssResource> nodes;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public List<IscsiUssResource> getNodes() {
        return nodes;
    }

    public void setNodes(List<IscsiUssResource> nodes) {
        this.nodes = nodes;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
