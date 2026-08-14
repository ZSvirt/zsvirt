package org.zstack.zsv.core.entity;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.host.HostVO;
import org.zstack.utils.StringDSL;

/**
 * Created by Wenhao.Zhang on 2025/03/05
 */
@PythonClass
public class NodeRolesItemView {
    private String uuid;
    private String resourceType;
    private String role;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public static NodeRolesItemView __example__() {
        NodeRolesItemView view = new NodeRolesItemView();
        view.uuid = StringDSL.createFixedUuid(HostVO.class);
        view.resourceType = "HostVO";
        view.role = "compute";
        return view;
    }
}
