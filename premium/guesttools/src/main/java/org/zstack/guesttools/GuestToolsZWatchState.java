package org.zstack.guesttools;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

public enum GuestToolsZWatchState {
    NotInstalled(),
    Running(),
    NotRunning();

    private static class Transaction {
        GuestToolsZWatchStateEvent event;
        GuestToolsZWatchState nextState;

        private Transaction(GuestToolsZWatchStateEvent event, GuestToolsZWatchState nextState) {
            this.event = event;
            this.nextState = nextState;
        }
    }

    GuestToolsZWatchState() {
    }

    private final Map<GuestToolsZWatchStateEvent, GuestToolsZWatchState.Transaction> transactionMap = new HashMap<GuestToolsZWatchStateEvent, GuestToolsZWatchState.Transaction>();

    static {
        NotInstalled.transactions(
                new GuestToolsZWatchState.Transaction(GuestToolsZWatchStateEvent.installFinished, GuestToolsZWatchState.Running),
                new GuestToolsZWatchState.Transaction(GuestToolsZWatchStateEvent.started, GuestToolsZWatchState.Running),
                new GuestToolsZWatchState.Transaction(GuestToolsZWatchStateEvent.stopped, GuestToolsZWatchState.NotInstalled),
                new GuestToolsZWatchState.Transaction(GuestToolsZWatchStateEvent.noOperation, GuestToolsZWatchState.NotInstalled)
        );
        Running.transactions(
                new GuestToolsZWatchState.Transaction(GuestToolsZWatchStateEvent.installFinished, GuestToolsZWatchState.Running),
                new GuestToolsZWatchState.Transaction(GuestToolsZWatchStateEvent.started, GuestToolsZWatchState.Running),
                new GuestToolsZWatchState.Transaction(GuestToolsZWatchStateEvent.stopped, GuestToolsZWatchState.NotRunning),
                new GuestToolsZWatchState.Transaction(GuestToolsZWatchStateEvent.noOperation, GuestToolsZWatchState.Running)
        );
        NotRunning.transactions(
                new GuestToolsZWatchState.Transaction(GuestToolsZWatchStateEvent.installFinished, GuestToolsZWatchState.NotRunning),
                new GuestToolsZWatchState.Transaction(GuestToolsZWatchStateEvent.started, GuestToolsZWatchState.Running),
                new GuestToolsZWatchState.Transaction(GuestToolsZWatchStateEvent.stopped, GuestToolsZWatchState.NotRunning),
                new GuestToolsZWatchState.Transaction(GuestToolsZWatchStateEvent.noOperation, GuestToolsZWatchState.NotRunning)
        );
    }

    private void transactions(GuestToolsZWatchState.Transaction... transactions) {
        for (GuestToolsZWatchState.Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    public GuestToolsZWatchState nextState(GuestToolsZWatchStateEvent event) {
        GuestToolsZWatchState.Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next state for current state[%s] on transaction event[%s]",
                    this, event));
        }

        return tran.nextState;
    }
}
