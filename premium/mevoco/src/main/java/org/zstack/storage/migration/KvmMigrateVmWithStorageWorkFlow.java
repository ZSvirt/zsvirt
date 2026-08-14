package org.zstack.storage.migration;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmInstanceExtensionPointEmitter;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.header.allocator.AllocateHostReply;
import org.zstack.header.allocator.DesignatedAllocateHostMsg;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.ReturnHostCapacityMsg;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.MigrateVmOnHypervisorMsg;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeAO;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class KvmMigrateVmWithStorageWorkFlow implements KvmLiveMigrationWorkFlowTemplate {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VmInstanceExtensionPointEmitter extEmitter;
    @Autowired
    private PluginRegistry pluginRgty;

    private CLogger logger = Utils.getLogger(KvmMigrateVmWithStorageWorkFlow.class);

    public static final String STORAGE_MIGRATION_TYPE = "vm_live_migration_with_storage";

    @Override
    public String getKvmStorageLiveMigrationType() {
        return STORAGE_MIGRATION_TYPE;
    }

    StorageLiveMigrateParam param = new StorageLiveMigrateParam();
    class StorageLiveMigrateParam {
        String originHostUuid;
        long cpuCapacity;
        long memoryCapacity;
    }

    @Override
    public List<Flow> getPrepareFlows(ParamIn paramIn) {
        VmInstanceVO vmInstanceVO = dbf.findByUuid(paramIn.getVmInstanceInventory().getUuid(), VmInstanceVO.class);

        List<Flow> flows = new ArrayList<>();
        flows.add(new Flow() {
            String __name__ = "allocate-host-connected-to-ps-" + paramIn.getMsg().getDstPrimaryStorageUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                long size = paramIn.getVolumesToMigrate().stream()
                        .map(VolumeAO::getSize)
                        .reduce(0L, Long::sum);

                param.cpuCapacity = vmInstanceVO.getCpuNum();
                param.memoryCapacity = vmInstanceVO.getMemorySize();
                param.originHostUuid = vmInstanceVO.getHostUuid();

                DesignatedAllocateHostMsg amsg = new DesignatedAllocateHostMsg();
                amsg.setVmInstance(VmInstanceInventory.valueOf(vmInstanceVO));
                amsg.setL3NetworkUuids(VmNicHelper.getL3Uuids(VmNicInventory.valueOf(vmInstanceVO.getVmNics())));
                amsg.setVmNicParams(VmNicHelper.getVmNicParamsWithVfNic(vmInstanceVO));
                amsg.setAllowNoL3Networks(true);
                amsg.setRequiredPrimaryStorageUuids(Collections.singleton(paramIn.getMsg().getDstPrimaryStorageUuid()));
                amsg.addLocationSpec(VmLocationSpec.avoidHost(vmInstanceVO.getHostUuid()));
                amsg.setCpuCapacity(vmInstanceVO.getCpuNum());
                amsg.setMemoryCapacity(vmInstanceVO.getMemorySize());
                amsg.setDiskSize(size);
                amsg.setHostUuid(paramIn.getMsg().getDstHostUuid());
                amsg.setVmOperation(VmInstanceConstant.VmOperation.NewCreate.toString());
                amsg.setAllocatorStrategy(HostAllocatorConstant.MIGRATE_VM_ALLOCATOR_TYPE);
                amsg.setZoneUuid(vmInstanceVO.getZoneUuid());
                amsg.setArchitecture(vmInstanceVO.getArchitecture());

                amsg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
                bus.send(amsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            AllocateHostReply r = reply.castReply();
                            paramIn.setDstHostInventory(r.getHost());
                            paramIn.getJobContext().setDstHostInventory(r.getHost());
                            trigger.next();
                        } else {
                            logger.warn(reply.getError().getDetails());
                            trigger.fail(operr("No host available for block live migration: %s", reply.getError().getDetails()));
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (paramIn.getDstHostInventory() != null) {
                    ReturnHostCapacityMsg rmsg = new ReturnHostCapacityMsg();
                    rmsg.setHostUuid(paramIn.getDstHostInventory().getUuid());
                    rmsg.setCpuCapacity(vmInstanceVO.getCpuNum());
                    rmsg.setMemoryCapacity(vmInstanceVO.getMemorySize());
                    bus.makeLocalServiceId(rmsg, HostAllocatorConstant.SERVICE_ID);
                    bus.send(rmsg);
                }

                trigger.rollback();
            }
        });

        flows.add(new Flow() {
            String __name__ = "call-pre-migrate-extension-point";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmInstanceVO vmvo = dbf.findByUuid(paramIn.getVmInstanceInventory().getUuid(), VmInstanceVO.class);
                VmInstanceInventory vmInstanceInventory = VmInstanceInventory.valueOf(vmvo);
                List<VmInstanceMigrateExtensionPoint> migrateVmExtensions = pluginRgty.getExtensionList(VmInstanceMigrateExtensionPoint.class);
                // FIXME migration with storage do not match migrate extension point
                new While<>(migrateVmExtensions).each((ext, comp) -> ext.preMigrateVm(vmInstanceInventory, paramIn.getDstHostInventory().getUuid(), new Completion(comp) {
                    @Override
                    public void success() {
                        comp.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        comp.done();
                    }
                })).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                VmInstanceVO vmvo = dbf.findByUuid(paramIn.getVmInstanceInventory().getUuid(), VmInstanceVO.class);
                VmInstanceInventory vmInstanceInventory = VmInstanceInventory.valueOf(vmvo);
                extEmitter.failedToMigrateVm(vmInstanceInventory, paramIn.getDstHostInventory().getUuid(), trigger.getErrorCode(), new NoErrorCompletion() {
                    @Override
                    public void done() {
                        trigger.rollback();
                    }
                });
            }
        });

        return flows;
    }

    @Override
    public List<Flow> getPreMigrateFlows(ParamIn paramIn) {
        List<Flow> flows = new ArrayList<>();
        flows.add(new Flow() {
            String __name__ = "prepare-existing-block-migration-for-vm-" + paramIn.getMsg().getVmInstanceUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(pluginRgty.getExtensionList(VolumeMigrateExtensionPoint.class)).each((ext, comp) -> {
                    ext.preMigrateVolumes(paramIn.getVolumesToKeep(), paramIn.getVmInstanceInventory().getHostUuid(), paramIn.getDstHostInventory().getUuid(), new Completion(comp) {
                        @Override
                        public void success() {
                            comp.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            comp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                new While<>(pluginRgty.getExtensionList(VolumeMigrateExtensionPoint.class)).each((ext, comp) -> {
                    ext.failedToMigrateVolumes(paramIn.getVolumesToKeep(), paramIn.getVmInstanceInventory().getHostUuid(), paramIn.getDstHostInventory().getUuid(), new Completion(comp) {
                                @Override
                                public void success() {
                                    comp.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    comp.done();
                                }
                            });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.rollback();
                    }
                });
            }
        });

        return flows;
    }

    @Override
    public List<Flow> getMigrateFlows(ParamIn paramIn) {
        List<Flow> flows = new ArrayList<>();
        flows.add(new NoRollbackFlow() {
            String __name__ = String.format("kvm-block-live-migration-vm-%s-to-host", paramIn.getVmInstanceInventory().getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                SQL.New(VolumeVO.class)
                        .in(VolumeVO_.uuid, paramIn.getVolumesToMigrate().stream().map(VolumeVO::getUuid).collect(Collectors.toList()))
                        .set(VolumeVO_.status, VolumeStatus.Migrating)
                        .update();

                Map<String, String> migrateDisksMap = KvmStorageLiveMigrationFlowChain.getMigrateDisks(paramIn.getVolumeMappingDict(), paramIn.getVolumesToMigrate(), paramIn.getInitializedVolumes());
                MigrateVmOnHypervisorMsg mmsg = new MigrateVmOnHypervisorMsg();
                mmsg.setReload(paramIn.getJobContext().isMigrateStarted());
                mmsg.setDestHostInventory(paramIn.getDstHostInventory());
                mmsg.setDiskMigrationMap(migrateDisksMap);
                mmsg.setMigrateFromDestination(false);
                mmsg.setSrcHostUuid(paramIn.getVmInstanceInventory().getHostUuid());
                mmsg.setStrategy(paramIn.getMsg().getStrategy());
                mmsg.setDownTime(paramIn.getMsg().getDownTime());
                mmsg.setVmInventory(paramIn.getVmInstanceInventory());
                mmsg.setStorageMigrationPolicy(MigrateVmOnHypervisorMsg.StorageMigrationPolicy.FullCopy);
                mmsg.setBandwidth((long) Math.ceil((double) paramIn.getTotalBandwidth() / paramIn.getVolumesToMigrate().size()));
                bus.makeTargetServiceIdByResourceUuid(mmsg, HostConstant.SERVICE_ID, mmsg.getHostUuid());
                bus.send(mmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        SQL.New(VolumeVO.class)
                                .in(VolumeVO_.uuid, paramIn.getVolumesToMigrate().stream().map(VolumeVO::getUuid).collect(Collectors.toList()))
                                .set(VolumeVO_.status, VolumeStatus.Ready)
                                .update();

                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        paramIn.setMigrateSucceeded(true);
                        trigger.next();
                    }
                });
                paramIn.getJobContext().setMigrateStarted(true);
                LongJobContextUtil.saveContext(paramIn.getMsg().getLongJobUuid(), paramIn.getJobContext());
            }
        });
        return flows;
    }

    @Override
    public List<Flow> getAfterMigrateFlows(ParamIn paramIn) {
        List<Flow> flows = new ArrayList<>();
        flows.add(new NoRollbackFlow() {
            String __name__ = "after-existing-block-migration-for-vm-" + paramIn.getVmInstanceInventory().getUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(pluginRgty.getExtensionList(VolumeMigrateExtensionPoint.class)).each((ext, comp) -> {
                    ext.afterMigrateVolumes(paramIn.getVolumesToKeep(), paramIn.getVmInstanceInventory().getHostUuid(), paramIn.getDstHostInventory().getUuid(), new Completion(comp) {
                        @Override
                        public void success() {
                            comp.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            comp.done();
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

        return flows;
    }

    @Override
    public List<Flow> getCompleteFlows(ParamIn paramIn) {
        List<Flow> flows = new ArrayList<>();
        flows.add(new NoRollbackFlow() {
            String __name__ = "return-capacity-to-src-host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ReturnHostCapacityMsg rmsg = new ReturnHostCapacityMsg();
                rmsg.setCpuCapacity(param.cpuCapacity);
                rmsg.setMemoryCapacity(param.memoryCapacity);
                rmsg.setHostUuid(param.originHostUuid);
                bus.makeLocalServiceId(rmsg, HostAllocatorConstant.SERVICE_ID);
                bus.send(rmsg);
                trigger.next();
            }
        });
        flows.add(new Flow() {
            String __name__ = String.format("post-migration-for-vm-%s", paramIn.getVmInstanceInventory().getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String detHostUuid = paramIn.getDstHostInventory() != null ? paramIn.getDstHostInventory().getUuid() : null;
                extEmitter.postMigrateVm(paramIn.getVmInstanceInventory(), detHostUuid, new Completion(trigger) {
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

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        });
        flows.add(new NoRollbackFlow() {
            String __name__ = "call-after-migrate-extension-point";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmInstanceVO vmvo = dbf.findByUuid(paramIn.getVmInstanceInventory().getUuid(), VmInstanceVO.class);
                VmInstanceInventory vmInstanceInventory = VmInstanceInventory.valueOf(vmvo);
                extEmitter.afterMigrateVm(vmInstanceInventory, vmInstanceInventory.getLastHostUuid(), new NoErrorCompletion() {
                    @Override
                    public void done() {
                        trigger.next();
                    }
                });
            }
        });

        return flows;
    }

    @Override
    public ErrorCode preCheck(ParamIn paramIn) {
        return null;
    }
}
