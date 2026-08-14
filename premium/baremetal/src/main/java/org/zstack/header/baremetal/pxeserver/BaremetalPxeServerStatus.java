package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by root on 8/1/17.
 */
public enum BaremetalPxeServerStatus {
    Connecting,
    Connected,
    Disconnected;

    static {
        Connecting.transactions(
                new BaremetalPxeServerStatus.Transaction(BaremetalPxeServerStatusEvent.connecting, Connecting),
                new BaremetalPxeServerStatus.Transaction(BaremetalPxeServerStatusEvent.connected, Connected),
                new BaremetalPxeServerStatus.Transaction(BaremetalPxeServerStatusEvent.disconnected, Disconnected)
        );

        Connected.transactions(
                new BaremetalPxeServerStatus.Transaction(BaremetalPxeServerStatusEvent.connecting, Connecting),
                new BaremetalPxeServerStatus.Transaction(BaremetalPxeServerStatusEvent.disconnected, Disconnected),
                new BaremetalPxeServerStatus.Transaction(BaremetalPxeServerStatusEvent.connected, Connected)
        );

        Disconnected.transactions(
                new BaremetalPxeServerStatus.Transaction(BaremetalPxeServerStatusEvent.connecting, Connecting),
                new BaremetalPxeServerStatus.Transaction(BaremetalPxeServerStatusEvent.connected, Connected),
                new BaremetalPxeServerStatus.Transaction(BaremetalPxeServerStatusEvent.disconnected, Disconnected)
        );
    }

    private static class Transaction {
        BaremetalPxeServerStatusEvent event;
        BaremetalPxeServerStatus nextStatus;

        private Transaction(BaremetalPxeServerStatusEvent event, BaremetalPxeServerStatus nextStatus) {
            this.event = event;
            this.nextStatus = nextStatus;
        }
    }

    private void transactions(BaremetalPxeServerStatus.Transaction... transactions) {
        for (BaremetalPxeServerStatus.Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    private Map<BaremetalPxeServerStatusEvent, Transaction> transactionMap = new HashMap<BaremetalPxeServerStatusEvent, Transaction>();

    public BaremetalPxeServerStatus nextStatus(BaremetalPxeServerStatusEvent event) {
        BaremetalPxeServerStatus.Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next status for current status[%s] on transaction event[%s]",
                    this, event));
        }

        return tran.nextStatus;
    }
}
