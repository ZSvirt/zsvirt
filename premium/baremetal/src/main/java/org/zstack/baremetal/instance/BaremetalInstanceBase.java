package org.zstack.baremetal.instance;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.baremetal.BaremetalUtils;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.baremetal.chassis.BaremetalChassisConstant;
import org.zstack.header.baremetal.chassis.BaremetalChassisStatus;
import org.zstack.header.baremetal.chassis.BaremetalChassisVO;
import org.zstack.header.baremetal.chassis.BaremetalChassisVO_;
import org.zstack.header.baremetal.instance.*;
import org.zstack.header.baremetal.instance.BaremetalInstanceDeletionPolicyManager.BaremetalInstanceDeletionPolicy;
import org.zstack.header.baremetal.network.*;
import org.zstack.header.baremetal.pxeserver.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.allocator.BeforeAllocateIpExtensionPoint;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by GuoYi on 7/9/18.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BaremetalInstanceBase implements BaremetalInstance {
    protected static final CLogger logger = Utils.getLogger(BaremetalInstanceBase.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected BaremetalInstanceDeletionPolicyManager deletionPolicyMgr;
    @Autowired
    protected PluginRegistry pluginRgty;

    protected BaremetalInstanceVO self;
    BaremetalInstanceBase(BaremetalInstanceVO self) {
        this.self = self;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof StartBaremetalInstanceMsg) {
            handle((StartBaremetalInstanceMsg) msg);
        } else if (msg instanceof StopBaremetalInstanceMsg) {
            handle((StopBaremetalInstanceMsg) msg);
        } else if (msg instanceof DestroyBaremetalInstanceMsg) {
            handle((DestroyBaremetalInstanceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(StartBaremetalInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "start-baremetal-instance-" + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                doStartBM(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        StartBaremetalInstanceReply reply = new StartBaremetalInstanceReply();
                        reply.setInventory(BaremetalInstanceInventory.valueOf(self));
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.info(String.format("Failed to start baremetal instance[uuid:%s]", self.getUuid()));
                        StartBaremetalInstanceReply reply = new StartBaremetalInstanceReply();
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(StopBaremetalInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "stop-baremetal-instance-" + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                StopBaremetalInstanceReply reply = new StopBaremetalInstanceReply();
                if (self.getState() == BaremetalInstanceState.Stopped) {
                    reply.setInventory(BaremetalInstanceInventory.valueOf(self));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                doStopBM(new Completion(chain) {
                    @Override
                    public void success() {
                        self.setState(BaremetalInstanceState.Stopped);
                        dbf.update(self);
                        logger.info(String.format("Successfully stopped baremetal instance[uuid:%s]", self.getUuid()));
                        reply.setInventory(BaremetalInstanceInventory.valueOf(self));
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.info(String.format("Failed to stop baremetal instance[uuid:%s]", self.getUuid()));
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(DestroyBaremetalInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return BaremetalInstanceConstant.SYNC_SIGNATURE_OF_BAREMETAL_INSTANCE + self.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                final BaremetalInstanceDeletionPolicy deletionPolicy = getBaremetalDeletionPolicy(msg);
                final DestroyBaremetalInstanceReply reply = new DestroyBaremetalInstanceReply();

                doDestroyBM(deletionPolicy, new Completion(chain) {
                    @Override
                    public void success() {
                        logger.info(String.format("Successfully destroyed baremetal instance[uuid:%s]", self.getUuid()));
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.info(String.format("Failed to destroy baremetal instance[uuid:%s]", self.getUuid()));
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void pxeBootBM(StartBaremetalInstanceMsg msg, Completion completion) {
        // update bm state and status
        self.setStatus(BaremetalInstanceStatus.Unprovisioned);
        if (msg.getReboot()) {
            self.setState(BaremetalInstanceState.Rebooting);
        } else {
            self.setState(BaremetalInstanceState.Starting);
        }
        self = dbf.updateAndRefresh(self);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("pxe-boot-baremetal-instance-%s", msg.getUuid()));
        chain.then(new ShareFlow() {
            BaremetalPxeServerVO pxeServer;
            BaremetalImageCacheInventory cache;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "allocate-baremetal-pxeserver";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        pxeServer = new SQLBatchWithReturn<BaremetalPxeServerVO>() {
                            @Override
                            protected BaremetalPxeServerVO scripts() {
                                // allocated before
                                if (self.getPxeServerUuid() != null) {
                                    return findByUuid(self.getPxeServerUuid(), BaremetalPxeServerVO.class);
                                }

                                List<String> pxeServerUuids = q(BaremetalPxeServerClusterRefVO.class)
                                        .eq(BaremetalPxeServerClusterRefVO_.clusterUuid, self.getClusterUuid())
                                        .select(BaremetalPxeServerClusterRefVO_.pxeServerUuid)
                                        .listValues();
                                if (pxeServerUuids == null || pxeServerUuids.isEmpty()) {
                                    logger.error(String.format("no baremetal pxeserver attached in cluster[uuid:%s]", self.getClusterUuid()));
                                    return null;
                                }

                                for (String pxeServerUuid : pxeServerUuids) {
                                    if (q(BaremetalImageCacheVO.class)
                                            .eq(BaremetalImageCacheVO_.imageUuid, self.getImageUuid())
                                            .eq(BaremetalImageCacheVO_.pxeServerUuid, pxeServerUuid)
                                            .isExists()) {
                                        return findByUuid(pxeServerUuid, BaremetalPxeServerVO.class);
                                    }
                                }

                                Long imageSize = q(ImageVO.class)
                                        .eq(ImageVO_.uuid, self.getImageUuid())
                                        .select(ImageVO_.actualSize)
                                        .findValue();

                                long reservedCapacity = SizeUtils.sizeStringToBytes(BaremetalPxeServerGlobalConfig.RESERVED_CAPACITY.value());
                                String pxeServerUuid = q(BaremetalPxeServerVO.class)
                                        .in(BaremetalPxeServerVO_.uuid, pxeServerUuids)
                                        .gt(BaremetalPxeServerVO_.availableCapacity, imageSize + reservedCapacity)
                                        .orderBy(BaremetalPxeServerVO_.availableCapacity, SimpleQuery.Od.DESC)
                                        .select(BaremetalPxeServerVO_.uuid)
                                        .limit(1)
                                        .findValue();
                                if (pxeServerUuid == null) {
                                    return null;
                                }
                                return findByUuid(pxeServerUuid, BaremetalPxeServerVO.class);
                            }
                        }.execute();

                        if (pxeServer == null) {
                            trigger.fail(Platform.operr(
                                    "failed to allocate baremetal pxeserver, make sure there is a pxeserver with enough available capacity attached on cluster[uuid:%s]"
                                    , self.getClusterUuid()
                            ));
                        } else {
                            self.setPxeServerUuid(pxeServer.getUuid());
                            self = dbf.updateAndRefresh(self);
                            trigger.next();
                        }
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "download-image-for-baremetal-instance-" + self.getUuid();

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        DownloadBaremetalImageCacheMsg dmsg = new DownloadBaremetalImageCacheMsg();
                        dmsg.setImageUuid(self.getImageUuid());
                        dmsg.setPxeServerUuid(pxeServer.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalPxeServerConstant.SERVICE_ID, self.getUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    DownloadBaremetalImageCacheReply rly = reply.castReply();
                                    cache = rly.getInventory();
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-pxe-configs-for-bm-instance";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateBaremetalInstanceConfigsMsg cmsg = new CreateBaremetalInstanceConfigsMsg();
                        cmsg.setPxeServerUuid(pxeServer.getUuid());
                        cmsg.setBmUuid(self.getUuid());
                        cmsg.setImageUuid(self.getImageUuid());
                        cmsg.setPxeNicMac(getPxeBootMacAddress(self.getUuid()));
                        cmsg.setUsername(self.getUsername());
                        cmsg.setPassword(BaremetalUtils.getEncryptedPassword(self.getPassword()));
                        cmsg.setTemplateUuid(self.getTemplateUuid());

                        bus.makeTargetServiceIdByResourceUuid(cmsg, BaremetalPxeServerConstant.SERVICE_ID, pxeServer.getUuid());
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        DeleteBaremetalInstanceConfigsMsg dmsg = new DeleteBaremetalInstanceConfigsMsg();
                        dmsg.setPxeServerUuid(pxeServer.getUuid());
                        dmsg.setPxeNicMac(getPxeBootMacAddress(self.getUuid()));

                        bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalPxeServerConstant.SERVICE_ID, pxeServer.getUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                self.setPxeServerUuid(null);
                                self = dbf.updateAndRefresh(self);
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "power-on-baremetal-instance-" + self.getUuid();

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        BaremetalChassisVO chassis = dbf.findByUuid(self.getChassisUuid(), BaremetalChassisVO.class);
                        powerOnBaremetalChassis(chassis, BaremetalChassisConstant.SERVER_BOOT_DEV_PXE,
                                msg.getReboot(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void diskBootBM(StartBaremetalInstanceMsg msg, Completion completion) {
        // update bm state
        if (msg.getReboot()) {
            self.setState(BaremetalInstanceState.Rebooting);
        } else {
            self.setState(BaremetalInstanceState.Starting);
        }
        self = dbf.updateAndRefresh(self);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("disk-boot-baremetal-instance-%s", msg.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "power-on-baremetal-instance-" + self.getUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                BaremetalChassisVO chassis = dbf.findByUuid(self.getChassisUuid(), BaremetalChassisVO.class);
                powerOnBaremetalChassis(chassis, BaremetalChassisConstant.SERVER_BOOT_DEV_DISK,
                        msg.getReboot(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        });

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void doStartBM(StartBaremetalInstanceMsg msg, Completion completion) {
        if (msg.getPxeBoot()) {
            pxeBootBM(msg, completion);
        } else {
            diskBootBM(msg, completion);
        }
    }

    private String getPxeBootMacAddress(String bmUuid) {
        return Q.New(BaremetalNicVO.class)
                .eq(BaremetalNicVO_.baremetalInstanceUuid, bmUuid)
                .eq(BaremetalNicVO_.pxe, true)
                .select(BaremetalNicVO_.mac)
                .limit(1)
                .findValue();
    }

    private void powerOnBaremetalChassis(BaremetalChassisVO bmc, String bootDev, boolean reboot, Completion completion) {
        if (!reboot && BaremetalUtils.powerOnRemoteServer(bmc.toInventory(), bootDev)) {
            completion.success();
        } else if (reboot && BaremetalUtils.powerResetRemoteServer(bmc.toInventory(), bootDev)) {
            completion.success();
        } else {
            completion.fail(operr("Failed to remotely power %s baremetal chassis[uuid:%s]", reboot ? "reset" : "on", bmc.getUuid()));
        }
    }

    private void powerOffBaremetalChassis(BaremetalChassisVO bmc, Completion completion) {
        if (BaremetalUtils.powerOffRemoteServer(bmc.toInventory())) {
            completion.success();
        } else {
            completion.fail(operr("Failed to remotely power off baremetal chassis[uuid:%s]", bmc.getUuid()));
        }
    }

    private BaremetalInstanceDeletionPolicy getBaremetalDeletionPolicy(final DestroyBaremetalInstanceMsg msg) {
        if (self.getState() == BaremetalInstanceState.Created) {
            return BaremetalInstanceDeletionPolicy.Direct;
        }

        return msg.getDeletionPolicy() == null ?
                deletionPolicyMgr.getDeletionPolicy(self.getUuid()) :
                BaremetalInstanceDeletionPolicy.valueOf(msg.getDeletionPolicy());
    }

    private void doDestroyBM(BaremetalInstanceDeletionPolicy deletionPolicy, Completion completion) {
        if (deletionPolicy == BaremetalInstanceDeletionPolicy.Delay) {
            StopBaremetalInstanceMsg smsg = new StopBaremetalInstanceMsg();
            smsg.setUuid(self.getUuid());
            bus.makeTargetServiceIdByResourceUuid(smsg, BaremetalInstanceConstant.SERVICE_ID, self.getUuid());
            bus.send(smsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    self.setState(BaremetalInstanceState.Destroyed);

                    // if bm is provisioning while it's been destroyed, update bm status to unprovisioned
                    if (self.getStatus() == BaremetalInstanceStatus.Provisioning) {
                        self.setStatus(BaremetalInstanceStatus.Unprovisioned);
                    }
                    self = dbf.updateAndRefresh(self);
                    completion.success();
                }
            });
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("expunge-baremetal-instance-%s", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "stop-baremetal-instance";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                StopBaremetalInstanceMsg smsg = new StopBaremetalInstanceMsg();
                smsg.setUuid(self.getUuid());
                bus.makeTargetServiceIdByResourceUuid(smsg, BaremetalInstanceConstant.SERVICE_ID, self.getUuid());
                bus.send(smsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        self.setState(BaremetalInstanceState.Destroyed);
                        self = dbf.updateAndRefresh(self);
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-baremetal-instance-configs";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (self.getPxeServerUuid() == null) {
                    trigger.next();
                    return;
                }

                DeleteBaremetalInstanceConfigsMsg dmsg = new DeleteBaremetalInstanceConfigsMsg();
                dmsg.setPxeServerUuid(self.getPxeServerUuid());
                dmsg.setPxeNicMac(getPxeBootMacAddress(self.getUuid()));
                bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalPxeServerConstant.SERVICE_ID, self.getPxeServerUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-baremetal-novnc-proxy";

            @Override
            public void run(FlowTrigger trigger, Map data){
                if (self.getPxeServerUuid() == null) {
                    trigger.next();
                    return;
                }

                DeleteBaremetalNoVNCProxyMsg dmsg = new DeleteBaremetalNoVNCProxyMsg();
                dmsg.setPxeServerUuid(self.getPxeServerUuid());
                dmsg.setBaremetalInstanceUuid(self.getUuid());
                bus.makeLocalServiceId(dmsg, BaremetalPxeServerConstant.SERVICE_ID);
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-baremetal-terminal-proxy";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (self.getPxeServerUuid() == null) {
                    trigger.next();
                    return;
                }

                DeleteBaremetalTerminalProxyMsg dmsg = new DeleteBaremetalTerminalProxyMsg();
                dmsg.setPxeServerUuid(self.getPxeServerUuid());
                dmsg.setBaremetalInstanceUuid(self.getUuid());
                bus.makeLocalServiceId(dmsg, BaremetalPxeServerConstant.SERVICE_ID);
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete bm nics and return allocated ips";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                // delete BaremetalBondingVO
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        String chassisUuid = q(BaremetalInstanceVO.class)
                                .eq(BaremetalInstanceVO_.uuid, self.getUuid())
                                .select(BaremetalInstanceVO_.chassisUuid)
                                .findValue();
                        sql(BaremetalBondingVO.class)
                                .eq(BaremetalBondingVO_.chassisUuid, chassisUuid)
                                .delete();
                    }
                }.execute();

                List<DeleteBaremetalNicMsg> dmsgs = new ArrayList<>();
                List<BaremetalNicVO> bmNics = Q.New(BaremetalNicVO.class)
                        .eq(BaremetalNicVO_.baremetalInstanceUuid, self.getUuid())
                        .list();
                for (BaremetalNicVO nic : bmNics) {
                    DeleteBaremetalNicMsg dmsg = new DeleteBaremetalNicMsg();
                    dmsg.setUuid(nic.getUuid());
                    dmsg.setBaremetalInstanceUuid(self.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalNetworkConstant.SERVICE_ID, self.getUuid());
                    dmsgs.add(dmsg);
                    for (BeforeAllocateIpExtensionPoint extension: pluginRgty.getExtensionList(BeforeAllocateIpExtensionPoint.class)) {
                        extension.releaseIpFromSdn(nic.getUuid(), nic.getL3NetworkUuid());
                    }
                }

                if (dmsgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                bus.send(dmsgs, 1, new CloudBusListCallBack(trigger) {
                    @Override
                    public void run(List<MessageReply> replies) {
                        trigger.next();
                    }
                });
            }
        });

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        // return chassis
                        sql(BaremetalChassisVO.class)
                                .eq(BaremetalChassisVO_.uuid, self.getChassisUuid())
                                .set(BaremetalChassisVO_.status, BaremetalChassisStatus.Available)
                                .update();

                        // delete bm instance
                        remove(self);
                    }
                }.execute();
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void doStopBM(Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("stop-baremetal-instance-%s", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "soft-stop-baremetal-instance-" + self.getUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                // TODO
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "ipmi-stop-baremetal-instance-" + self.getUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                BaremetalChassisVO chassis = dbf.findByUuid(self.getChassisUuid(), BaremetalChassisVO.class);
                powerOffBaremetalChassis(chassis, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        });

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }
}
