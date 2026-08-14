package org.zstack.header.longjob;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.NoJsonSchema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by GuoYi on 11/13/17.
 */
public enum LongJobState {
    Waiting,
    Suspended,
    Running,
    Succeeded,
    Canceling,
    Canceled,
    Failed;

    static {
        Waiting.transactions(
                new Transaction(LongJobStateEvent.start, LongJobState.Running),
                new Transaction(LongJobStateEvent.fail, LongJobState.Failed),
                new Transaction(LongJobStateEvent.canceling, LongJobState.Canceled),
                new Transaction(LongJobStateEvent.canceled, LongJobState.Canceled)
        );

        Suspended.transactions(
                new Transaction(LongJobStateEvent.resume, LongJobState.Running),
                new Transaction(LongJobStateEvent.fail, LongJobState.Failed),
                new Transaction(LongJobStateEvent.suspend, LongJobState.Suspended),
                new Transaction(LongJobStateEvent.canceling, LongJobState.Canceling),
                new Transaction(LongJobStateEvent.canceled, LongJobState.Canceled)
        );

        Running.transactions(
                new Transaction(LongJobStateEvent.succeed, LongJobState.Succeeded),
                new Transaction(LongJobStateEvent.fail, LongJobState.Failed),
                new Transaction(LongJobStateEvent.resume, LongJobState.Running),
                new Transaction(LongJobStateEvent.suspend, LongJobState.Suspended),
                new Transaction(LongJobStateEvent.canceling, LongJobState.Canceling),
                new Transaction(LongJobStateEvent.canceled, LongJobState.Canceled)
        );

        Canceling.transactions(
                new Transaction(LongJobStateEvent.canceling, LongJobState.Canceling),
                new Transaction(LongJobStateEvent.canceled, LongJobState.Canceled),
                new Transaction(LongJobStateEvent.suspend, LongJobState.Canceling),
                new Transaction(LongJobStateEvent.fail, LongJobState.Canceled),
                new Transaction(LongJobStateEvent.succeed, LongJobState.Succeeded)
        );
    }

    public static List<LongJobState> finalStates = Arrays.asList(Succeeded, Failed, Canceled);

    private static class Transaction {
        LongJobStateEvent event;
        LongJobState nextState;

        private Transaction(LongJobStateEvent event, LongJobState nextState) {
            this.event = event;
            this.nextState = nextState;
        }
    }

    private void transactions(Transaction... transactions) {
        for (Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    @NoJsonSchema
    private Map<LongJobStateEvent, Transaction> transactionMap = new HashMap<>();

    public LongJobState nextState(LongJobStateEvent event) {
        if (transactionMap.isEmpty()) {
            // final state will not changed again.
            return this;
        }

        Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next state for current state[%s] on transaction event[%s]",
                    this, event));
        }

        return tran.nextState;
    }
}
