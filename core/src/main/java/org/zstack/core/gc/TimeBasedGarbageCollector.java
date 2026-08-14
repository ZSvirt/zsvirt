package org.zstack.core.gc;

import org.zstack.header.errorcode.ErrorCode;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2017/3/4.
 */
public abstract class TimeBasedGarbageCollector extends GarbageCollector {
    @GC
    public volatile Long NEXT_TIME;
    @GC
    public volatile TimeUnit NEXT_TIME_UNIT;

    private TimerTask currentTimer;

    public TimeBasedGarbageCollector() {
        canceller = this::destroy;
    }

    protected void setupTimer() {
        logger.debug(String.format("[GC] schedule a GC job[name:%s, id:%s] to run after %s %s",
                NAME, uuid, NEXT_TIME, NEXT_TIME_UNIT));

        currentTimer = new TimerTask() {
            @Override
            public void run() {
                runTrigger();
            }
        };

        new Timer().schedule(currentTimer, NEXT_TIME_UNIT.toMillis(NEXT_TIME));
    }

    @Override
    protected void cancel() {
        if (currentTimer != null)  {
            currentTimer.cancel();
        }

        super.cancel();
    }

    @Override
    protected void fail(ErrorCode err) {
        super.fail(err);
        setupTimer();
    }

    public void load(GarbageCollectorVO vo) {
        loadFromVO(vo);
        prepare();
        setupTimer();
    }

    public final void submit(Long next, TimeUnit unit) {
        NEXT_TIME_UNIT = unit;
        NEXT_TIME = next;

        saveToDb();
        prepare();
        setupTimer();

        gcMgr.registerGC(this);
    }

    public void deduplicateSubmit(Long next, TimeUnit unit) {
        if (existedAndNotCompleted()) {
            return;
        }
        submit(next, unit);
    }

    protected void prepare() {}
    protected void destroy() {}
}
