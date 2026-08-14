package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.AsyncTimer;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import javax.persistence.Tuple;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class HostTrackImpl implements HostTracker, ManagementNodeChangeListener, Component, ManagementNodeReadyExtensionPoint {
    private final static CLogger logger = Utils.getLogger(HostTrackImpl.class);

    private Map<String, Tracker> trackers = new ConcurrentHashMap<>();
    private static boolean alwaysStartRightNow = false;

    static class BusyHostItem {
        long toleranceDeadline;
    }

    /**
     * original name: skippedPingHostDeadline
     * see ZSV-8178
     */
    private static final Cache<String, BusyHostItem> busyHosts =
            CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    protected TimeHelper timeHelper;

    private static Map<String, HostReconnectTaskFactory> hostReconnectTaskFactories = new HashMap<>();

    private Map<String, AtomicInteger> hostDisconnectCount = new ConcurrentHashMap<>();

    @Override
    public void managementNodeReady() {
        reScanHost();
    }

    enum ReconnectDecision {
        DoNothing,
        ReconnectNow,
        SubmitReconnectTask,
        StopPing
    }

    private void reconnectNow(String uuid, Completion completion) {
        ReconnectHostMsg msg = new ReconnectHostMsg();
        msg.setHostUuid(uuid);
        msg.setSkipIfHostConnected(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, uuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private class Tracker extends AsyncTimer {
        private final CLogger logger = Utils.getLogger(HostTrackImpl.class);

        private String uuid;
        private String hypervisorType;
        private HostReconnectTask reconnectTask;

        Tracker(String uuid) {
            super(TimeUnit.SECONDS, HostGlobalConfig.PING_HOST_INTERVAL.value(Long.class));
            this.uuid = uuid;
            hypervisorType = Q.New(HostVO.class).select(HostVO_.hypervisorType)
                    .eq(HostVO_.uuid, uuid).findValue();
            if (hypervisorType == null) {
                throw new CloudRuntimeException(String.format("host[uuid:%s] is deleted, why you submit a tracker for it???", uuid));
            }


            __name__ = String.format("host-tracker-%s-hypervisor-%s", uuid, hypervisorType);
        }

        @Override
        protected void execute() {
            track();
        }

        private void track()  {
            Tuple t = Q.New(HostVO.class).select(HostVO_.state, HostVO_.status)
                    .eq(HostVO_.uuid, uuid).findTuple();

            if (t == null) {
                logger.debug(String.format("host[uuid:%s] seems to be deleted, stop tracking it", uuid));
                return;
            }

            HostState state = t.get(0, HostState.class);

            if (state == HostState.PreMaintenance || state == HostState.Maintenance) {
                logger.debug(String.format("host[uuid:%s] is in state of %s, not tracking it this time", uuid, state));
                continueToRunThisTimer();
                return;
            }

            final BusyHostItem busyHostItem = busyHosts.getIfPresent(uuid);
            if (busyHostItem != null && timeHelper.getCurrentTimeMillis() <= busyHostItem.toleranceDeadline) {
                logger.debug(String.format("skip tracking host[uuid:%s] this time, deadline %s", uuid, busyHostItem.toleranceDeadline));
                continueToRunThisTimer();
                return;
            }

            PingHostMsg msg = new PingHostMsg();
            msg.setHostUuid(uuid);
            bus.makeLocalServiceId(msg, HostConstant.SERVICE_ID);
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply reply) {
                    decideWhatToDoNext(makeReconnectDecision(reply));
                }

                private ReconnectDecision makeReconnectDecision(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("[Host Tracker]: unable track host[uuid:%s], %s", uuid, reply.getError()));
                        return ReconnectDecision.DoNothing;
                    }

                    PingHostReply r = reply.castReply();
                    if (r.isNoReconnect()) {
                        return ReconnectDecision.DoNothing;
                    }

                    AtomicInteger disconnectCount = hostDisconnectCount.get(uuid);
                    int threshold = HostGlobalConfig.AUTO_RECONNECT_ON_ERROR_MAX_ATTEMPT_NUM.value(Integer.class);
                    if (threshold > 0 && disconnectCount != null && disconnectCount.get() >= threshold) {
                        logger.warn(String.format("stop pinging host[uuid:%s, hypervisorType:%s] because it fail to reconnect too many times", uuid, hypervisorType));
                        return ReconnectDecision.StopPing;
                    }

                    boolean autoReconnect = HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.value(Boolean.class);
                    if (!r.isConnected() && autoReconnect) {
                        return ReconnectDecision.SubmitReconnectTask;
                    }

                    // host can be successfully pinged
                    if (r.getCurrentHostStatus().equals(HostStatus.Disconnected.toString())) {
                        if (autoReconnect) {
                            return ReconnectDecision.ReconnectNow;
                        } else {
                            logger.warn(String.format("stop pinging host[uuid:%s, hypervisorType:%s] because it's disconnected and connection.autoReconnectOnError is false", uuid, hypervisorType));
                            return ReconnectDecision.StopPing;
                        }
                    }

                    // host can be pinged and the current status is Connected
                    return ReconnectDecision.DoNothing;
                }
            });
        }

        private void decideWhatToDoNext(ReconnectDecision decision) {
            if (decision == ReconnectDecision.DoNothing) {
                continueToRunThisTimer();
            } else if (decision == ReconnectDecision.ReconnectNow) {
                reconnectNow(uuid, new Completion(new NoErrorCompletion() {
                    @Override
                    public void done() {
                        continueToRunThisTimer();
                    }
                }) {
                    @Override
                    public void success() {
                        continueToRunThisTimer();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        submitReconnectTask(errorCode);
                    }
                });
            } else if (decision == ReconnectDecision.StopPing) {
                cancel();
            } else if (decision == ReconnectDecision.SubmitReconnectTask) {
                submitReconnectTask(null);
            } else {
                throw new CloudRuntimeException("should not be here");
            }
        }

        private void submitReconnectTask(ErrorCode lastConnectError) {
            if (isCanceled()) {
                return;
            }

            if (reconnectTask != null) {
                reconnectTask.cancel();
            }

            reconnectTask = createTask(lastConnectError);
            reconnectTask.start();
        }

        HostReconnectTask createTask(ErrorCode lastConnectError) {
            final HostReconnectTaskFactory factory = getHostReconnectTaskFactory(hypervisorType);
            NoErrorCompletion completion = new NoErrorCompletion() {
                @Override
                public void done() {
                    continueToRunThisTimer();
                }
            };

            return lastConnectError == null ?
                    factory.createTask(uuid, completion) :
                    factory.createTaskWithLastConnectError(uuid, lastConnectError, completion);
        }

        @Override
        public void cancel() {
            if (reconnectTask != null) {
                reconnectTask.cancel();
            }

            super.cancel();

            trackers.remove(uuid);
        }
    }


    public void trackHost(String hostUuid) {
        if (!destMaker.isManagedByUs(hostUuid)) {
            logger.debug(String.format("skip tracking host[uuid:%s]: not managed by us", hostUuid));
            return;
        }

        Tracker t = trackers.get(hostUuid);
        if (t != null) {
            t.cancel();
        }

        t = new Tracker(hostUuid);
        trackers.put(hostUuid, t);

        if (CoreGlobalProperty.UNIT_TEST_ON && !alwaysStartRightNow) {
            t.start();
        } else {
            t.startRightNow();
        }

        logger.debug(String.format("starting tracking hosts[uuid:%s]", hostUuid));
    }

    @Override
    public void untrackHost(String huuid) {
        Tracker t = trackers.get(huuid);
        if (t != null) {
            t.cancel();
        }
        trackers.remove(huuid);
        logger.debug(String.format("stop tracking host[uuid:%s]", huuid));
    }

    @Override
    public void trackHost(Collection<String> huuids) {
        huuids.forEach(this::trackHost);
    }

    @Override
    public void untrackHost(Collection<String> huuids) {
        huuids.forEach(this::untrackHost);
    }

    private void reScanHost() {
        reScanHost(false);
    }

    private void reScanHost(boolean skipExisting) {
        if (!skipExisting) {
            new HashSet<>(trackers.values()).forEach(Tracker::cancel);
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                long count = sql("select count(h) from HostVO h", Long.class).find();
                sql("select h.uuid from HostVO h", String.class).limit(1000).paginate(count, (List<String> hostUuids) -> {
                    List<String> byUs = hostUuids.stream().filter(huuid -> {
                        if (skipExisting) {
                            return destMaker.isManagedByUs(huuid) && !trackers.containsKey(huuid);
                        } else {
                            return destMaker.isManagedByUs(huuid);
                        }
                    }).collect(Collectors.toList());

                    trackHost(byUs);
                });
            }
        }.execute();
    }

    @Override
    @AsyncThread
    public void nodeJoin(ManagementNodeInventory inv) {
        reScanHost();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        reScanHost();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {

    }

    private HostReconnectTaskFactory getHostReconnectTaskFactory(String hvType) {
        HostReconnectTaskFactory f = hostReconnectTaskFactories.get(hvType);
        if (f == null) {
            throw new CloudRuntimeException(String.format("cannot find HostReconnectTaskFactory with hypervisorType[%s]", hvType));
        }

        return f;
    }

    @Override
    public boolean start() {
        populateExtensions();
        onHostStatusChange();
        registerHostStatusEvents();

        HostGlobalConfig.PING_HOST_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> {
            logger.debug(String.format("%s change from %s to %s, restart host trackers",
                    oldConfig.getCanonicalName(), oldConfig.value(), newConfig.value()));
            reScanHost();
        });

        HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.installUpdateExtension((oc, nc)-> {
            if (nc.value(Boolean.class)) {
                logger.debug(String.format("%s change from %s to %s, restart host trackers",
                        oc.getCanonicalName(), oc.value(), nc.value()));
                reScanHost(true);
            }
        });

        return true;
    }

    private void populateExtensions() {
        pluginRgty.getExtensionList(HostReconnectTaskFactory.class).forEach(f -> {
            HostReconnectTaskFactory old = hostReconnectTaskFactories.get(f.getHypervisorType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate HostReconnectTaskFactory[%s, %s] with the same type[%s]", f, old, f.getHypervisorType()));
            }

            hostReconnectTaskFactories.put(f.getHypervisorType(), f);
        });
    }

    private void onHostStatusChange() {
        evtf.onLocal(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new EventCallback() {

            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
                if (HostStatus.Connected.toString().equals(d.getNewStatus())) {
                    hostDisconnectCount.remove(d.getHostUuid());
                } else if (HostStatus.Disconnected.toString().equals(d.getNewStatus()) &&
                        HostStatus.Connecting.toString().equals(d.getOldStatus())) {
                    hostDisconnectCount.computeIfAbsent(d.getHostUuid(), key -> new AtomicInteger(0)).addAndGet(1);
                }
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private void registerHostStatusEvents() {
        evtf.on(HostCanonicalEvents.HOST_STATUS_BUSY, new EventCallback<HostCanonicalEvents.HostPingSkipData>() {
            @Override
            protected void run(Map tokens, HostCanonicalEvents.HostPingSkipData data) {
                BusyHostItem item = new BusyHostItem();
                item.toleranceDeadline = data.getBusyUntil();
                busyHosts.put(data.getHostUuid(), item);
            }
        });
        evtf.on(HostCanonicalEvents.HOST_STATUS_RESUME_FROM_BUSY, new EventCallback<HostCanonicalEvents.HostPingSkipData>() {
            @Override
            protected void run(Map tokens, HostCanonicalEvents.HostPingSkipData data) {
                busyHosts.invalidate(data.getHostUuid());
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }
}
