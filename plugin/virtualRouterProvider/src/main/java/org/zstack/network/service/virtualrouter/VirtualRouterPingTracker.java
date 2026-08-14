package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmStatus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.tacker.PingTracker;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by frank on 6/29/2015.
 */
public class VirtualRouterPingTracker extends PingTracker implements ManagementNodeChangeListener, ManagementNodeReadyExtensionPoint {
    private final static CLogger logger = Utils.getLogger(VirtualRouterPingTracker.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    protected PluginRegistry pluginRgty;

    public String getResourceName() {
        return "virtual router";
    }

    @Override
    public NeedReplyMessage getPingMessage(String resUuid) {
        PingVirtualRouterVmMsg msg = new PingVirtualRouterVmMsg();
        msg.setVirtualRouterVmUuid(resUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, resUuid);
        return msg;
    }

    @Override
    public int getPingInterval() {
        return VirtualRouterGlobalConfig.PING_INTERVAL.value(Integer.class);
    }

    @Override
    public int getParallelismDegree() {
        return VirtualRouterGlobalConfig.PING_PARALLELISM_DEGREE.value(Integer.class);
    }

    private void callExtensionPoints(String resourceUuid, MessageReply reply) {
        for (VirtualRouterTrackerExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterTrackerExtensionPoint.class)) {
            ext.handleTracerReply(resourceUuid, reply);
        }
    }

    @Override
    public void handleReply(final String resourceUuid, MessageReply reply) {
        if (!reply.isSuccess()) {
            logger.warn(String.format("[Virtual Router VM Tracker]: unable to ping the virtual router vm[uuid: %s], %s", resourceUuid, reply.getError()));
            callExtensionPoints(resourceUuid, reply);
            return;
        }

        PingVirtualRouterVmReply pr = reply.castReply();
        callExtensionPoints(resourceUuid, reply);

        if (!pr.isDoReconnect() || pr.isConnected()) {
            return;
        }

        logger.debug(String.format("[Virtual Router VM Tracker]: the virtual router vm[uuid:%s] is detected a reconnect is needed, issuing it...", resourceUuid));
        ReconnectVirtualRouterVmMsg msg = new ReconnectVirtualRouterVmMsg();
        msg.setVirtualRouterVmUuid(resourceUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, resourceUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("[Virtual Router VM Tracker]: failed to reconnect the virtual router vm[uuid:%s], %s", resourceUuid, reply.getError()));
                } else {
                    logger.debug(String.format("[Virtual Router VM Tracker]: successfully reconnect the virtual router vm[uuid:%s]", resourceUuid));
                }
            }
        });
    }

    private void trackOurs() {
        List<String> vrUuids = Q.New(VirtualRouterVmVO.class)
                .select(VirtualRouterVmVO_.uuid)
                .listValues();
        List<String> toTrack = transformAndRemoveNull(vrUuids, arg -> destinationMaker.isManagedByUs(arg) ? arg : null);

        untrackAll();
        track(toTrack);
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        trackOurs();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        trackOurs();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
    }

    @Override
    protected void startHook() {
        VirtualRouterGlobalConfig.PING_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                pingIntervalChanged();
            }
        });
    }

    @Override
    @AsyncThread
    public void managementNodeReady() {
        trackOurs();
    }

    @Override
    protected void trackHook(String resourceUuid) {
        ApplianceVmStatus status = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, resourceUuid).select(VirtualRouterVmVO_.status).findValue();

        if (status == ApplianceVmStatus.Connecting) {
            ReconnectVirtualRouterVmMsg msg = new ReconnectVirtualRouterVmMsg();
            msg.setVirtualRouterVmUuid(resourceUuid);
            msg.setStatusChange(false);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, resourceUuid);
            bus.send(msg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("virtual router[uuid:%s] reconnection failed, because %s", resourceUuid, reply.getError()));
                    } else {
                        logger.debug(String.format("virtual router[uuid:%s] reconnect successfully", resourceUuid));
                    }
                }
            });
        }
    }
}
