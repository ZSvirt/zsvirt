package org.zstack.vpc.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The VirtualRouterPingFailureTracker will monitor the ping heart beat
 * and remove the failed virtual router from regular PingTracker.
 */
public class VirtualRouterPingFailureTracker implements ManagementNodeReadyExtensionPoint,
        VirtualRouterTrackerExtensionPoint {
    private final static CLogger logger = Utils.getLogger(VirtualRouterPingFailureTracker.class);

    @Autowired
    private VirtualRouterPingTracker tracker;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private final ConcurrentHashMap<String, String> failed = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> recovering = new ConcurrentHashMap<>();

    private String getVrMgmtIp(String vrUuid) {
        return SQL.New("select nic.ip from ApplianceVmVO vm, VmNicVO nic " +
                "where vm.uuid = :uuid " +
                "and nic.vmInstanceUuid = vm.uuid " +
                "and nic.l3NetworkUuid = vm.managementNetworkUuid", String.class)
                .param("uuid", vrUuid)
                .find();
    }

    public String notifyFailure(String resourceUuid) {
        return failed.computeIfAbsent(resourceUuid, this::getVrMgmtIp);
    }

    public boolean inTracking(String resourceUuid) {
        return failed.containsKey(resourceUuid);
    }

    @Override
    public void handleTracerReply(final String resourceUuid, MessageReply reply) {
        if (!reply.isSuccess()) {
            notifyFailure(resourceUuid);
            tracker.untrack(resourceUuid);
        }
    }

    @Override
    public void managementNodeReady() {
        thdf.submitPeriodicTask(new PeriodicTask() {
            final AtomicInteger cnt = new AtomicInteger(0);
            final AtomicBoolean skipReconnect = new AtomicBoolean(false);

            private void doRecoverFlow(String resUuid) {
                SimpleFlowChain chain = new SimpleFlowChain();
                chain.setName("do-recover-vr-" + resUuid);
                chain.then(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        logger.info("re-provisioning vr: " + resUuid);
                        ProvisionVirtualRouterConfigMsg pmsg = new ProvisionVirtualRouterConfigMsg();
                        pmsg.setVirtualRouterVmUuid(resUuid);
                        bus.makeTargetServiceIdByResourceUuid(pmsg, VmInstanceConstant.SERVICE_ID, resUuid);
                        bus.send(pmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    skipReconnect.set(true);
                                }
                                trigger.next();
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (skipReconnect.get()) {
                            trigger.next();
                            return;
                        }

                        logger.info("reconnecting vr: " + resUuid);
                        ReconnectVirtualRouterVmMsg msg = new ReconnectVirtualRouterVmMsg();
                        msg.setVirtualRouterVmUuid(resUuid);
                        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, resUuid);
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess() && dbf.isExist(resUuid, VirtualRouterVmVO.class)) {
                                    notifyFailure(resUuid);
                                }
                                trigger.next();
                            }
                        });
                    }
                }).done(new FlowDoneHandler(null) {
                    @Override
                    public void handle(Map data) {
                        recovering.remove(resUuid);
                        tracker.track(resUuid);
                    }
                }).error(new FlowErrorHandler(null) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        recovering.remove(resUuid);
                    }
                }).start();
            }

            private void recoverVR(String resUuid) {
                if (null != recovering.putIfAbsent(resUuid, Boolean.TRUE)) {
                    return;
                }

                // In case of failure during recovering
                failed.remove(resUuid);

                doRecoverFlow(resUuid);
            }

            private void checkReachable() {
                for (Map.Entry<String, String> entry: failed.entrySet()) {
                    final String resUuid = entry.getKey();
/*
                    if (!destinationMaker.isManagedByUs(resUuid)) {
                        logger.info(String.format("skipped vr %s", resUuid));
                        failed.remove(resUuid);
                        continue;
                    }
*/
                    ShellResult ret = ShellUtils.runAndReturn(String.format("ping -c 1 -W 1 %s", entry.getValue()));
                    if (ret.isReturnCode(0) || NetworkUtils.isReachable(entry.getValue(), 100)) {
                        recoverVR(resUuid);
                    }
                }
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.MILLISECONDS;
            }

            @Override
            public long getInterval() {
                return 200;
            }

            @Override
            public String getName() {
                return "tracking-vr-ping-failure";
            }

            @Override
            public void run() {
                if (cnt.addAndGet(1) > 1) {
                    return;
                }

                try {
                    checkReachable();
                } catch (Exception ignored) {
                } finally {
                    cnt.set(0);
                }
            }
        });
        logger.info("started vrouter failure checker");
    }
}
