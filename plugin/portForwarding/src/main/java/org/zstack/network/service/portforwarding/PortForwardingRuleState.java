package org.zstack.network.service.portforwarding;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

/**
 */
public enum  PortForwardingRuleState {
    Enabled,
    Disabled;

    static {
        Enabled.transactions(
                new Transaction(PortForwardingRuleStateEvent.disable, PortForwardingRuleState.Disabled),
                new Transaction(PortForwardingRuleStateEvent.enable, PortForwardingRuleState.Enabled)
        );

        Disabled.transactions(
                new Transaction(PortForwardingRuleStateEvent.disable, PortForwardingRuleState.Disabled),
                new Transaction(PortForwardingRuleStateEvent.enable, PortForwardingRuleState.Enabled)
        );
    }

    private static class Transaction {
        PortForwardingRuleStateEvent event;
        PortForwardingRuleState nextState;

        private Transaction(PortForwardingRuleStateEvent event, PortForwardingRuleState nextState) {
            this.event = event;
            this.nextState = nextState;
        }
    }


    private void transactions(Transaction...transactions) {
        for (Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    private Map<PortForwardingRuleStateEvent, Transaction> transactionMap = new HashMap<PortForwardingRuleStateEvent, Transaction>();

    public PortForwardingRuleState nextState(PortForwardingRuleStateEvent event) {
        Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next state for current state[%s] on transaction event[%s]",
                    this, event));
        }

        return tran.nextState;
    }
}
