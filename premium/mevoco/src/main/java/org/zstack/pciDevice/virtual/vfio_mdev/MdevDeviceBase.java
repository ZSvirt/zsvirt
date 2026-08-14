package org.zstack.pciDevice.virtual.vfio_mdev;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.pciDevice.virtual.VirtualPciDeviceBase;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by GuoYi on 2019-04-24.
 */
public class MdevDeviceBase extends VirtualPciDeviceBase {
    public static final String DELETE_VFIO_MDEV_DEVICE = "/mdevdevice/delete";
    public static final String HOT_PLUG_MDEV_DEVICE = "/mdevdevice/hotplug";
    public static final String HOT_UNPLUG_MDEV_DEVICE = "/mdevdevice/hotunplug";
    private static CLogger logger = Utils.getLogger(MdevDeviceBase.class);

    protected MdevDeviceVO self;

    public MdevDeviceBase() {
    }

    public MdevDeviceBase(MdevDeviceVO self) {
        this.self = self;
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

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAttachMdevDeviceToVmMsg) {
            handle((APIAttachMdevDeviceToVmMsg) msg);
        } else if (msg instanceof APIDetachMdevDeviceFromVmMsg) {
            handle((APIDetachMdevDeviceFromVmMsg) msg);
        } else if (msg instanceof APIUpdateMdevDeviceMsg) {
            handle((APIUpdateMdevDeviceMsg) msg);
        } else if (msg instanceof APIDeleteMdevDeviceMsg) {
            handle((APIDeleteMdevDeviceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof DetachMdevDeviceMsg) {
            handle((DetachMdevDeviceMsg) msg);
        } else if (msg instanceof CheckAndReserveMdevDeviceMsg) {
            handle((CheckAndReserveMdevDeviceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CheckAndReserveMdevDeviceMsg msg) {
        CheckAndReserveMdevDeviceReply reply = new CheckAndReserveMdevDeviceReply();
        thdf.chainSubmit(new ChainTask(reply) {
            @Override
            public String getSyncSignature() {
                return MdevDeviceConstants.CHECK_AND_RESERVE_MDEV_DEVICE_FOR_VM;
            }

            @Override
            public void run(SyncTaskChain chain) {
                // check
                String vmUuid = msg.getVmUuid();
                String mdevUuid = msg.getMdevUuid();

                if (!MdevDeviceUtils.checkMdevDeviceAvailable(mdevUuid)) { // no attached, no reserved
                    reply.setError(operr("mdev device [%s] is not available", mdevUuid));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                // reserve
                MdevDeviceUtils.reserveMdevDeviceInDB(mdevUuid, vmUuid, MdevDeviceChooser.Device);
                logger.debug(String.format("reserved mdev device[uuid:%s] for vm[uuid:%s]", mdevUuid, vmUuid));

                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(DetachMdevDeviceMsg msg) {
        DetachMdevDeviceReply reply = new DetachMdevDeviceReply();

        // detach mdev device from vm
        MdevDeviceVO mdev = dbf.findByUuid(msg.getMdevDeviceUuid(), MdevDeviceVO.class);
        mdev.setVmInstanceUuid(null);
        mdev.setChooser(MdevDeviceChooser.None);
        mdev.setStatus(MdevDeviceStatus.Active);
        mdev = dbf.updateAndRefresh(mdev);
        logger.debug(String.format("detached mdev device[uuid:%s] from vm[uuid:%s]",
                msg.getMdevDeviceUuid(), msg.getVmInstanceUuid()));
        reply.setInventory(mdev.toInventory());
        bus.reply(msg, reply);
    }

    private void handle(APIAttachMdevDeviceToVmMsg msg) {
        APIAttachMdevDeviceToVmEvent evt = new APIAttachMdevDeviceToVmEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("attach-mdev-device-to-vm");
        chain.then(new ShareFlow() {
            final String vmUuid = msg.getVmInstanceUuid();
            final String mdevUuid = msg.getMdevDeviceUuid();
            final List<String> hostUuids = new ArrayList<>();
            final boolean isSeDevice = Q.New(MdevDeviceVO.class)
                    .eq(MdevDeviceVO_.uuid, mdevUuid)
                    .eq(MdevDeviceVO_.type, MdevDeviceType.SE_Controller)
                    .isExists();
            final boolean isRunningVm = Q.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, vmUuid)
                    .eq(VmInstanceVO_.state, VmInstanceState.Running)
                    .isExists();

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "check-and-reserve-mdev-device-before-attach-to-vm";

                    @Override
                    public void run(FlowTrigger trigger, @SuppressWarnings("rawtypes") Map data) {
                        CheckAndReserveMdevDeviceMsg cmsg = new CheckAndReserveMdevDeviceMsg();
                        cmsg.setVmUuid(vmUuid);
                        cmsg.setMdevUuid(mdevUuid);
                        bus.makeTargetServiceIdByResourceUuid(cmsg, MdevDeviceConstants.SERVICE_ID, mdevUuid);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    CheckAndReserveMdevDeviceReply rly = reply.castReply();
                                    if (!rly.isSuccess()) {
                                        trigger.fail(rly.getError());
                                    } else {
                                        trigger.next();
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, @SuppressWarnings("rawtypes") Map data) {
                        MdevDeviceUtils.detachMdevDeviceFromVmInDB(mdevUuid);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "hot-plug-mdev-device-to-running-vm";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VmInstanceVO vmvo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).find();
                        HotPlugMdevDeviceCommand cmd = new HotPlugMdevDeviceCommand();
                        cmd.setMdevDeviceUuid(mdevUuid);
                        cmd.setVmUuid(vmUuid);
                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setCommand(cmd);
                        msg.setHostUuid(vmvo.getHostUuid());
                        msg.setPath(HOT_PLUG_MDEV_DEVICE);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vmvo.getHostUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    KVMHostAsyncHttpCallReply r = reply.castReply();
                                    HotPlugMdevDeviceRsp rsp = r.toResponse(HotPlugMdevDeviceRsp.class);
                                    if (rsp.isSuccess()) {
                                        MdevDeviceUtils.attachMdevDeviceToVmInDB(mdevUuid);
                                        MdevDeviceVO mdev = dbf.findByUuid(mdevUuid, MdevDeviceVO.class);
                                        logger.debug(String.format("attached mdev device[uuid:%s] to vm instance[uuid:%s]",
                                                mdevUuid, vmUuid));
                                        evt.setInventory(mdev.toInventory());
                                        bus.publish(evt);
                                    } else {
                                        trigger.fail(operr("failed to hot plug mdev device to running vm, because:%s", rsp.getError()));
                                    }
                                } else {
                                    trigger.fail(reply.getError());
                                }
                                return;
                            }
                        });
                    }

                    @Override
                    public boolean skip(Map data) {
                        return !(isSeDevice && isRunningVm);
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "get-candidate-host-to-start-vm";

                    @Override
                    public void run(FlowTrigger trigger, @SuppressWarnings("rawtypes") Map data) {
                        // make sure vm can start in host that holds the mdev device
                        GetVmStartingCandidateClustersHostsMsg gmsg = new GetVmStartingCandidateClustersHostsMsg();
                        gmsg.setUuid(msg.getVmInstanceUuid());
                        bus.makeLocalServiceId(gmsg, VmInstanceConstant.SERVICE_ID);
                        bus.send(gmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(operr("failed to get candidate hosts to start vm[uuid:%s], %s",
                                            msg.getVmInstanceUuid(), reply.getError()));
                                    return;
                                }

                                GetVmStartingCandidateClustersHostsReply grly = (GetVmStartingCandidateClustersHostsReply) reply;
                                if (!grly.isSuccess()) {
                                    trigger.fail(operr("failed to get candidate hosts to start vm[uuid:%s], %s",
                                            msg.getVmInstanceUuid(), grly.getError()));
                                    return;
                                }

                                hostUuids.addAll(grly.getHostInventories().stream().map(HostInventory::getUuid).collect(Collectors.toList()));
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "check-and-allocate-mdev-device-to-stopping-vm";

                    @Override
                    public void run(FlowTrigger trigger, @SuppressWarnings("rawtypes") Map data) {
                        boolean exists = Q.New(MdevDeviceVO.class)
                                .eq(MdevDeviceVO_.uuid, mdevUuid)
                                .eq(MdevDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                                .eq(MdevDeviceVO_.status, MdevDeviceStatus.Reserved)
                                .in(MdevDeviceVO_.hostUuid, hostUuids)
                                .isExists();
                        if (!exists) {
                            MdevDeviceUtils.detachMdevDeviceFromVmInDB(mdevUuid);
                            trigger.fail(operr("vm[uuid:%s] cannot start in host that hold mdev device[uuid:%s]",
                                    msg.getVmInstanceUuid(), msg.getMdevDeviceUuid()));
                            return;
                        }

                        MdevDeviceUtils.attachMdevDeviceToVmInDB(msg.getMdevDeviceUuid());
                        MdevDeviceVO mdev = dbf.findByUuid(msg.getMdevDeviceUuid(), MdevDeviceVO.class);

                        logger.debug(String.format("attached mdev device[uuid:%s] to vm instance[uuid:%s]",
                                msg.getMdevDeviceUuid(), msg.getVmInstanceUuid()));
                        evt.setInventory(mdev.toInventory());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(evt) {
                    @Override
                    public void handle(@SuppressWarnings("rawtypes") Map data) {
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(evt) {
                    @Override
                    public void handle(ErrorCode errCode, @SuppressWarnings("rawtypes") Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void handle(APIDetachMdevDeviceFromVmMsg msg) {
        APIDetachMdevDeviceFromVmEvent evt = new APIDetachMdevDeviceFromVmEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("delete-mdev-device-to-vm");
        chain.then(new ShareFlow() {
            final String vmUuid = msg.getVmInstanceUuid();
            final String mdevUuid = msg.getMdevDeviceUuid();
            final boolean isSeDevice = Q.New(MdevDeviceVO.class)
                    .eq(MdevDeviceVO_.uuid, mdevUuid)
                    .eq(MdevDeviceVO_.type, MdevDeviceType.SE_Controller)
                    .isExists();
            final boolean isRunningVm = Q.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, vmUuid)
                    .eq(VmInstanceVO_.state, VmInstanceState.Running)
                    .isExists();

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "hot-unplug-mdev-device-from-running-vm";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VmInstanceVO vmvo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).find();
                        HotUnplugMdevDeviceCommand cmd = new HotUnplugMdevDeviceCommand();
                        cmd.setMdevDeviceUuid(mdevUuid);
                        cmd.setVmUuid(vmUuid);
                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setCommand(cmd);
                        msg.setHostUuid(vmvo.getHostUuid());
                        msg.setPath(HOT_UNPLUG_MDEV_DEVICE);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vmvo.getHostUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    KVMHostAsyncHttpCallReply r = reply.castReply();
                                    HotUnplugMdevDeviceRsp rsp = r.toResponse(HotUnplugMdevDeviceRsp.class);
                                    if (rsp.isSuccess()) {
                                        trigger.next();
                                    } else {
                                        trigger.fail(operr("failed to hot unplug mdev device to running vm, because:%s", rsp.getError()));
                                    }
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public boolean skip(Map data) {
                        return !(isSeDevice && isRunningVm);
                    }
                });
                
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        DetachMdevDeviceMsg dmsg = new DetachMdevDeviceMsg();
                        dmsg.setMdevDeviceUuid(msg.getMdevDeviceUuid());
                        dmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                        bus.makeLocalServiceId(dmsg, MdevDeviceConstants.SERVICE_ID);
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(operr("failed to get candidate hosts to start vm[uuid:%s], %s",
                                            msg.getVmInstanceUuid(), reply.getError()));
                                    return;
                                } else {
                                    DetachMdevDeviceReply rly = reply.castReply();
                                    evt.setInventory(rly.getInventory());
                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(@SuppressWarnings("rawtypes") Map data) {
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, @SuppressWarnings("rawtypes") Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void handle(APIUpdateMdevDeviceMsg msg) {
        APIUpdateMdevDeviceEvent evt = new APIUpdateMdevDeviceEvent(msg.getId());
        MdevDeviceVO mdev = dbf.findByUuid(msg.getUuid(), MdevDeviceVO.class);
        if (StringUtils.isNotBlank(msg.getName())) {
            mdev.setName(msg.getName());
        }
        if (StringUtils.isNotBlank(msg.getDescription())) {
            mdev.setDescription(msg.getDescription());
        }
        if (StringUtils.isNotBlank(msg.getState())) {
            mdev.setState(MdevDeviceState.valueOf(msg.getState()));
        }
        mdev = dbf.updateAndRefresh(mdev);
        logger.debug(String.format("updated mdev device[uuid:%s], name: %s, state: %s",
                msg.getMdevDeviceUuid(), mdev.getName(), mdev.getState()));
        evt.setInventory(mdev.toInventory());
        bus.publish(evt);
    }


    private void handle(APIDeleteMdevDeviceMsg msg) {
        APIDeleteMdevDeviceEvent evt = new APIDeleteMdevDeviceEvent(msg.getId());
        MdevDeviceVO mdev = dbf.findByUuid(msg.getMdevDeviceUuid(), MdevDeviceVO.class);
        String hostUuid = mdev.getHostUuid();
        DeleteVfioMdevDeviceCommand cmd = new DeleteVfioMdevDeviceCommand();
        cmd.setMdevDeviceUuid(mdev.getUuid());
        KVMHostAsyncHttpCallMsg smsg = new KVMHostAsyncHttpCallMsg();
        smsg.setCommand(cmd);
        smsg.setHostUuid(hostUuid);
        smsg.setPath(DELETE_VFIO_MDEV_DEVICE);
        bus.makeTargetServiceIdByResourceUuid(smsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(smsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    DeleteVfioMdevDeviceRsp rsp = r.toResponse(DeleteVfioMdevDeviceRsp.class);
                    if (rsp.isSuccess()) {
                        MdevDeviceVO vo = Q.New(MdevDeviceVO.class).eq(MdevDeviceVO_.uuid, mdev.getUuid()).find();
                        dbf.remove(vo);
                        logger.debug(String.format("delete mdev device[uuid:%s]", mdev.getUuid()));
                    } else {
                        reply.setError(operr("operation error, because:%s", rsp.getError()));
                        evt.setError(reply.getError());
                    }
                } else {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    public static class DeleteVfioMdevDeviceCommand extends KVMAgentCommands.AgentCommand {
        public String MdevDeviceUuid;

        public String getMdevDeviceUuid() {
            return MdevDeviceUuid;
        }

        public void setMdevDeviceUuid(String mdevDeviceUuid) {
            MdevDeviceUuid = mdevDeviceUuid;
        }
    }


    public static class DeleteVfioMdevDeviceRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class HotPlugMdevDeviceCommand extends KVMAgentCommands.AgentCommand {
        public String MdevDeviceUuid;
        public String vmUuid;

        public String getMdevDeviceUuid() {
            return MdevDeviceUuid;
        }

        public void setMdevDeviceUuid(String mdevDeviceUuid) {
            MdevDeviceUuid = mdevDeviceUuid;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class HotPlugMdevDeviceRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class HotUnplugMdevDeviceCommand extends KVMAgentCommands.AgentCommand {
        public String MdevDeviceUuid;
        public String vmUuid;

        public String getMdevDeviceUuid() {
            return MdevDeviceUuid;
        }

        public void setMdevDeviceUuid(String mdevDeviceUuid) {
            MdevDeviceUuid = mdevDeviceUuid;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class HotUnplugMdevDeviceRsp extends KVMAgentCommands.AgentResponse {
    }
}
