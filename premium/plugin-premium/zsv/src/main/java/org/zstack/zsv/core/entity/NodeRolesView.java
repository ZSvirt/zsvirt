package org.zstack.zsv.core.entity;

import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.utils.StringDSL;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by Wenhao.Zhang on 2025/03/04
 */
public class NodeRolesView {
    private String uuid;
    private String resourceType;
    private List<NodeRolesItemView> roles;

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

    public List<NodeRolesItemView> getRoles() {
        return roles;
    }

    public void setRoles(List<NodeRolesItemView> roles) {
        this.roles = roles;
    }

    public static NodeRolesView __example__() {
        NodeRolesView view = new NodeRolesView();
        view.uuid = StringDSL.createFixedUuid(ManagementNodeVO.class);
        view.resourceType = "ManagementNodeVO";

        NodeRolesItemView next = new NodeRolesItemView();
        next.setUuid(view.getUuid());
        next.setResourceType(view.getResourceType());
        next.setRole("management");

        view.roles = list(next, NodeRolesItemView.__example__());
        return view;
    }
}
