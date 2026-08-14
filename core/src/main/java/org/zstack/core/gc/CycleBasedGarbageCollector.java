package org.zstack.core.gc;

import org.zstack.core.db.SQL;

/**
 * Created by mingjian.deng on 2017/10/23.
 */
public abstract class CycleBasedGarbageCollector extends TimeBasedGarbageCollector {
    public CycleBasedGarbageCollector() {
        super();
    }

    @Override
    protected void success() {
        assert uuid != null;
        unlock();

        logger.debug(String.format("[GC] a job[name:%s, id:%s] completes successfully", NAME, uuid));

        SQL.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.uuid, uuid)
                .set(GarbageCollectorVO_.status, GCStatus.Idle).update();

        super.setupTimer();
    }
}
