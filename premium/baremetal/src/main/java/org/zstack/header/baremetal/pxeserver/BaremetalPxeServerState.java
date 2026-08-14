package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by GuoYi on 2018-10-10.
 */
public enum BaremetalPxeServerState {
    Enabled,
    Disabled,
    Maintenance;

    static {
        Enabled.transactions(
                new Transaction(BaremetalPxeServerStateEvent.disable, Disabled),
                new Transaction(BaremetalPxeServerStateEvent.enable, Enabled),
                new Transaction(BaremetalPxeServerStateEvent.maintain, Maintenance)
        );

        Disabled.transactions(
                new Transaction(BaremetalPxeServerStateEvent.disable, Disabled),
                new Transaction(BaremetalPxeServerStateEvent.enable, Enabled),
                new Transaction(BaremetalPxeServerStateEvent.maintain, Maintenance)
        );

        Maintenance.transactions(
                new Transaction(BaremetalPxeServerStateEvent.disable, BaremetalPxeServerState.Disabled),
                new Transaction(BaremetalPxeServerStateEvent.enable, BaremetalPxeServerState.Enabled),
                new Transaction(BaremetalPxeServerStateEvent.maintain, Maintenance)
        );
    }

    private static class Transaction {
        BaremetalPxeServerStateEvent event;
        BaremetalPxeServerState nextState;

        private Transaction(BaremetalPxeServerStateEvent event, BaremetalPxeServerState nextState) {
            this.event = event;
            this.nextState = nextState;
        }
    }

    private void transactions(Transaction... transactions) {
        for (Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    private Map<BaremetalPxeServerStateEvent, Transaction> transactionMap = new HashMap<>();

    public BaremetalPxeServerState nextState(BaremetalPxeServerStateEvent event) {
        Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(
                    String.format("cannot find next state for current state[%s] on transaction event[%s]", this, event)
            );
        }
        return tran.nextState;
    }
}
