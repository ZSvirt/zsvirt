package org.zstack.managements.entity.common;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class ManagementsStatusView {
    private String vip;
    private String uiHttpPath;
    private List<ManagementNodeStatusView> nodes;

    public String getVip() {
        return vip;
    }

    public void setVip(String vip) {
        this.vip = vip;
    }

    public String getUiHttpPath() {
        return uiHttpPath;
    }

    public void setUiHttpPath(String uiHttpPath) {
        this.uiHttpPath = uiHttpPath;
    }

    public List<ManagementNodeStatusView> getNodes() {
        return nodes;
    }

    public void setNodes(List<ManagementNodeStatusView> nodes) {
        this.nodes = nodes;
    }

    public static ManagementsStatusView __example__() {
        ManagementsStatusView view = new ManagementsStatusView();
        view.setVip("172.26.30.7");
        view.setUiHttpPath("https://172.26.30.7:443");
        view.setNodes(list(ManagementNodeStatusView.__example1__(), ManagementNodeStatusView.__example2__()));
        return view;
    }
}
