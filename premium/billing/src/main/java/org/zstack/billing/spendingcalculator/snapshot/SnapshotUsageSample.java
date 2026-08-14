package org.zstack.billing.spendingcalculator.snapshot;

import org.zstack.billing.UsageSample;

/**
 * Created by camile on 2017/5/18.
 */
public class SnapshotUsageSample extends UsageSample {
    protected String snapshotUuid;
    protected String snapshotName;

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    public void setSnapshotName(String snapshotName) {
        this.snapshotName = snapshotName;
    }
}
