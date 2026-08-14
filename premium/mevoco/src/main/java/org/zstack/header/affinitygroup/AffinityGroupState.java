package org.zstack.header.affinitygroup;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2016/12/22.
 */
public enum AffinityGroupState {
    Enabled,
    Disabled;

    static {
        Enabled.transactions(
                new AffinityGroupState.Transaction(AffinityGroupStateEvent.disable, AffinityGroupState.Disabled),
                new AffinityGroupState.Transaction(AffinityGroupStateEvent.enable, AffinityGroupState.Enabled)
        );

        Disabled.transactions(
                new AffinityGroupState.Transaction(AffinityGroupStateEvent.disable, AffinityGroupState.Disabled),
                new AffinityGroupState.Transaction(AffinityGroupStateEvent.enable, AffinityGroupState.Enabled)
        );
    }

    private static class Transaction {
        AffinityGroupStateEvent event;
        AffinityGroupState nextState;

        private Transaction(AffinityGroupStateEvent event, AffinityGroupState nextState) {
            this.event = event;
            this.nextState = nextState;
        }
    }


    private void transactions(AffinityGroupState.Transaction...transactions) {
        for (AffinityGroupState.Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    private Map<AffinityGroupStateEvent, AffinityGroupState.Transaction> transactionMap = new HashMap<AffinityGroupStateEvent, AffinityGroupState.Transaction>();

    public AffinityGroupState nextState(AffinityGroupStateEvent event) {
        AffinityGroupState.Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next state for current state[%s] on transaction event[%s]",
                    this, event));
        }

        return tran.nextState;
    }
}
