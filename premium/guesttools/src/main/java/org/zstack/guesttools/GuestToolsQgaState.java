package org.zstack.guesttools;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

public enum GuestToolsQgaState {
    NotInstalled(),
    Running(),
    NotRunning(),
    NotUpgraded();

    private static class Transaction {
        GuestToolsQgaStateEvent event;
        GuestToolsQgaState nextState;

        private Transaction(GuestToolsQgaStateEvent event, GuestToolsQgaState nextState) {
            this.event = event;
            this.nextState = nextState;
        }
    }

    GuestToolsQgaState() {
    }

    private final Map<GuestToolsQgaStateEvent, GuestToolsQgaState.Transaction> transactionMap = new HashMap<GuestToolsQgaStateEvent, GuestToolsQgaState.Transaction>();

    static {
        NotInstalled.transactions(
                /* NotInstalled->NotUpgraded only works for guest os with qga */
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.zwatchRunning, GuestToolsQgaState.NotUpgraded),

                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.qgaNotRunning, GuestToolsQgaState.NotInstalled),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.zsToolNotFound, GuestToolsQgaState.NotInstalled),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.zsToolFound, GuestToolsQgaState.Running),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.noOperation, GuestToolsQgaState.NotInstalled)
        );
        Running.transactions(
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.zwatchRunning, GuestToolsQgaState.Running),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.qgaNotRunning, GuestToolsQgaState.NotRunning),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.zsToolNotFound, GuestToolsQgaState.NotInstalled),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.zsToolFound, GuestToolsQgaState.Running),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.noOperation, GuestToolsQgaState.Running)
        );
        NotRunning.transactions(
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.zwatchRunning, GuestToolsQgaState.NotRunning),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.qgaNotRunning, GuestToolsQgaState.NotRunning),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.zsToolNotFound, GuestToolsQgaState.NotInstalled),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.zsToolFound, GuestToolsQgaState.Running),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.noOperation, GuestToolsQgaState.NotRunning)
        );
        NotUpgraded.transactions(
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.zwatchRunning, GuestToolsQgaState.NotUpgraded),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.qgaNotRunning, GuestToolsQgaState.NotUpgraded),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.zsToolNotFound, GuestToolsQgaState.NotUpgraded),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.zsToolFound, GuestToolsQgaState.Running),
                new GuestToolsQgaState.Transaction(GuestToolsQgaStateEvent.noOperation, GuestToolsQgaState.NotUpgraded)
        );
    }

    private void transactions(GuestToolsQgaState.Transaction... transactions) {
        for (GuestToolsQgaState.Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    public GuestToolsQgaState nextState(GuestToolsQgaStateEvent event) {
        GuestToolsQgaState.Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next state for current state[%s] on transaction event[%s]",
                    this, event));
        }

        return tran.nextState;
    }
}
