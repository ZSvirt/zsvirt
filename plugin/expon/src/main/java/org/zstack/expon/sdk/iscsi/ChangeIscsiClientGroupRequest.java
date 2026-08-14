package org.zstack.expon.sdk.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExponRestRequest(
        path = "/block/iscsi/clients/{id}/hosts",
        method = HttpMethod.PUT,
        responseClass = ChangeIscsiClientGroupResponse.class
)
public class ChangeIscsiClientGroupRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String id;
    @Param(validValues = {"add", "remove"})
    private String action;
    @Param
    private List<IscsiClient> hosts;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<IscsiClient> getHosts() {
        return hosts;
    }

    public void setHosts(List<IscsiClient> hosts) {
        this.hosts = hosts;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
