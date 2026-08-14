package org.zstack.utils;

import java.util.HashMap;
import java.util.Map;

public enum State {
    Enabled,
    Disabled;

    static {
        Enabled.transactions(
                new State.Transaction(StateEvent.disable, State.Disabled),
                new State.Transaction(StateEvent.enable, State.Enabled)
        );

        Disabled.transactions(
                new State.Transaction(StateEvent.disable, State.Disabled),
                new State.Transaction(StateEvent.enable, State.Enabled)
        );
    }

    private static class Transaction {
        StateEvent event;
        State nextState;

        private Transaction(StateEvent event, State nextState) {
            this.event = event;
            this.nextState = nextState;
        }
    }


    private void transactions(State.Transaction...transactions) {
        for (State.Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    private Map<StateEvent, State.Transaction> transactionMap = new HashMap<StateEvent, State.Transaction>();

    public State nextState(StateEvent event) {
        State.Transaction tran = transactionMap.get(event);
        return tran.nextState;
    }

}
