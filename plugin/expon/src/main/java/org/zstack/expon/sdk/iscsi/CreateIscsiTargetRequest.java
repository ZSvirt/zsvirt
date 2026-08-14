package org.zstack.expon.sdk.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExponRestRequest(
        path = "/block/iscsi/gateways",
        method = HttpMethod.POST,
        responseClass = CreateIscsiClientGroupResponse.class
)
public class CreateIscsiTargetRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String name;

    @Param(required = false)
    private String description;

    @Param
    private int port = 3260;

    @Param(required = false)
    private String iqn;

    @Param
    private String tianshuId;

    @Param
    private List<IscsiUssResource> nodes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setIqn(String iqn) {
        this.iqn = iqn;
    }

    public String getIqn() {
        return iqn;
    }

    public String getTianshuId() {
        return tianshuId;
    }

    public void setTianshuId(String tianshuId) {
        this.tianshuId = tianshuId;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    public List<IscsiUssResource> getNodes() {
        return nodes;
    }

    public void setNodes(List<IscsiUssResource> nodes) {
        this.nodes = nodes;
    }
}
