package org.zstack.header.host;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

public enum HostState {
    Enabled,
    Disabled,
    PreMaintenance,
    Maintenance;

    static {
        Enabled.transactions(
                new Transaction(HostStateEvent.disable, HostState.Disabled),
                new Transaction(HostStateEvent.enable, HostState.Enabled),
                new Transaction(HostStateEvent.preMaintain, HostState.PreMaintenance)
        );

        Disabled.transactions(
                new Transaction(HostStateEvent.disable, HostState.Disabled),
                new Transaction(HostStateEvent.enable, HostState.Enabled),
                new Transaction(HostStateEvent.preMaintain, HostState.PreMaintenance)
        );

        PreMaintenance.transactions(
                new Transaction(HostStateEvent.disable, HostState.Disabled),
                new Transaction(HostStateEvent.enable, HostState.Enabled),
                new Transaction(HostStateEvent.maintain, HostState.Maintenance),
                new Transaction(HostStateEvent.preMaintain, HostState.PreMaintenance)
        );

        Maintenance.transactions(
                new Transaction(HostStateEvent.disable, HostState.Disabled),
                new Transaction(HostStateEvent.enable, HostState.Enabled)
        );
    }

    private static class Transaction {
        HostStateEvent event;
        HostState nextState;

        private Transaction(HostStateEvent event, HostState nextState) {
            this.event = event;
            this.nextState = nextState;
        }
    }

    private void transactions(Transaction... transactions) {
        for (Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    private Map<HostStateEvent, Transaction> transactionMap = new HashMap<HostStateEvent, Transaction>();

    public HostState nextState(HostStateEvent event) {
        Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next state for current state[%s] on transaction event[%s]",
                    this, event));
        }

        return tran.nextState;
    }

    public HostStateEvent getTargetStateDrivenEvent(HostState targetState) {
        return transactionMap.values().stream().filter(it -> it.nextState == targetState).findFirst().map(it -> it.event).orElse(null);
    }
}
