package org.zstack.header.host;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

public enum HostStatus {
    Connecting,
    Connected,
    Disconnected;

    static {
        Connecting.transactions(
                new Transaction(HostStatusEvent.connecting, Connecting),
                new Transaction(HostStatusEvent.connected, Connected),
                new Transaction(HostStatusEvent.disconnected, Disconnected)
        );

        Connected.transactions(
                new Transaction(HostStatusEvent.connecting, Connecting),
                new Transaction(HostStatusEvent.disconnected, Disconnected),
                new Transaction(HostStatusEvent.connected, Connected)
        );

        Disconnected.transactions(
                new Transaction(HostStatusEvent.connecting, Connecting),
                new Transaction(HostStatusEvent.connected, Connected),
                new Transaction(HostStatusEvent.disconnected, Disconnected)
        );
    }

    private static class Transaction {
        HostStatusEvent event;
        HostStatus nextStatus;

        private Transaction(HostStatusEvent event, HostStatus nextStatus) {
            this.event = event;
            this.nextStatus = nextStatus;
        }
    }

    private void transactions(Transaction... transactions) {
        for (Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    private Map<HostStatusEvent, Transaction> transactionMap = new HashMap<HostStatusEvent, Transaction>();

    public HostStatus nextStatus(HostStatusEvent event) {
        Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next status for current status[%s] on transaction event[%s]",
                    this, event));
        }

        return tran.nextStatus;
    }
}
