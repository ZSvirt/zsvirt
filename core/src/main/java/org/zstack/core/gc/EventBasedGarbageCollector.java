package org.zstack.core.gc;

import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.thread.AsyncThread;
import org.zstack.utils.DebugUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2017/3/3.
 */
public abstract class EventBasedGarbageCollector extends GarbageCollector {
    public void load(GarbageCollectorVO vo) {
        loadFromVO(vo);
        setup();
        installTriggers();
        logger.debug(String.format("[GC] loaded a job[name:%s, id:%s]", NAME, uuid));

        if (!triggerAfterLoad()) {
            return;
        }

        if (!lock()) {
            logger.debug(String.format("[GC] the job[name:%s, id:%s] is being executed by another trigger," +
                    "skip management node to excuete", NAME, uuid));
            return;
        }

        logger.debug(String.format("[GC] the job[name:%s, id:%s] is triggered by management node", NAME, uuid));
        runTrigger();
    }

    protected boolean triggerAfterLoad() {
        return true;
    }

    private Map<String, EventCallback> eventCallbacks = new HashMap<>();

    protected void onEvent(String path, Trigger c) {
        eventCallbacks.put(path, new EventCallback() {
            @Override
            @AsyncThread
            protected void run(Map tokens, Object data) {
                GarbageCollectorVO vo = dbf.findByUuid(uuid, GarbageCollectorVO.class);

                if (vo == null) {
                    logger.warn(String.format("[GC] cannot find a job[name:%s, id:%s], assume it's deleted", NAME, uuid));
                    cancel();
                    return;
                }

                if (!c.trigger(tokens, data)) {
                    // don't trigger it
                    return;
                }

                if (!lock()) {
                    logger.debug(String.format("[GC] the job[name:%s, id:%s] is being executed by another trigger," +
                            "skip this event[%s]", NAME, uuid, path));
                    return;
                }

                logger.debug(String.format("[GC] the job[name:%s, id:%s] is triggered by an event[%s]", NAME, uuid, path));

                vo.setStatus(GCStatus.Processing);
                dbf.update(vo);
                runTrigger();
            }
        });
    }

    protected abstract void setup();

    private void installTriggers() {
        setup();
        DebugUtils.Assert(!eventCallbacks.isEmpty(), String.format("%s[%s] doesn't call onEvent() in the setup() to install any triggers",
                NAME, getClass()));

        for (Map.Entry<String, EventCallback> e : eventCallbacks.entrySet()) {
            evtf.on(e.getKey(), e.getValue());
        }


        canceller = () -> eventCallbacks.values().forEach((it) -> evtf.off(it));
    }

    public final void submit() {
        saveToDb();
        installTriggers();

        gcMgr.registerGC(this);
    }

    protected void deduplicateSubmit() {
        if (existedAndNotCompleted()) {
            return;
        }
        submit();
    }
}
