package org.zstack.header.host;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author : jingwang
 * @create 2023/4/13 10:06 AM
 */
public enum HostPowerStatus {
    POWER_ON,
    POWER_OFF,
    POWER_SHUTDOWN,
    POWER_BOOTING,
    POWER_UNKNOWN,
    UN_CONFIGURED;

    static {
        POWER_ON.transactions(
                new Transaction(HostPowerStatusEvent.on, POWER_ON),
                new Transaction(HostPowerStatusEvent.off, POWER_SHUTDOWN)
        );
        POWER_OFF.transactions(
                new Transaction(HostPowerStatusEvent.on, POWER_BOOTING),
                new Transaction(HostPowerStatusEvent.off, POWER_OFF)
        );
        POWER_SHUTDOWN.transactions(
                new Transaction(HostPowerStatusEvent.off, POWER_SHUTDOWN)
        );
        POWER_BOOTING.transactions(
                new Transaction(HostPowerStatusEvent.on, POWER_BOOTING),
                new Transaction(HostPowerStatusEvent.off, POWER_SHUTDOWN)
        );
    }


    private static class Transaction {
        HostPowerStatusEvent event;
        HostPowerStatus nextStatus;

        private Transaction(HostPowerStatusEvent event, HostPowerStatus nextStatus) {
            this.event = event;
            this.nextStatus = nextStatus;
        }
    }

    private void transactions(HostPowerStatus.Transaction... transactions) {
        for (HostPowerStatus.Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    private Map<HostPowerStatusEvent, HostPowerStatus.Transaction> transactionMap = new HashMap<HostPowerStatusEvent, HostPowerStatus.Transaction>();

    public HostPowerStatus nextStatus(HostPowerStatusEvent event) {
        HostPowerStatus.Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next status for current status[%s] on transaction event[%s]",
                    this, event));
        }

        return tran.nextStatus;
    }
}
