package org.zstack.zops;

import java.util.List;

public class UpdateChronyServersManagementNodeMsg extends ManagementNodeMessage {
    private List<String> chronyServers;

    public List<String> getChronyServers() {
        return chronyServers;
    }

    public void setChronyServers(List<String> chronyServers) {
        this.chronyServers = chronyServers;
    }
}
