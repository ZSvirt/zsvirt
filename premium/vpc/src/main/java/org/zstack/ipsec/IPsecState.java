package org.zstack.ipsec;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.network.service.eip.EipState;
import org.zstack.network.service.eip.EipStateEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2016/12/22.
 */
public enum  IPsecState {
    Enabled,
    Disabled;

    static {
        Enabled.transactions(
                new IPsecState.Transaction(IPSecStateEvent.disable, IPsecState.Disabled),
                new IPsecState.Transaction(IPSecStateEvent.enable, IPsecState.Enabled)
        );

        Disabled.transactions(
                new IPsecState.Transaction(IPSecStateEvent.disable, IPsecState.Disabled),
                new IPsecState.Transaction(IPSecStateEvent.enable, IPsecState.Enabled)
        );
    }

    private static class Transaction {
        IPSecStateEvent event;
        IPsecState nextState;

        private Transaction(IPSecStateEvent event, IPsecState nextState) {
            this.event = event;
            this.nextState = nextState;
        }
    }


    private void transactions(IPsecState.Transaction...transactions) {
        for (IPsecState.Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    private Map<IPSecStateEvent, IPsecState.Transaction> transactionMap = new HashMap<IPSecStateEvent, IPsecState.Transaction>();

    public IPsecState nextState(IPSecStateEvent event) {
        IPsecState.Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next state for current state[%s] on transaction event[%s]",
                    this, event));
        }

        return tran.nextState;
    }
}
