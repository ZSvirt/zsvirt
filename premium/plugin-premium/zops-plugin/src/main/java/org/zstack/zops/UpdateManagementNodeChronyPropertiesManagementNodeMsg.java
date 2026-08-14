package org.zstack.zops;

import java.util.List;

public class UpdateManagementNodeChronyPropertiesManagementNodeMsg extends ManagementNodeMessage {
    private List<String> chronyServer;
    public void setChronyServer(List<String> chronyServer) {
        this.chronyServer = chronyServer;
    }
    public List<String> getChronyServer() {
        return chronyServer;
    }

}
