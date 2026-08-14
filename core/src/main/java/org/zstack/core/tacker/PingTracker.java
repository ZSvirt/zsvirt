package org.zstack.core.tacker;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusSteppingCallback;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 */
public abstract class PingTracker implements Component {
    public abstract String getResourceName();
    public abstract NeedReplyMessage getPingMessage(String resUuid);
    public abstract int getPingInterval();
    public abstract int getParallelismDegree();
    public abstract void handleReply(String resourceUuid, MessageReply reply);

    private final static CLogger logger = Utils.getLogger(PingTracker.class);

    private final List<String> resourceUuids = Collections.synchronizedList(new ArrayList<String>());
    private final Set<String> resourceInTracking = Collections.synchronizedSet(new HashSet<String>());
    private Future<Void> trackerThread = null;

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ThreadFacade thdf;

    private class Tracker implements PeriodicTask {
        @Override
        public TimeUnit getTimeUnit() {
            return TimeUnit.SECONDS;
        }

        @Override
        public long getInterval() {
            return getPingInterval();
        }

        @Override
        public String getName() {
            return String.format("pingTracker-for-%s-managementNode-%s", getResourceName(), Platform.getManagementServerId());
        }

        @Override
        public void run() {
            try {
                final List<NeedReplyMessage> msgs = new ArrayList<NeedReplyMessage>();
                synchronized (resourceUuids) {
                    final Map<NeedReplyMessage, String> tmp = new HashMap<NeedReplyMessage, String>();

                    for (String resUuid : resourceUuids) {
                        if (resourceInTracking.contains(resUuid)) {
                            continue;
                        }

                        NeedReplyMessage msg = getPingMessage(resUuid);
                        msgs.add(msg);
                        resourceInTracking.add(resUuid);
                        tmp.put(msg, resUuid);
                    }

                    if (msgs.isEmpty()) {
                        return;
                    }

                    bus.send(msgs, getParallelismDegree(), new CloudBusSteppingCallback(null) {
                        @Override
                        public void run(NeedReplyMessage msg, MessageReply reply) {
                            String resUuid = tmp.get(msg);
                            DebugUtils.Assert(resUuid!=null, "where is my resource uuid???");
                            try {
                                handleReply(resUuid, reply);
                            } finally {
                                resourceInTracking.remove(resUuid);
                            }
                        }
                    });
                }
            } catch (Throwable t) {
                logger.warn("unhandled throwable", t);
            }
        }
    }

    protected void trackHook(String resourceUuid) {
    }
    
    protected void untrackHook(String resourceUuid) {
    }

    protected void startHook() {
    }

    protected void pingIntervalChanged() {
        startTracker();
    }
    
    public void track(String resUuid) {
        synchronized (resourceUuids) {
            if (!resourceUuids.contains(resUuid)) {
                resourceUuids.add(resUuid);
                trackHook(resUuid);
                logger.debug(String.format("start tracking %s[uuid:%s]", getResourceName(), resUuid));
            }
        }
    }

    public void untrackAll() {
        synchronized (resourceUuids) {
            resourceUuids.clear();
            logger.debug(String.format("untrack all %s", getResourceName()));
        }
    }

    public void untrack(String resUuid) {
        synchronized (resourceUuids) {
            resourceUuids.remove(resUuid);
            untrackHook(resUuid);
            logger.debug(String.format("stop tracking %s[uuid:%s]", getResourceName(), resUuid));
        }
    }

    public void track(Collection<String> resUuids) {
        synchronized (resourceUuids) {
            for (String resUuid : resUuids) {
                if (!resourceUuids.contains(resUuid)) {
                    resourceUuids.add(resUuid);
                    trackHook(resUuid);
                    logger.debug(String.format("start tracking %s[uuid:%s]", getResourceName(), resUuid));
                }
            }
        }
    }

    public void untrack(Collection<String> resUuids) {
        synchronized (resourceUuids) {
            for (String resUuid : resUuids) {
                resourceUuids.remove(resUuid);
                untrackHook(resUuid);
                logger.debug(String.format("stop tracking %s[uuid:%s]", getResourceName(), resUuid));
            }
        }
    }

    protected synchronized void startTracker() {
        if (trackerThread != null) {
            trackerThread.cancel(true);
        }

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            trackerThread = thdf.submitPeriodicTask(new Tracker(), getPingInterval());
        } else {
            trackerThread = thdf.submitPeriodicTask(new Tracker(), (long) getPingInterval() + new Random().nextInt(30));
        }
    }

    @Override
    public boolean start() {
        startTracker();
        startHook();
        return true;
    }

    @Override
    public boolean stop() {
        trackerThread.cancel(true);
        return true;
    }
}
