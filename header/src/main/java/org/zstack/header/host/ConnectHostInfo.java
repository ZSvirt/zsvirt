package org.zstack.header.host;

public class ConnectHostInfo {
    public boolean newAdded;
    public boolean startPingTaskOnFailure;

    // do not install these packages when connect host
    public String skipPackages;

    public static ConnectHostInfo fromConnectHostMsg(ConnectHostMsg msg) {
        ConnectHostInfo info = new ConnectHostInfo();
        info.newAdded = msg.isNewAdd();
        info.startPingTaskOnFailure = msg.isStartPingTaskOnFailure();
        return info;
    }
}
