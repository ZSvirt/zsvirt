package org.zstack.header.storage.snapshot;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

/**
 */
public enum VolumeSnapshotStatus {
    Creating,
    Ready,
    Deleted,
    Deleting;

    static {
        Creating.transactions(
                new Transaction(StatusEvent.ready, VolumeSnapshotStatus.Ready),
                new Transaction(StatusEvent.delete, VolumeSnapshotStatus.Deleting)
        );
        Ready.transactions(
                new Transaction(StatusEvent.ready, VolumeSnapshotStatus.Ready),
                new Transaction(StatusEvent.delete, VolumeSnapshotStatus.Deleting)

        );
        Deleting.transactions(
                new Transaction(StatusEvent.ready, VolumeSnapshotStatus.Ready)
        );
    }

    public enum StatusEvent {
        delete,
        ready,
    }

    private static class Transaction {
        StatusEvent event;
        VolumeSnapshotStatus nextStatus;

        private Transaction(StatusEvent event, VolumeSnapshotStatus nextStatus) {
            this.event = event;
            this.nextStatus = nextStatus;
        }
    }

    private Map<StatusEvent, Transaction> transactionMap = new HashMap<>();

    private void transactions(Transaction... transactions) {
        for (Transaction tran : transactions) {
            transactionMap.put(tran.event, tran);
        }
    }

    public VolumeSnapshotStatus nextState(StatusEvent event) {
        Transaction tran = transactionMap.get(event);
        if (tran == null) {
            throw new CloudRuntimeException(String.format("cannot find next status for current status[%s] on transaction event[%s]",
                    this, event));
        }

        return tran.nextStatus;
    }
}
