package org.zstack.header.baremetal.chassis;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by GuoYi on 7/6/18.
 */
public enum BaremetalChassisState {
    Enabled,
    Disabled;

    static {
        Enabled.transactions(
                new Transaction(BaremetalChassisStateEvent.disable, Disabled),
                new Transaction(BaremetalChassisStateEvent.enable, Enabled)
        );

        Disabled.transactions(
                new Transaction(BaremetalChassisStateEvent.disable, Disabled),
                new Transaction(BaremetalChassisStateEvent.enable, Enabled)
        );
    }

    private static class Transaction {
        BaremetalChassisStateEvent event;
        BaremetalChassisState nextState;

        private Transaction(BaremetalChassisStateEvent event, BaremetalChassisState nextState) {
            this.event = event;
            this.nextState = nextState;
        }
    }

    private void transactions(Transaction... transactions) {
        for (Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    private Map<BaremetalChassisStateEvent, Transaction> transactionMap = new HashMap<>();

    public BaremetalChassisState nextState(BaremetalChassisStateEvent event) {
        Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(
                    String.format("cannot find next state for current state[%s] on transaction event[%s]", this, event)
            );
        }
        return tran.nextState;
    }
}
