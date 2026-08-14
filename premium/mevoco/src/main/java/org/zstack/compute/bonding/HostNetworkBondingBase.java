package org.zstack.compute.bonding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.bonding.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.*;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.network.hostNetworkInterface.*;
import org.zstack.pciDevice.PciDeviceUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;


import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostNetworkBondingBase {
    protected static final CLogger logger = Utils.getLogger(HostNetworkBondingBase.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private HostNetworkBondingManager bondMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ThreadFacade thdf;

    protected HostNetworkBondingVO self;
    protected String syncThreadName;

    public HostNetworkBondingBase(HostNetworkBondingVO self) {
        this.self = self;
        this.syncThreadName = "Bonding-" + self.getUuid();
    }

    protected HostNetworkBondingInventory getSelfInventory() {
        return HostNetworkBondingInventory.valueOf(self);
    }

    @MessageSafe
    void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateBondingMsg) {
            handle((APIUpdateBondingMsg) msg);
        } else if (msg instanceof APIDeleteBondingMsg) {
            handle((APIDeleteBondingMsg) msg);
        } else if (msg instanceof APIAttachNicToBondingMsg) {
            handle((APIAttachNicToBondingMsg) msg);
        } else if (msg instanceof APIDetachNicFromBondingMsg) {
            handle((APIDetachNicFromBondingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof UpdateBondingMsg) {
            handle((UpdateBondingMsg) msg);
        } else if (msg instanceof BondingDeletionMsg) {
            handle((BondingDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    void attachSlavesToBonding(HostNetworkBondingVO bondingVO, Set<String> slaveUuids) {
        Set<HostNetworkInterfaceVO> nicVOs = new HashSet<>();
        for (String slaveUuid : slaveUuids) {
            HostNetworkInterfaceVO interfaceVO = dbf.findByUuid(slaveUuid, HostNetworkInterfaceVO.class);
            if (interfaceVO != null) {
                interfaceVO.setBondingUuid(bondingVO.getUuid());
                interfaceVO.setInterfaceType(NetworkInterfaceType.bondingSlave.toString());
                nicVOs.add(interfaceVO);

                PciDeviceUtils.updatePciDevicePassThroughStateFromAvailableToDisabled(
                        interfaceVO.getInterfaceName(), interfaceVO.getHostUuid());
            }
        }
        dbf.updateCollection(nicVOs);
    }

    void detachSlavesFromBonding(HostNetworkBondingVO bondingVO, Set<String> slaveUuids) {
        if (!slaveUuids.isEmpty()) {
            SQL.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.hostUuid, bondingVO.getHostUuid())
                    .eq(HostNetworkInterfaceVO_.bondingUuid, bondingVO.getUuid())
                    .in(HostNetworkInterfaceVO_.uuid, slaveUuids)
                    .set(HostNetworkInterfaceVO_.bondingUuid, null)
                    .set(HostNetworkInterfaceVO_.interfaceType, NetworkInterfaceType.noMaster.toString())
                    .set(HostNetworkInterfaceVO_.slaveActive, Boolean.FALSE)
                    .update();

            PciDeviceUtils.updatePciDevicePassThroughStateFromDisabledToAvailable(new ArrayList<>(slaveUuids), bondingVO.getHostUuid());
        }
    }

    private void handle(APIUpdateBondingMsg msg) {
        APIUpdateBondingEvent event = new APIUpdateBondingEvent(msg.getId());

        UpdateBondingMsg umsg = new UpdateBondingMsg();
        umsg.setUuid(msg.getUuid());
        umsg.setSlaveUuids(msg.getSlaveUuids());
        umsg.setType(msg.getType());
        umsg.setMode(msg.getMode());
        umsg.setXmitHashPolicy(msg.getXmitHashPolicy());
        umsg.setDescription(msg.getDescription());

        bus.makeTargetServiceIdByResourceUuid(umsg, HostNetworkBondingConstant.SERVICE_ID, umsg.getUuid());
        bus.send(umsg, new CloudBusCallBack(umsg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    UpdateBondingReply uReply = reply.castReply();
                    event.setInventory(uReply.getInventory());
                } else {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void updateBonding(UpdateBondingMsg msg, Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("update-bonding-%s-on-host-%s", self.getBondingName(), self.getHostUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "update-bonding-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {

                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                HostNetworkBondingVO bondingVO = dbf.findByUuid(msg.getUuid(), HostNetworkBondingVO.class);
                                data.put(HostNetworkBondingConstant.Param.PREBONDINGVO.toString(), bondingVO);
                                bondingVO = dbf.reload(bondingVO);

                                // update bonding
                                if (msg.getMode() != null) {
                                    bondingVO.setMode(msg.getMode());
                                }
                                if (msg.getXmitHashPolicy() != null || HostNetworkBondingConstant.BONDING_MODE_AB.equals(msg.getMode())) {
                                    bondingVO.setXmitHashPolicy(msg.getXmitHashPolicy());
                                }
                                bondingVO.setDescription(msg.getDescription());
                                bondingVO = dbf.updateAndRefresh(bondingVO);

                                if (msg.getSlaveUuids() != null) {
                                    List<HostNetworkInterfaceInventory> oldSlaves = HostNetworkBondingInventory.valueOf(bondingVO).getSlaves();
                                    List<HostNetworkInterfaceVO> newSlaves = new ArrayList<>();
                                    if (!msg.getSlaveUuids().isEmpty()) {
                                        newSlaves.addAll(Q.New(HostNetworkInterfaceVO.class).in(HostNetworkInterfaceVO_.uuid, msg.getSlaveUuids()).list());
                                    }
                                    Set<String> oldSlaveUuids = oldSlaves.stream().map(HostNetworkInterfaceInventory::getUuid).collect(Collectors.toSet());
                                    Set<String> newSlaveUuids = newSlaves.stream().map(HostNetworkInterfaceVO::getUuid).collect(Collectors.toSet());
                                    Set<String> addedSlaveUuids = new HashSet<>(newSlaveUuids);
                                    addedSlaveUuids.removeAll(oldSlaveUuids);
                                    Set<String> removedSlaveUuids = new HashSet<>(oldSlaveUuids);
                                    removedSlaveUuids.removeAll(newSlaveUuids);

                                    // update interface
                                    detachSlavesFromBonding(bondingVO, removedSlaveUuids);
                                    attachSlavesToBonding(bondingVO, addedSlaveUuids);

                                    HostNetworkBondingInventory bondingInv = HostNetworkBondingInventory.valueOf(bondingVO);
                                    List<HostNetworkInterfaceInventory> slaveInv = new ArrayList<>(HostNetworkInterfaceInventory.valueOf(newSlaves));
                                    bondingInv.setSlaves(slaveInv);
                                    data.put(HostNetworkBondingConstant.Param.BONDINGINV.toString(), bondingInv);
                                } else {
                                    HostNetworkBondingInventory bondingInv = HostNetworkBondingInventory.valueOf(bondingVO);
                                    data.put(HostNetworkBondingConstant.Param.BONDINGINV.toString(), bondingInv);
                                }
                            }
                        }.execute();

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        HostNetworkBondingVO preBondingVO = (HostNetworkBondingVO) data.get(HostNetworkBondingConstant.Param.PREBONDINGVO.toString());

                        // update bonding
                        dbf.updateAndRefresh(preBondingVO);

                        if (msg.getSlaveUuids() != null) {
                            List<HostNetworkInterfaceInventory> oldSlaves = HostNetworkBondingInventory.valueOf(preBondingVO).getSlaves();
                            List<HostNetworkInterfaceVO> newSlaves = new ArrayList<>();
                            if (!msg.getSlaveUuids().isEmpty()) {
                                newSlaves.addAll(Q.New(HostNetworkInterfaceVO.class).in(HostNetworkInterfaceVO_.uuid, msg.getSlaveUuids()).list());
                            }
                            Set<String> oldSlaveUuids = oldSlaves.stream().map(HostNetworkInterfaceInventory::getUuid).collect(Collectors.toSet());
                            Set<String> newSlaveUuids = newSlaves.stream().map(HostNetworkInterfaceVO::getUuid).collect(Collectors.toSet());
                            Set<String> addedSlaveUuids = new HashSet<>(newSlaveUuids);
                            addedSlaveUuids.removeAll(oldSlaveUuids);
                            Set<String> removedSlaveUuids = new HashSet<>(oldSlaveUuids);
                            removedSlaveUuids.removeAll(newSlaveUuids);
                            // update interface
                            detachSlavesFromBonding(preBondingVO, addedSlaveUuids);
                            attachSlavesToBonding(preBondingVO, removedSlaveUuids);
                        }

                        trigger.rollback();
                    }
                });

                if (msg.getMode() != null || msg.getXmitHashPolicy() != null || msg.getSlaveUuids() !=null || msg.getType() != null) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "apply-to-backend";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            HostNetworkBondingInventory preBondingInv = HostNetworkBondingInventory.valueOf(
                                    (HostNetworkBondingVO) data.get(HostNetworkBondingConstant.Param.PREBONDINGVO.toString()));
                            HostNetworkBondingInventory bondingInv = (HostNetworkBondingInventory) data.get(HostNetworkBondingConstant.Param.BONDINGINV.toString());

                            String type = msg.getType() == null ? preBondingInv.getType() : msg.getType();
                            HostNetworkBondingFactory factory = bondMgr.getHostNetworkBondingFactory(type);
                            factory.updateBonding(preBondingInv, bondingInv, new Completion(trigger) {
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

                    flow(new NoRollbackFlow() {
                        String __name__ = "auto-detach-vf-nics-after-detach-slaves";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            if (msg.getSlaveUuids() == null) {
                                trigger.next();
                                return;
                            }

                            HostNetworkBondingVO preBondingVO = (HostNetworkBondingVO) data.get(HostNetworkBondingConstant.Param.PREBONDINGVO.toString());
                            List<String> l3Uuids = SQL.New("select l3.uuid from L2NetworkVO l2, L3NetworkVO l3" +
                                            " where l2.uuid = l3.l2NetworkUuid" +
                                            " and l2.physicalInterface = :bondingName")
                                    .param("bondingName", preBondingVO.getBondingName())
                                    .list();
                            if (l3Uuids.isEmpty()) {
                                trigger.next();
                                return;
                            }

                            List<HostNetworkInterfaceInventory> oldSlaves = HostNetworkBondingInventory.valueOf(preBondingVO).getSlaves();
                            List<HostNetworkInterfaceVO> newSlaves = new ArrayList<>();
                            if (!msg.getSlaveUuids().isEmpty()) {
                                newSlaves.addAll(Q.New(HostNetworkInterfaceVO.class).in(HostNetworkInterfaceVO_.uuid, msg.getSlaveUuids()).list());
                            }
                            Set<String> oldSlaveUuids = oldSlaves.stream().map(HostNetworkInterfaceInventory::getUuid).collect(Collectors.toSet());
                            Set<String> newSlaveUuids = newSlaves.stream().map(HostNetworkInterfaceVO::getUuid).collect(Collectors.toSet());
                            Set<String> removedSlaveUuids = new HashSet<>(oldSlaveUuids);
                            removedSlaveUuids.removeAll(newSlaveUuids);

                            if (removedSlaveUuids.isEmpty()) {
                                trigger.next();
                                return;
                            }

                            List<Tuple> vfNicTuples = SQL.New("select nic.vmInstanceUuid, nic.uuid" +
                                            " from VmVfNicVO nic, HostNetworkInterfaceVO if, EthernetVfPciDeviceVO pci" +
                                            " where nic.pciDeviceUuid = pci.uuid" +
                                            " and pci.interfaceName = if.interfaceName" +
                                            " and pci.hostUuid = if.hostUuid" +
                                            " and if.uuid in (:interfaceUuids)" +
                                            " and pci.l3NetworkUuid in (:l3Uuids)", Tuple.class)
                                    .param("interfaceUuids", removedSlaveUuids)
                                    .param("l3Uuids", l3Uuids)
                                    .list();

                            if (vfNicTuples.isEmpty()) {
                                trigger.next();
                                return;
                            }

                            List<DetachNicFromVmMsg> dmsgs = new ArrayList<>();
                            for (Tuple t : vfNicTuples) {
                                String vmUuid = (String) t.get(0);
                                String nicUuid = (String) t.get(1);
                                DetachNicFromVmMsg dmsg = new DetachNicFromVmMsg();
                                dmsg.setVmInstanceUuid(vmUuid);
                                dmsg.setVmNicUuid(nicUuid);
                                bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, vmUuid);
                                dmsgs.add(dmsg);
                            }

                            new While<>(dmsgs).step((dmsg, comp) -> {
                                bus.send(dmsg, new CloudBusCallBack(comp) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            logger.error(String.format("failed to auto detach vf nic[uuid:%s]" +
                                                    " whose physical interface has been detached from bonds", dmsg.getVmNicUuid()));
                                        } else {
                                            logger.debug(String.format("successfully auto detach vf nic[uuid:%s]" +
                                                    " whose physical interface has been detached from bonds", dmsg.getVmNicUuid()));
                                        }

                                        comp.done();
                                    }
                                });
                            }, 10).run(new WhileDoneCompletion(trigger) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    trigger.next();
                                }
                            });
                        }
                    });
                }

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        HostNetworkBondingInventory bondingInv = (HostNetworkBondingInventory)data.get(HostNetworkBondingConstant.Param.BONDINGINV.toString());
                        bondingInv.getSlaves().forEach(slave ->
                            SQL.New(HostNetworkInterfaceServiceRefVO.class)
                                    .eq(HostNetworkInterfaceServiceRefVO_.interfaceUuid, slave.getUuid())
                                    .hardDelete()
                        );
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

    private void handle(APIAttachNicToBondingMsg msg) {
        APIAttachNicToBondingEvent event = new APIAttachNicToBondingEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                attachNicToBonding(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        event.setInventory(HostNetworkBondingInventory.valueOf(dbf.findByUuid(msg.getBondingUuid(), HostNetworkBondingVO.class)));
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("attach-nic-to-bonding-%s", msg.getUuid());
            }
        });
    }

    private void attachNicToBonding(APIAttachNicToBondingMsg msg, Completion completion) {
        HostNetworkBondingVO bondingVO = dbf.findByUuid(msg.getUuid(), HostNetworkBondingVO.class);
        HostNetworkBondingInventory bondingInv = HostNetworkBondingInventory.valueOf(bondingVO);

        List<HostNetworkInterfaceVO> interfaceVOS = Q.New(HostNetworkInterfaceVO.class).in(HostNetworkInterfaceVO_.uuid, msg.getSlaveUuids()).list();
        List<HostNetworkInterfaceInventory> slaves = HostNetworkInterfaceInventory.valueOf(interfaceVOS);

        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName(String.format("attach-nic-to-bonding-%s-on-host-%s", bondingVO.getBondingName(), bondingVO.getHostUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-to-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        HostNetworkBondingFactory factory = bondMgr.getHostNetworkBondingFactory(msg.getType());
                        factory.attachNicToBonding(bondingInv, slaves, new Completion(trigger) {
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

                flow(new NoRollbackFlow() {
                    String __name__ = "update-bonding-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        Set<String> slaveSet = new HashSet<>(msg.getSlaveUuids());
                        attachSlavesToBonding(bondingVO, slaveSet);
                        trigger.next();
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

    private void handle(APIDetachNicFromBondingMsg msg) {
        APIDetachNicFromBondingEvent event = new APIDetachNicFromBondingEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                detachNicFromBonding(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        event.setInventory(HostNetworkBondingInventory.valueOf(dbf.findByUuid(msg.getBondingUuid(), HostNetworkBondingVO.class)));
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("attach-nic-to-bonding-%s", msg.getUuid());
            }
        });
    }

    private void detachNicFromBonding(APIDetachNicFromBondingMsg msg, Completion completion) {
        HostNetworkBondingVO bondingVO = dbf.findByUuid(msg.getUuid(), HostNetworkBondingVO.class);
        HostNetworkBondingInventory bondingInv = HostNetworkBondingInventory.valueOf(bondingVO);

        List<HostNetworkInterfaceVO> interfaceVOS = Q.New(HostNetworkInterfaceVO.class).in(HostNetworkInterfaceVO_.uuid, msg.getSlaveUuids()).list();
        List<HostNetworkInterfaceInventory> slaves = HostNetworkInterfaceInventory.valueOf(interfaceVOS);

        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName(String.format("detach-nic-from-bonding-%s-on-host-%s", bondingVO.getBondingName(), bondingVO.getHostUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-to-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        HostNetworkBondingFactory factory = bondMgr.getHostNetworkBondingFactory(msg.getType());
                        factory.detachNicFromBonding(bondingInv, slaves, new Completion(trigger) {
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

                flow(new NoRollbackFlow() {
                    String __name__ = "update-bonding-in-db";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        Set<String> slaveSet = new HashSet<>(msg.getSlaveUuids());
                        detachSlavesFromBonding(bondingVO, slaveSet);
                        trigger.next();
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

    private void handle(APIDeleteBondingMsg msg) {
        APIDeleteBondingEvent event = new APIDeleteBondingEvent(msg.getId());

        final String issuer = HostNetworkBondingVO.class.getSimpleName();
        HostNetworkBondingVO vo = dbf.findByUuid(msg.getUuid(), HostNetworkBondingVO.class);
        final List<HostNetworkBondingInventory> ctx = asList(HostNetworkBondingInventory.valueOf(vo));

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-bonding-%s-on-host-%s", vo.getUuid(), vo.getHostUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
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
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.publish(event);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                event.setError(errCode);
                bus.publish(event);
            }
        }).start();
    }

    private void doDeleteBonding(BondingDeletionMsg msg, Completion completion) {
        HostNetworkBondingVO vo = dbf.findByUuid(msg.getBondingUuid(), HostNetworkBondingVO.class);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-bonding-%s-on-host-%s", msg.getBondingUuid(), vo.getHostUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                /* Cascade delete l2
                flow(new NoRollbackFlow() {
                    String __name__ =  String.format("delete-l2-network-of-the-bonding-%s", vo.getBondingName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {

                        List<L2NetworkVO> l2NetworkVos = Q.New(L2NetworkVO.class)
                                .eq(L2NetworkVO_.physicalInterface, vo.getBondingName()).list();
                        new While<>(l2NetworkVos).all((l2, wcomp) -> {
                            DeleteL2NetworkMsg msg = new DeleteL2NetworkMsg();
                            msg.setUuid(l2.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, l2.getUuid());
                            bus.send(msg, new CloudBusCallBack(wcomp) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.info(String.format("delete l2 network[uuid:%s] failed, reason:%s", l2.getUuid(), reply.getError().getDetails()));
                                    }
                                    wcomp.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });
                */

                flow(new NoRollbackFlow() {
                    @Override
                    public boolean skip(Map data) {
                        return msg.isDbOnly();
                    }

                    String __name__ = "apply-to-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        HostNetworkBondingFactory factory = bondMgr.getHostNetworkBondingFactory(vo.getType());
                        factory.deleteBonding(HostNetworkBondingInventory.valueOf(vo), new Completion(trigger) {
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

                flow(new NoRollbackFlow() {
                    String __name__ = "remove-bonding-from-db";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                List<HostNetworkInterfaceVO> interfaceVOs = Q.New(HostNetworkInterfaceVO.class)
                                        .eq(HostNetworkInterfaceVO_.bondingUuid, vo.getUuid())
                                        .list();

                                for (HostNetworkInterfaceVO interfaceVO : interfaceVOs) {
                                    interfaceVO.setBondingUuid(null);
                                    interfaceVO.setInterfaceType(NetworkInterfaceType.noMaster.toString());
                                    interfaceVO.setSlaveActive(Boolean.FALSE);
                                    merge(interfaceVO);

                                    PciDeviceUtils.updatePciDevicePassThroughStateFromDisabledToAvailable(
                                            interfaceVO.getInterfaceName(), interfaceVO.getHostUuid());
                                }

                                remove(vo);
                            }
                        }.execute();
                        
                        trigger.next();
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

    private void handle(UpdateBondingMsg msg) {
        UpdateBondingReply reply = new UpdateBondingReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                updateBonding(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        self = dbf.findByUuid(msg.getBondingUuid(), HostNetworkBondingVO.class);
                        reply.setInventory(getSelfInventory());
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("update-bonding-%s", msg.getUuid());
            }
        });
    }

    private void handle(BondingDeletionMsg msg) {
        BondingDeletionReply reply = new BondingDeletionReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                doDeleteBonding(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-bonding-%s", msg.getBondingUuid());
            }
        });
    }
}
