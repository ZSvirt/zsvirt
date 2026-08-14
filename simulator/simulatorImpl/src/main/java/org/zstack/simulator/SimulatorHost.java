package org.zstack.simulator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.host.HostBase;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.CheckNetworkPhysicalInterfaceMsg;
import org.zstack.header.network.l2.CheckNetworkPhysicalInterfaceReply;
import org.zstack.header.simulator.*;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
class SimulatorHost extends HostBase {
    private static final CLogger logger = Utils.getLogger(SimulatorHost.class);
    private SimulatorConnection conn;
    private volatile boolean isDisconnected;

    @Autowired
    private PluginRegistry pluginRegty;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private SimulatorConfig config;


    protected SimulatorHost(SimulatorHostVO self) {
        super(self);
        conn = new SimulatorConnectionImpl(this);
    }

    @Override
    public void changeStateHook(HostState current, HostStateEvent stateEvent, HostState next) {
        logger.debug(String.format("Host: %s changed state from %s to %s by %s", self.getName(), current, next, stateEvent));
    }

    @Override
    public void deleteHook() {
        logger.debug(String.format("Host: %s is being deleted", self.getName()));
    }

    @Override
    public void connectHook(ConnectHostInfo info, Completion complete) {
        for (SimulatorConnectExtensionPoint sc : pluginRegty.getExtensionList(SimulatorConnectExtensionPoint.class)) {
            String err = sc.connect(conn);
            if (err != null) {
                logger.warn(err);
                complete.fail(operr(err));
                return;
            }
        }

        logger.debug(String.format("Host: %s is connected", self.getName()));
        complete.success();
    }

    @Override
    protected void updateOsHook(UpdateHostOSMsg msg, Completion completion) {
        logger.debug("update operating system of host " + self.getUuid());
    }

    @Override
    public void maintenanceHook(ChangeHostStateMsg msg ,Completion completion) {
        logger.debug(String.format("Host: %s entered maintenance mode", self.getName()));
        completion.success();
    }

    SimulatorHostVO getSimulatorHostVO() {
        return (SimulatorHostVO) self;
    }

    @Override
    protected void pingHook(Completion completion) {
        if (!isDisconnected) {
            completion.success();
        } else {
            completion.fail(operr("set to disconnected"));
        }
    }

    @Override
    protected void deleteTakeOverFlag(Completion completion) {
        logger.debug("takeOverFlag deleted successfully");
        completion.success();
    }

    @Override
    protected int getVmMigrateQuantity() {
        return 1;
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        super.handleApiMessage(msg);
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof CheckNetworkPhysicalInterfaceMsg) {
            handle((CheckNetworkPhysicalInterfaceMsg) msg);
        } else if (msg instanceof SimulatorHostConnectionControlMsg) {
            handle((SimulatorHostConnectionControlMsg) msg);
        } else if (msg instanceof ChangeVmStateOnSimulatorHostMsg) {
            handle((ChangeVmStateOnSimulatorHostMsg) msg);
        } else if (msg instanceof RemoveVmOnSimulatorMsg) {
            handle((RemoveVmOnSimulatorMsg) msg);
        } else if (msg instanceof TakeSnapshotOnHypervisorMsg) {
            handle((TakeSnapshotOnHypervisorMsg) msg);
        } else if (msg instanceof DetachNicFromVmOnHypervisorMsg) {
            handle((DetachNicFromVmOnHypervisorMsg) msg);
        } else if (msg instanceof VmAttachNicOnHypervisorMsg) {
            handle((VmAttachNicOnHypervisorMsg) msg);
        } else if (msg instanceof CreateVmOnHypervisorMsg) {
            handle((CreateVmOnHypervisorMsg) msg);
        } else if (msg instanceof StopVmOnHypervisorMsg) {
            handle((StopVmOnHypervisorMsg) msg);
        } else if (msg instanceof RebootVmOnHypervisorMsg) {
            handle((RebootVmOnHypervisorMsg) msg);
        } else if (msg instanceof StartVmOnHypervisorMsg) {
            handle((StartVmOnHypervisorMsg) msg);
        } else if (msg instanceof MigrateVmOnHypervisorMsg) {
            handle((MigrateVmOnHypervisorMsg) msg);
        } else if (msg instanceof AttachVolumeToVmOnHypervisorMsg) {
            handle((AttachVolumeToVmOnHypervisorMsg) msg);
        } else if (msg instanceof DetachVolumeFromVmOnHypervisorMsg) {
            handle((DetachVolumeFromVmOnHypervisorMsg) msg);
        } else if (msg instanceof DestroyVmOnHypervisorMsg) {
            handle((DestroyVmOnHypervisorMsg) msg);
        } else if (msg instanceof UpdateVmOnHypervisorMsg) {
            handle((UpdateVmOnHypervisorMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(UpdateVmOnHypervisorMsg msg) {
        UpdateVmOnHypervisorReply reply = new UpdateVmOnHypervisorReply();

        if (msg.getSpec().isCpuChanged()) {
            reply.setCpuUpdatedTo(msg.getSpec().getCpuNum());
        }
        if (msg.getSpec().isMemoryChanged()) {
            reply.setMemoryUpdatedTo(msg.getSpec().getMemorySize());
        }
        if (msg.getSpec().isReservedMemoryChanged()) {
            logger.info(String.format("%s ignored reserved memory change request", getClass().getSimpleName()));
        }

        bus.reply(msg, reply);
    }

    private void handle(MigrateVmOnHypervisorMsg msg) {
        MigrateVmOnHypervisorReply reply = new MigrateVmOnHypervisorReply();
        if (!config.migrateSuccess) {
            reply.setError(operr("on purpose"));
        } else {
            logger.debug(String.format("Successfully migrate vm[uuid:%s] on simulator host[uuid:%s] to host[uuid:%s]", msg.getVmInventory().getUuid(), self.getUuid(), msg.getDestHostInventory().getUuid()));
            config.removeVm(msg.getSrcHostUuid(), msg.getVmInventory().getUuid());
            config.putVm(msg.getDestHostInventory().getUuid(), msg.getVmInventory().getUuid(), VmInstanceState.Running);
        }
        bus.reply(msg, reply);
    }

    private void handle(VmAttachNicOnHypervisorMsg msg) {
        bus.reply(msg, new VmAttachNicOnHypervisorReply());
    }

    private void handle(DetachNicFromVmOnHypervisorMsg msg) {
        DetachNicFromVmOnHypervisorReply reply = new DetachNicFromVmOnHypervisorReply();
        bus.reply(msg, reply);
    }

    private void handle(TakeSnapshotOnHypervisorMsg msg) {
        TakeSnapshotOnHypervisorReply reply = new TakeSnapshotOnHypervisorReply();
        if (!config.snapshotSuccess) {
            reply.setError(operr("on purpose"));
            bus.reply(msg, reply);
            return;
        }

        List<TakeSnapshotOnHypervisorMsg> lst = config.snapshots.get(msg.getHostUuid());
        lst = new ArrayList<TakeSnapshotOnHypervisorMsg>();
        lst.add(msg);
        config.snapshots.put(msg.getHostUuid(), lst);
    }

    private void handle(RemoveVmOnSimulatorMsg msg) {
        logger.debug(String.format("Successfully remove vm[uuid:%s] on simulator host[uuid:%s]", msg.getVmUuid(), msg.getHostUuid()));
        removeVm(msg.getVmUuid());
        MessageReply reply = new MessageReply();
        bus.reply(msg, reply);
    }

    private void handle(ChangeVmStateOnSimulatorHostMsg msg) {
        synchronized (config.vms) {
            if (!config.containVm(msg.getVmUuid())) {
                logger.warn(String.format("Cannot find vm[uuid:%s] on simulator host[uuid:%s], it's a stranger vm", msg.getVmUuid(), self.getUuid()));
            } else {
                logger.debug(String.format("Change state of vm[uuid:%s] on simulator host[uuid:%s] to %s", msg.getVmUuid(), self.getUuid(), msg.getVmState()));
            }
            String hostUuid = msg.getHostUuid() == null ? self.getUuid() : msg.getHostUuid();
            config.putVm(hostUuid, msg.getVmUuid(), VmInstanceState.valueOf(msg.getVmState()));
        }
        MessageReply reply = new MessageReply();
        bus.reply(msg, reply);
    }

    private void handle(SimulatorHostConnectionControlMsg msg) {
        logger.debug(String.format("Change simulator[uuid:%s] isDisonnected to %s", self.getUuid(), msg.isDisconnected()));
        isDisconnected = msg.isDisconnected();
        MessageReply reply = new MessageReply();
        bus.reply(msg, reply);
    }

    private void handle(CheckNetworkPhysicalInterfaceMsg msg) {
        logger.debug(String.format("Successfully checked physical network interface %s on simulator host", msg.getPhysicalInterface()));
        CheckNetworkPhysicalInterfaceReply reply = new CheckNetworkPhysicalInterfaceReply();
        bus.reply(msg, reply);
    }

    private void handle(DestroyVmOnHypervisorMsg msg) {
        logger.debug(String.format("Successfully destroyed vm on simulator host[uuid:%s], %s", self.getUuid(), JSONObjectUtil.toJsonString(msg.getVmInventory())));
        DestroyVmOnHypervisorReply reply = new DestroyVmOnHypervisorReply();
        bus.reply(msg, reply);
    }

    private void handle(DetachVolumeFromVmOnHypervisorMsg msg) {
        logger.debug(String.format("Successfully detached volume[uuid:%s] to vm[uuid:%s] on simulator host[uuid:%s]", msg.getInventory().getUuid(), msg.getVmInventory().getUuid(), self.getUuid()));
        DetachVolumeFromVmOnHypervisorReply reply = new DetachVolumeFromVmOnHypervisorReply();
        bus.reply(msg, reply);
    }

    private void handle(AttachVolumeToVmOnHypervisorMsg msg) {
        logger.debug(String.format("Successfully attached volume[uuid:%s] to vm[uuid:%s] on simulator host[uuid:%s]", msg.getInventory().getUuid(), msg.getVmInventory().getUuid(), self.getUuid()));
        AttachVolumeToVmOnHypervisorReply reply = new AttachVolumeToVmOnHypervisorReply();
        bus.reply(msg, reply);
    }

    private void handle(StartVmOnHypervisorMsg msg) {
        logger.debug(String.format("Successfully started vm on simulator host[uuid:%s], %s", self.getUuid(), JSONObjectUtil.toJsonString(msg.getVmSpec())));
        StartVmOnHypervisorReply reply = new StartVmOnHypervisorReply();
        setVmState(msg.getVmSpec().getVmInventory().getUuid(), VmInstanceState.Running);
        bus.reply(msg, reply);
    }

    private void handle(StopVmOnHypervisorMsg msg) {
        logger.debug(String.format("Successfully stopped vm on simulator host[uuid:%s], %s", self.getUuid(), JSONObjectUtil.toJsonString(msg.getVmInventory())));
        StopVmOnHypervisorReply reply = new StopVmOnHypervisorReply();
        setVmState(msg.getVmInventory().getUuid(), VmInstanceState.Stopped);
        bus.reply(msg, reply);
    }

    private void handle(CreateVmOnHypervisorMsg msg) {
        logger.debug(String.format("Successfully created vm on simulator host[uuid:%s], %s", self.getUuid(), JSONObjectUtil.toJsonString(msg.getVmSpec())));
        CreateVmOnHypervisorReply reply = new CreateVmOnHypervisorReply();
        setVmState(msg.getVmSpec().getVmInventory().getUuid(), VmInstanceState.Running);
        bus.reply(msg, reply);
    }

    private void handle(RebootVmOnHypervisorMsg msg) {
        logger.debug(String.format("Successfully rebooted vm on simulator host[uuid:%s], %s", self.getUuid(), JSONObjectUtil.toJsonString(msg.getVmInventory())));
        RebootVmOnHypervisorReply reply = new RebootVmOnHypervisorReply();
        setVmState(msg.getVmInventory().getUuid(), VmInstanceState.Running);
        bus.reply(msg, reply);
    }

    void setVmState(String vmUuid, VmInstanceState state) {
        synchronized (config.vms) {
            config.putVm(self.getUuid(), vmUuid, state);
        }
    }

    void removeVm(String vmUuid) {
        synchronized (config.vms) {
            config.removeVm(self.getUuid(), vmUuid);
        }
    }

    protected int getHostSyncLevel() {
        return 10;
    }

    protected HostInventory getSelfInventory() {
        return HostInventory.valueOf(self);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }
}
