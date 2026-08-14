package org.zstack.header.storage.snapshot;

import java.util.function.Supplier;

/**
 * @ Author : yh.w
 * @ Date   : Created in 16:05 2023/8/21
 */
public enum SnapshotMode {
    FULL,
    INCREMENTAL,
    AUTO;

    public static boolean isFullSnapShot(SnapshotMode mode,  Supplier<Boolean> supplier) {
        if (mode == FULL) {
            return true;
        } else if (mode == INCREMENTAL) {
            return false;
        } else {
            return supplier.get();
        }
    }
}
