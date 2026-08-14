package org.zstack.zsv.snapshotgroup;

import com.google.common.collect.Maps;
import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import static org.zstack.core.Platform.operr;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.vm.AttachDataVolumeToVmMsg;
import org.zstack.header.vm.DetachDataVolumeFromVmMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.*;
import org.zstack.storage.snapshot.group.VolumeSnapshotGroupConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @ Author : yh.w
 * @ Date   : Created in 9:58 2023/8/4
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ZsvBeforeRevertSnapshotGroupFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(ZsvBeforeRevertSnapshotGroupFlow.class);

    @Autowired
    DatabaseFacade dbf;
    @Autowired
    CloudBus bus;

    private static String ZsvRevertVolumeDeviceIdSpec = "ZsvRevertVolumeDeviceIdSpec";

    private class RevertVolumeDeviceIdSpec {
        private List<VolumeInventory> attachedVols = Lists.newArrayList();
        private List<VolumeInventory> detachedVols = Lists.newArrayList();
        private Map<String, Integer> currentVolumeDeviceIdMap = Maps.newHashMap();
        private Map<String, Integer> snapShotVolumeDeviceIdMap = Maps.newHashMap();
        private boolean rootVolumeChanged = false;
        private String currentRootVolumeUuid;
        private String snapShotRootVolumeUuid;
        private String vmUuid;
        private List<String> currentVolumeOrder;
        private List<String> snapShotVolumeOrder;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getSnapShotRootVolumeUuid() {
            return snapShotRootVolumeUuid;
        }

        public void setSnapShotRootVolumeUuid(String snapShotRootVolumeUuid) {
            this.snapShotRootVolumeUuid = snapShotRootVolumeUuid;
        }

        public String getCurrentRootVolumeUuid() {
            return currentRootVolumeUuid;
        }

        public void setCurrentRootVolumeUuid(String currentRootVolumeUuid) {
            this.currentRootVolumeUuid = currentRootVolumeUuid;
        }

        public List<VolumeInventory> getAttachedVols() {
            return attachedVols;
        }

        public void setAttachedVols(List<VolumeInventory> attachedVols) {
            this.attachedVols = attachedVols;
        }

        public List<VolumeInventory> getDetachedVols() {
            return detachedVols;
        }

        public void setDetachedVols(List<VolumeInventory> detachedVols) {
            this.detachedVols = detachedVols;
        }

        public Map<String, Integer> getCurrentVolumeDeviceIdMap() {
            return currentVolumeDeviceIdMap;
        }

        public void setCurrentVolumeDeviceIdMap(Map<String, Integer> currentVolumeDeviceIdMap) {
            this.currentVolumeDeviceIdMap = currentVolumeDeviceIdMap;
        }

        public Map<String, Integer> getSnapShotVolumeDeviceIdMap() {
            return snapShotVolumeDeviceIdMap;
        }

        public void setSnapShotVolumeDeviceIdMap(Map<String, Integer> snapShotVolumeDeviceIdMap) {
            this.snapShotVolumeDeviceIdMap = snapShotVolumeDeviceIdMap;
        }

        public boolean isRootVolumeChanged() {
            return rootVolumeChanged;
        }

        public void setRootVolumeChanged(boolean rootVolumeChanged) {
            this.rootVolumeChanged = rootVolumeChanged;
        }

        public List<String> getCurrentVolumeOrder() {
            return currentVolumeOrder;
        }

        public void setCurrentVolumeOrder(List<String> currentVolumeOrder) {
            this.currentVolumeOrder = currentVolumeOrder;
        }

        public List<String> getSnapShotVolumeOrder() {
            return snapShotVolumeOrder;
        }

        public void setSnapShotVolumeOrder(List<String> snapShotVolumeOrder) {
            this.snapShotVolumeOrder = snapShotVolumeOrder;
        }
    }


    private void configVolumeDeviceOrder(Map<String, Integer> volDeviceIdMap, List<String> volumeOrderList) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                LocalDateTime now = LocalDateTime.now();
                volDeviceIdMap.forEach((key, value) -> sql(VolumeVO.class).eq(VolumeVO_.uuid, key).set(VolumeVO_.deviceId, value).update());
                for (int count = volumeOrderList.size(); count > 0; count--) {
                    sql(VolumeVO.class).eq(VolumeVO_.uuid, volumeOrderList.get(volumeOrderList.size() - count))
                            .set(VolumeVO_.lastAttachDate, Timestamp.valueOf(now.plusSeconds(-count))).update();
                }
            }
        }.execute();
    }

    @Override
    public boolean skip(Map data) {
        VolumeSnapshotGroupInventory inventory = (VolumeSnapshotGroupInventory) data.get(VolumeSnapshotGroupConstant.Parmas.SnapshotGroup.toString());
        return inventory == null || inventory.getVolumeSnapshotRefs()
                .stream().anyMatch(ref -> ref.getVolumeType().equals(VolumeType.Memory.toString()));
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        RevertVolumeDeviceIdSpec spec = new RevertVolumeDeviceIdSpec();
        data.put(ZsvRevertVolumeDeviceIdSpec, spec);

        String sgUuid = (String) data.get(VolumeSnapshotGroupConstant.Parmas.SnapshotGroupUuid.toString());
        VolumeSnapshotGroupVO vsg = dbf.findByUuid(sgUuid, VolumeSnapshotGroupVO.class);
        if (vsg == null) {
            return;
        }

        VmInstanceVO vm = dbf.findByUuid(vsg.getVmInstanceUuid(), VmInstanceVO.class);

        AtomicReference<String> ar = new AtomicReference<>();
        // save current device id sort
        spec.setCurrentVolumeDeviceIdMap(vm.getAllDiskVolumes()
                .stream()
                .collect(Collectors.toMap(VolumeVO::getUuid, VolumeVO::getDeviceId)));

        spec.setSnapShotVolumeDeviceIdMap(vsg.getVolumeSnapshotRefs()
                .stream()
                .peek(it -> {
                    // use deviceId = 0 to determine root, because volume type may be changed by SetBootVolumeMsg
                    if (it.getDeviceId() == 0) {
                        ar.set(it.getVolumeUuid());
                    }
                })
                .collect(Collectors.toMap(VolumeSnapshotGroupRefVO::getVolumeUuid, VolumeSnapshotGroupRefVO::getDeviceId)));

        spec.setCurrentRootVolumeUuid(vm.getRootVolumeUuid());
        spec.setSnapShotRootVolumeUuid(ar.get());
        spec.setVmUuid(vm.getUuid());

        Set<String> needToAttachVols = new HashSet<>(spec.getSnapShotVolumeDeviceIdMap().keySet());
        needToAttachVols.removeAll(spec.getCurrentVolumeDeviceIdMap().keySet());
        Set<String> needToDetachVols = new HashSet<>(spec.getCurrentVolumeDeviceIdMap().keySet());
        needToDetachVols.removeAll(spec.getSnapShotVolumeDeviceIdMap().keySet());

        spec.setCurrentVolumeOrder(vm.getAllDiskVolumes().stream()
                .sorted(Comparator.comparing(VolumeVO::getLastAttachDate))
                .map(VolumeVO::getUuid)
                .collect(Collectors.toList()));
        spec.setSnapShotVolumeOrder(vsg.getVolumeSnapshotRefs().stream()
                .sorted(Comparator.comparing(VolumeSnapshotGroupRefVO::getVolumeLastAttachDate))
                .map(VolumeSnapshotGroupRefVO::getVolumeUuid)
                .collect(Collectors.toList()));

        logger.info(String.format("current volumes: %s, snapShotVolumes: %s, needToDetachVolumes: %s, needToAttachVolumes: %s",
                    spec.getCurrentVolumeDeviceIdMap().keySet(), spec.getSnapShotVolumeDeviceIdMap().keySet(), needToDetachVols, needToAttachVols));

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("volume-device-id-revert-flow");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "attach-data-volume-in-order";


                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(needToAttachVols).each((volId, compl) -> {
                            VolumeVO volVo = dbf.findByUuid(volId, VolumeVO.class);
                            if (volVo == null) {
                                compl.addError(operr("volume[uuid:%s] has been deleted", volId));
                                compl.allDone();
                                return;
                            }
                            VolumeInventory volInv = VolumeInventory.valueOf(volVo);
                            AttachDataVolumeToVmMsg amsg = new AttachDataVolumeToVmMsg();
                            amsg.setVmInstanceUuid(vm.getUuid());
                            amsg.setVolume(volInv);
                            bus.makeTargetServiceIdByResourceUuid(amsg, VmInstanceConstant.SERVICE_ID, amsg.getVmInstanceUuid());
                            bus.send(amsg, new CloudBusCallBack(compl) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        spec.getAttachedVols().add(volInv);
                                        compl.done();
                                    } else {
                                        compl.addError(reply.getError());
                                        compl.allDone();
                                    }
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "set-boot-volume";

                    @Override
                    public boolean skip(Map data) {
                        return spec.getCurrentRootVolumeUuid().equals(spec.getSnapShotRootVolumeUuid());
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SetVmBootVolumeMsg smsg = new SetVmBootVolumeMsg();
                        smsg.setVmInstanceUuid(vm.getUuid());
                        smsg.setVolumeUuid(spec.getSnapShotRootVolumeUuid());
                        bus.makeTargetServiceIdByResourceUuid(smsg, VolumeConstant.SERVICE_ID, spec.getSnapShotRootVolumeUuid());
                        bus.send(smsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    spec.setRootVolumeChanged(true);
                                    trigger.next();
                                }
                            }
                        });
                    }
                });


                flow(new NoRollbackFlow() {
                    String __name__ = "detach-current-data-volumes";


                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(needToDetachVols).each((volId, compl) -> {
                            VolumeInventory volInv = VolumeInventory.valueOf(dbf.findByUuid(volId, VolumeVO.class));
                            DetachDataVolumeFromVmMsg amsg = new DetachDataVolumeFromVmMsg();
                            amsg.setVmInstanceUuid(vm.getUuid());
                            amsg.setVolume(volInv);
                            bus.makeTargetServiceIdByResourceUuid(amsg, VmInstanceConstant.SERVICE_ID, amsg.getVmInstanceUuid());
                            bus.send(amsg, new CloudBusCallBack(compl) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        spec.getDetachedVols().add(volInv);
                                        compl.done();
                                    } else {
                                        compl.addError(reply.getError());
                                        compl.allDone();
                                    }
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "config-volume-device-order";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        configVolumeDeviceOrder(spec.getSnapShotVolumeDeviceIdMap(), spec.getSnapShotVolumeOrder());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(trigger) {
                    @Override
                    public void handle(Map data) {
                        trigger.next();
                    }
                });

                error(new FlowErrorHandler(trigger) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        trigger.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        RevertVolumeDeviceIdSpec spec = (RevertVolumeDeviceIdSpec) data.get(ZsvRevertVolumeDeviceIdSpec);
        if (spec == null) {
            trigger.rollback();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("rollback-volume-device-id-revert-flow");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "attach-current-volume";

                    @Override
                    public boolean skip(Map data) {
                        return spec.detachedVols.isEmpty();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(spec.detachedVols).each((volInv, compl) -> {
                            AttachDataVolumeToVmMsg amsg = new AttachDataVolumeToVmMsg();
                            amsg.setVmInstanceUuid(spec.getVmUuid());
                            amsg.setVolume(volInv);
                            bus.makeTargetServiceIdByResourceUuid(amsg, VmInstanceConstant.SERVICE_ID, amsg.getVmInstanceUuid());
                            bus.send(amsg, new CloudBusCallBack(compl) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        compl.done();
                                    } else {
                                        compl.addError(reply.getError());
                                        compl.allDone();
                                    }
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

                flow(new NoRollbackFlow() {
                    String __name__ = "set-boot-volume";

                    @Override
                    public boolean skip(Map data) {
                        return !spec.isRootVolumeChanged();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SetVmBootVolumeMsg smsg = new SetVmBootVolumeMsg();
                        smsg.setVmInstanceUuid(spec.getVmUuid());
                        smsg.setVolumeUuid(spec.getCurrentRootVolumeUuid());
                        bus.makeTargetServiceIdByResourceUuid(smsg, VolumeConstant.SERVICE_ID, spec.getCurrentRootVolumeUuid());
                        bus.send(smsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                trigger.next();
                            }
                        });
                    }
                });


                flow(new NoRollbackFlow() {
                    String __name__ = "detach-snapshot-group-volumes";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        configVolumeDeviceOrder(spec.getCurrentVolumeDeviceIdMap(), spec.getCurrentVolumeOrder());

                        if (!spec.getAttachedVols().isEmpty()) {
                            new While<>(spec.getAttachedVols()).each((volInv, compl) -> {
                                DetachDataVolumeFromVmMsg amsg = new DetachDataVolumeFromVmMsg();
                                amsg.setVmInstanceUuid(spec.getVmUuid());
                                amsg.setVolume(volInv);
                                bus.makeTargetServiceIdByResourceUuid(amsg, VmInstanceConstant.SERVICE_ID, amsg.getVmInstanceUuid());
                                bus.send(amsg, new CloudBusCallBack(compl) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (reply.isSuccess()) {
                                            compl.done();
                                        } else {
                                            compl.addError(reply.getError());
                                            compl.allDone();
                                        }
                                    }
                                });
                            }).run(new WhileDoneCompletion(trigger) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    trigger.next();
                                }
                            });
                        } else {
                            trigger.next();
                        }
                    }
                });

                done(new FlowDoneHandler(trigger) {
                    @Override
                    public void handle(Map data) {
                        trigger.rollback();
                    }
                });

                error(new FlowErrorHandler(trigger) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        trigger.rollback();
                    }
                });
            }
        }).start();
    }
}
