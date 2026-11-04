package org.zstack.ha.spi;

import org.zstack.header.core.HaCompletion;
import org.zstack.header.errorcode.ErrorCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by xing5 on 2016/3/28.
 */
public interface HaHypervisorWorker {
    void start(HaCompletion completion);

    /**
     * HostCheckResult
     */
    class Context {
        // config
        public volatile boolean onlyCheckAlive = false;

        // results
        public final List<ContextItem> items = new ArrayList<>();

        public final transient Object lock = new Object();
        public boolean started = false;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(1024);

            for (ContextItem item : items) {
                builder.append("\t");
                synchronized (item.lock) {
                    builder.append(item.checkerName);

                    if (item.storageType != null) {
                        builder.append(" (").append(item.storageType).append(") ");
                    }

                    builder.append(": [weight=").append(item.weight).append("] ").append(item.alive);
                    if (item.error != null) {
                        builder.append(" ").append(item.error.getDetails());
                    }

                    if (!item.aliveVmUuidList.isEmpty()) {
                        builder.append("\n\t\t").append("alive VM: ");
                        builder.append(String.join(", ", item.aliveVmUuidList));
                    }
                }
                builder.append("\n");
            }

            return builder.toString();
        }
    }

    /**
     * HostScanResult
     * Result of host alive scan used by HA checkers and host-scan extension points.
     */
    class ContextItem {
        public String checkerName;
        public ErrorCode error;
        public AliveLevel alive = AliveLevel.NotKnow;
        public String evidence;
        public int weight = 1;

        public String storageType;
        public final List<String> aliveVmUuidList = new ArrayList<>(); // empty if storageType is null

        public final transient Object lock = new Object();
    }

    enum AliveLevel {
        MustAlive,
        Alive,
        MayDied, // maybe host alive, but the checker can not access
        NotKnow,
    }

    class Report {
        public String hostUuid;
        public AliveLevel alive;
        public String evidence;

        public int weightTotal;
        public int aliveWeightTotal;

        public final HashSet<String> aliveVmUuids = new HashSet<>();
    }
}
