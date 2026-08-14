package org.zstack.network.service.eip;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

/**
 */
public enum EipState {
    Enabled,
    Disabled;

    static {
        Enabled.transactions(
                new Transaction(EipStateEvent.disable, EipState.Disabled),
                new Transaction(EipStateEvent.enable, EipState.Enabled)
        );

        Disabled.transactions(
                new Transaction(EipStateEvent.disable, EipState.Disabled),
                new Transaction(EipStateEvent.enable, EipState.Enabled)
        );
    }

    private static class Transaction {
        EipStateEvent event;
        EipState nextState;

        private Transaction(EipStateEvent event, EipState nextState) {
            this.event = event;
            this.nextState = nextState;
        }
    }


    private void transactions(Transaction...transactions) {
        for (Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    private Map<EipStateEvent, Transaction> transactionMap = new HashMap<EipStateEvent, Transaction>();

    public EipState nextState(EipStateEvent event) {
        Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next state for current state[%s] on transaction event[%s]",
                    this, event));
        }

        return tran.nextState;
    }
}
