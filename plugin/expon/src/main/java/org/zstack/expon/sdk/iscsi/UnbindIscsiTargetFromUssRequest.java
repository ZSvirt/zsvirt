package org.zstack.expon.sdk.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExponRestRequest(
        path = "/block/iscsi/gateways/{id}/remove_nodes",
        method = HttpMethod.PUT,
        responseClass = UnbindIscsiTargetFromUssResponse.class
)
public class UnbindIscsiTargetFromUssRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();
    @Param
    private String id;
    @Param
    private List<IscsiUssResource> nodes;
    @Param
    private boolean force;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<IscsiUssResource> getNodes() {
        return nodes;
    }

    public void setNodes(List<IscsiUssResource> nodes) {
        this.nodes = nodes;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
