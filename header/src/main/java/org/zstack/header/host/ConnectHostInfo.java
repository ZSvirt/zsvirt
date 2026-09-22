package org.zstack.header.host;

public class ConnectHostInfo {
    public final boolean newAdded;
    public final boolean startPingTaskOnFailure;
    public final boolean reconnect;

    /**
     * do not install these packages when connect host.
     * set by connect flow.
     */
    public String skipPackages;

    private ConnectHostInfo(
            boolean newAdded,
            boolean startPingTaskOnFailure,
            boolean reconnect) {
        this.newAdded = newAdded;
        this.startPingTaskOnFailure = startPingTaskOnFailure;
        this.reconnect = reconnect;
    }

    public static ConnectHostInfo fromConnectHostMsg(ConnectHostMsg msg) {
        return new ConnectHostInfo(msg.isNewAdd(),
                msg.isStartPingTaskOnFailure(),
                msg.isReconnect());
    }
}
