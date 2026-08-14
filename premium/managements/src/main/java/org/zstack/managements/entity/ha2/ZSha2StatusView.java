package org.zstack.managements.entity.ha2;

import org.zstack.managements.entity.common.ManagementNodeStatusView;
import org.zstack.managements.entity.common.ManagementsStatusView;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class ZSha2StatusView {
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

    public ManagementsStatusView toManagementsStatusView() {
        ManagementsStatusView result = new ManagementsStatusView();
        result.setVip(this.getVip());
        result.setUiHttpPath(this.getUiHttpPath());
        result.setNodes(this.getNodes());
        return result;
    }

    public static ZSha2StatusView __example__() {
        ZSha2StatusView view = new ZSha2StatusView();
        view.setVip("172.26.30.7");
        view.setUiHttpPath("https://172.26.30.7:443");
        view.setNodes(list(ManagementNodeStatusView.__example1__(), ManagementNodeStatusView.__example2__()));
        return view;
    }
}
