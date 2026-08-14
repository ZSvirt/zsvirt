package org.zstack.network.service.vip;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

/**
 */
public enum VipState {
    Enabled,
    Disabled;

    static {
        Enabled.transactions(
                new Transaction(VipStateEvent.disable, VipState.Disabled),
                new Transaction(VipStateEvent.enable, VipState.Enabled)
        );

        Disabled.transactions(
                new Transaction(VipStateEvent.disable, VipState.Disabled),
                new Transaction(VipStateEvent.enable, VipState.Enabled)
        );
    }

    private static class Transaction {
        VipStateEvent event;
        VipState nextState;

        private Transaction(VipStateEvent event, VipState nextState) {
            this.event = event;
            this.nextState = nextState;
        }
    }


    private void transactions(Transaction...transactions) {
        for (Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    private Map<VipStateEvent, Transaction> transactionMap = new HashMap<VipStateEvent, Transaction>();

    public VipState nextState(VipStateEvent event) {
        Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next state for current state[%s] on transaction event[%s]",
                    this, event));
        }

        return tran.nextState;
    }
}
