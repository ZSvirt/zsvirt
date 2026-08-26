package org.zstack.compute.vm;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.ReturnHostCapacityMsg;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.*;
import org.zstack.header.vo.ResourceVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Create by lining at 2020/08/17
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateHostAndPrimaryStorageFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmAllocateHostAndPrimaryStorageFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        if (spec.getImageSpec().relyOnImageCache()) {
            String imageUuid = spec.getImageSpec().getInventory().getUuid();
            List<String> requirdPsUuids = spec.getCandidatePrimaryStorageUuidsForRootVolume();
            List<String> cachedPsUuids = Q.New(ImageCacheVO.class).select(ImageCacheVO_.primaryStorageUuid)
                    .eq(ImageCacheVO_.imageUuid, imageUuid)
                    .listValues();
            if (!CollectionUtils.isEmpty(requirdPsUuids) && Collections.disjoint(requirdPsUuids, cachedPsUuids)) {
                trigger.fail(operr("creation rely on image cache[uuid:%s, locate ps uuids: [%s]], cannot create other places.", imageUuid, cachedPsUuids));
                return;
            } else if (!CollectionUtils.isEmpty(requirdPsUuids)) {
                requirdPsUuids.retainAll(cachedPsUuids);
            }
        }

        // The creation parameter specifies the primary storage, no need to automatically allocate the primary storage
        if (rootVolumePsUnique(spec) && (!needCreateDataVolume(spec) || dataVolumePsUnique(spec))) {
            allocate(trigger, spec);
            return;
        }

        List<String> possibleClusterUuids = getPossibleClusterUuids(spec);
        List<String> possiblePsUuids = getPossiblePrimaryStorageUuids(spec);

        spec.addLocationSpec(VmLocationSpec.onlyAllowedCluster(possibleClusterUuids));
        // Multiple clusters, and each cluster has a different primary storage
        // Do not automatically allocate the primary storage, specifying primary storage impacts cluster selection
        if (possibleClusterUuids.size() > 1) {
            boolean clusterWithSamePs = true;
            for (String clusterUuid : possibleClusterUuids) {
                List<String> psUuids = Q.New(PrimaryStorageClusterRefVO.class)
                        .select(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                        .eq(PrimaryStorageClusterRefVO_.clusterUuid, clusterUuid)
                        .listValues();
                if (!psUuids.containsAll(possiblePsUuids)) {
                    clusterWithSamePs = false;
                    break;
                }
            }

            if (!clusterWithSamePs) {
                allocate(trigger, spec);
                return;
            }
        }

        // Not local + non-local，no need to automatically allocate the primary storage
        if (!isMixPrimaryStorage(spec)) {
            allocate(trigger, spec);
            return;
        }

        List<Tuple> availablePsTuples = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.uuid, PrimaryStorageVO_.type)
                .in(PrimaryStorageVO_.uuid, possiblePsUuids)
                .eq(PrimaryStorageVO_.state, PrimaryStorageState.Enabled)
                .eq(PrimaryStorageVO_.status, PrimaryStorageStatus.Connected)
                .listTuple();

        List<String> availablePsUuids = new ArrayList<>();
        List<String> localPsUuids = new ArrayList<>();
        List<String> nonLocalPsUuids = new ArrayList<>();
        for (Tuple tuple : availablePsTuples) {
            String psUuid = (String)tuple.get(0);
            String psType = (String)tuple.get(1);
            availablePsUuids.add((String)tuple.get(0));

            if (psType.equals(PrimaryStorageConstants.LOCAL_STORAGE_TYPE)) {
                localPsUuids.add(psUuid);
            } else {
                nonLocalPsUuids.add(psUuid);
            }
        }

        if (availablePsUuids.isEmpty()) {
            allocate(trigger, spec);
            return;
        }

        if (!CollectionUtils.isEmpty(spec.getCandidatePrimaryStorageUuidsForRootVolume())) {
            List<String> filterPsUuids = spec.getCandidatePrimaryStorageUuidsForRootVolume().stream().filter(availablePsUuids::contains).collect(Collectors.toList());
            if (filterPsUuids.isEmpty()) {
                trigger.fail(Platform.operr(String.format("none of the specified primary storages%s are available",  spec.getCandidatePrimaryStorageUuidsForRootVolume())));
                return;
            }
            spec.setCandidatePrimaryStorageUuidsForRootVolume(filterPsUuids);
        }
        if (needCreateDataVolume(spec) && !CollectionUtils.isEmpty(spec.getCandidatePrimaryStorageUuidsForDataVolume())) {
            List<String> filterPsUuids = spec.getCandidatePrimaryStorageUuidsForDataVolume().stream().filter(availablePsUuids::contains).collect(Collectors.toList());
            if (filterPsUuids.isEmpty()) {
                trigger.fail(Platform.operr(String.format("none of the specified primary storages%s are available",  spec.getCandidatePrimaryStorageUuidsForDataVolume())));
                return;
            }
            spec.setCandidatePrimaryStorageUuidsForDataVolume(filterPsUuids);
        }

        // local + non-local， need to automatically allocate the primary storage
        List<List<String>> psCombos = getPrimaryStorageCombinationFromSpec(spec, localPsUuids, nonLocalPsUuids);
        new While<>(psCombos).each((combo, whileCompletion) -> {
            spec.setRequiredPrimaryStorageUuidForRootVolume(combo.get(0));
            spec.setRequiredPrimaryStorageUuidForDataVolume(combo.get(1));

            FlowChain chain = buildAllocateHostAndPrimaryStorageFlowChain(trigger, spec);
            chain.done(new FlowDoneHandler(whileCompletion) {
                @Override
                public void handle(Map data) {
                    whileCompletion.allDone();
                }
            }).error(new FlowErrorHandler(whileCompletion) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    whileCompletion.addError(errCode);
                    whileCompletion.done();
                }
            }).start();
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().size() == psCombos.size()) {
                    trigger.fail(errorCodeList.getCauses().get(0));
                    return;
                }

                trigger.next();
            }
        });
    }

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        FlowChain rollbackChain = FlowChainBuilder.newShareFlowChain();
        rollbackChain.setName(String.format("rollback-allocate-host-and-ps-for-vm-%s", spec.getVmInventory().getUuid()));
        rollbackChain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        rollbackChain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                        for (VmInstanceSpec.VolumeSpec vspec : spec.getVolumeSpecs()) {
                            if (vspec.isVolumeCreated()) {
                                // don't return capacity as it has been returned when the volume is deleted
                                continue;
                            }

                            IncreasePrimaryStorageCapacityMsg msg = new IncreasePrimaryStorageCapacityMsg();
                            msg.setDiskSize(vspec.getSize());
                            msg.setPrimaryStorageUuid(vspec.getPrimaryStorageInventory().getUuid());
                            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, vspec.getPrimaryStorageInventory().getUuid());
                            bus.send(msg);
                        }
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                        HostInventory host = spec.getDestHost();

                        // if ChangeImage, then no need to ReturnHostCapacity, and resume vm info
                        if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.ChangeImage) {
                            VmInstanceVO vmvo = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
                            vmvo.setClusterUuid(spec.getVmInventory().getClusterUuid());
                            vmvo.setLastHostUuid(spec.getVmInventory().getLastHostUuid());
                            vmvo.setHypervisorType(spec.getVmInventory().getHypervisorType());
                            dbf.update(vmvo);
                        } else if (host != null) {
                            ReturnHostCapacityMsg msg = new ReturnHostCapacityMsg();
                            msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
                            msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
                            msg.setHostUuid(host.getUuid());
                            msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
                            bus.send(msg);
                        }

                        extEmitter.cleanUpAfterVmFailedToStart(spec.getVmInventory(), spec.getCurrentVmOperation());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(chain) {
                    @Override
                    public void handle(Map data) {
                        chain.rollback();
                    }
                });

                error(new FlowErrorHandler(chain) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        chain.rollback();
                    }
                });
            }
        }).start();
    }

    private void setFlowMarshaller(FlowChain chain) {
        chain.setFlowMarshaller(new FlowMarshaller() {
            @Override
            public Flow marshalTheNextFlow(String previousFlowClassName, String nextFlowClassName, FlowChain chain, Map data) {
                Flow nflow = null;
                for (MarshalVmOperationFlowExtensionPoint mext : pluginRgty.getExtensionList(MarshalVmOperationFlowExtensionPoint.class)) {
                    VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                    nflow = mext.marshalVmOperationFlow(previousFlowClassName, nextFlowClassName, chain, spec);
                    if (nflow != null) {
                        logger.debug(String.format("a VM[uuid: %s, operation: %s] operation flow[%s] is changed to the flow[%s] by %s",
                                spec.getVmInventory().getName(), spec.getCurrentVmOperation(), nextFlowClassName, nflow.getClass(), mext.getClass()));
                        break;
                    }
                }

                return nflow;
            }
        });
    }

    // local + non-local
    private boolean isMixPrimaryStorage(VmInstanceSpec spec) {
        List<String> psUuids = getPossiblePrimaryStorageUuids(spec);
        if (psUuids.size() < 2) {
            return false;
        }

        List<String> psTypes = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.type)
                .in(PrimaryStorageVO_.uuid, psUuids)
                .groupBy(PrimaryStorageVO_.type)
                .listValues();
        return !(!psTypes.contains(PrimaryStorageConstants.LOCAL_STORAGE_TYPE) || psTypes.size() <= 1);
    }

    private List<String> getPossibleClusterUuids(VmInstanceSpec spec) {
        VmInstanceInventory vm = spec.getVmInventory();
        String hostUuid = spec.getRequiredHostUuid();

        String clusterUuid = vm.getClusterUuid() != null ? vm.getClusterUuid() : spec.getRequiredClusterUuid();
        if (clusterUuid != null) {
            return Collections.singletonList(clusterUuid);
        }

        if (hostUuid != null) {
            clusterUuid = Q.New(HostVO.class)
                    .select(HostVO_.clusterUuid)
                    .eq(HostVO_.uuid, hostUuid)
                    .findValue();
            return Collections.singletonList(clusterUuid);
        }

        String l3Uuid = vm.getDefaultL3NetworkUuid();
        String zoneUuid = vm.getZoneUuid();

        Q q = Q.New(ClusterVO.class).select(ClusterVO_.uuid);
        if (zoneUuid != null) {
            q.eq(ClusterVO_.zoneUuid, zoneUuid);
        }
        if (spec.getArchitecture() != null) {
            q.eq(ClusterVO_.architecture, spec.getArchitecture().name());
        }

        List<String> possibleClusterUuids = q.listValues();
        List<String> needClusterUuids;
        if (!CollectionUtils.isEmpty(spec.getCandidatePrimaryStorageUuidsForRootVolume())) {
            needClusterUuids = Q.New(PrimaryStorageClusterRefVO.class)
                    .select(PrimaryStorageClusterRefVO_.clusterUuid)
                    .in(PrimaryStorageClusterRefVO_.primaryStorageUuid, spec.getCandidatePrimaryStorageUuidsForRootVolume())
                    .listValues();
            possibleClusterUuids.retainAll(needClusterUuids);
        }
        if (!CollectionUtils.isEmpty(spec.getCandidatePrimaryStorageUuidsForDataVolume())) {
            needClusterUuids = Q.New(PrimaryStorageClusterRefVO.class)
                    .select(PrimaryStorageClusterRefVO_.clusterUuid)
                    .in(PrimaryStorageClusterRefVO_.primaryStorageUuid, spec.getCandidatePrimaryStorageUuidsForDataVolume())
                    .listValues();
            possibleClusterUuids.retainAll(needClusterUuids);
        }

        if (possibleClusterUuids.size() < 2) {
            return possibleClusterUuids;
        }

        if (l3Uuid != null) {
            String l2Uuid = Q.New(L3NetworkVO.class)
                    .select(L3NetworkVO_.l2NetworkUuid)
                    .eq(L3NetworkVO_.uuid, l3Uuid)
                    .findValue();
            needClusterUuids = Q.New(L2NetworkClusterRefVO.class)
                    .select(L2NetworkClusterRefVO_.clusterUuid)
                    .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2Uuid)
                    .listValues();
            possibleClusterUuids.retainAll(needClusterUuids);
        }

        return possibleClusterUuids;
    }

    private List<String> getPossiblePrimaryStorageUuids(VmInstanceSpec spec) {
        List<String> clusterUuids = getPossibleClusterUuids(spec);
        if (clusterUuids.isEmpty()) {
            return Collections.emptyList();
        }

        return SQL.New("select distinct(t0.uuid) from PrimaryStorageVO t0, PrimaryStorageClusterRefVO t1" +
                " where t0.uuid = t1.primaryStorageUuid" +
                " and t1.clusterUuid in (:clusterUuids)", String.class)
                .param("clusterUuids", clusterUuids)
                .list();
    }


    private boolean rootVolumePsUnique(VmInstanceSpec spec) {
        return spec.getCandidatePrimaryStorageUuidsForRootVolume().size() == 1;
    }

    private boolean dataVolumePsUnique(VmInstanceSpec spec) {
        return spec.getCandidatePrimaryStorageUuidsForDataVolume().size() == 1;
    }

    private boolean needCreateDataVolume(VmInstanceSpec spec) {
        return !CollectionUtils.isEmpty(spec.getNonTemplateDeprecatedDisksSpecs());
    }

    private FlowChain buildAllocateHostAndPrimaryStorageFlowChain(final FlowTrigger trigger, VmInstanceSpec spec) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("allocate-host-and-ps-for-vm-%s", spec.getVmInventory().getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new VmAllocateHostFlow());

                flow(new VmAllocatePrimaryStorageFlow());
            }
        });
        setFlowMarshaller(chain);

        return chain;
    }

    private void allocate(final FlowTrigger trigger, VmInstanceSpec spec) {
        FlowChain chain = buildAllocateHostAndPrimaryStorageFlowChain(trigger, spec);
        chain.done(new FlowDoneHandler(trigger) {
            @Override
            public void handle(Map data) {
                trigger.next();
            }
        }).error(new FlowErrorHandler(trigger) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                trigger.fail(errCode);
            }
        });
        chain.start();
    }

    private List<List<String>> getPrimaryStorageCombinationFromSpec(VmInstanceSpec spec, List<String> localPsUuids, List<String> nonLocalPsUuids) {
        List<String> availPsForRootVolume = new ArrayList<String>();
        availPsForRootVolume.addAll(localPsUuids);
        availPsForRootVolume.addAll(nonLocalPsUuids);

        List<String> availPsForDataVolume = new ArrayList<String>();
        availPsForDataVolume.addAll(nonLocalPsUuids);
        availPsForDataVolume.addAll(localPsUuids);

        boolean autoAllocateRootVolumePs = false;
        boolean autoAllocateDataVolumePs = false;
        List<String> rootPs = new ArrayList<>();
        List<String> dataPs = new ArrayList<>();
        if (!CollectionUtils.isEmpty(spec.getCandidatePrimaryStorageUuidsForRootVolume())) {
            rootPs.addAll(spec.getCandidatePrimaryStorageUuidsForRootVolume());
        } else {
            autoAllocateRootVolumePs = true;
            rootPs.addAll(availPsForRootVolume);
        }

        String rootVolumeStrategy = spec.getRootDiskOffering() == null ? null : spec.getRootDiskOffering().getAllocatorStrategy();
        sortPrimaryStorages(rootPs, rootVolumeStrategy, spec.getImageSpec());

        if (needCreateDataVolume(spec)) {
            if (!CollectionUtils.isEmpty(spec.getCandidatePrimaryStorageUuidsForDataVolume())) {
                dataPs.addAll(spec.getCandidatePrimaryStorageUuidsForDataVolume());
            } else {
                autoAllocateDataVolumePs = true;
                dataPs.addAll(availPsForDataVolume);
            }
            sortPrimaryStorages(dataPs, PrimaryStorageConstant.DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE, null);
        } else {
            dataPs.add(null);
        }
        filterPrimaryStorageCandidates(spec, rootPs, autoAllocateRootVolumePs);

        List<List<String>> finalPsCombos = new ArrayList<>();
        if (autoAllocateRootVolumePs && autoAllocateDataVolumePs) {
            // First priority：root local, data non-local
            // Second priority：root local, data local / root non-local, data non-local
            // Third priority：root non-local, data local
            List<String> tmpLocalPsUuids = rootPs.stream().filter(localPsUuids::contains).collect(Collectors.toList());
            List<String> tmpNonLocalPsUuids = rootPs.stream().filter(nonLocalPsUuids::contains).collect(Collectors.toList());
            rootPs.clear();
            rootPs.addAll(tmpLocalPsUuids);
            rootPs.addAll(tmpNonLocalPsUuids);

            tmpLocalPsUuids = dataPs.stream().filter(localPsUuids::contains).collect(Collectors.toList());
            tmpNonLocalPsUuids = dataPs.stream().filter(nonLocalPsUuids::contains).collect(Collectors.toList());
            dataPs.clear();
            dataPs.addAll(tmpNonLocalPsUuids);
            dataPs.addAll(tmpLocalPsUuids);
        }

        rootPs.forEach(r -> dataPs.forEach(d -> finalPsCombos.add(Arrays.asList(r, d))));
        return finalPsCombos;
    }

    private void filterPrimaryStorageCandidates(VmInstanceSpec spec, List<String> rootPs, boolean rootAutoAllocation) {
        for (VmAllocatePrimaryStorageExtensionPoint ext :
                pluginRgty.getExtensionList(VmAllocatePrimaryStorageExtensionPoint.class)) {
            ext.filterPrimaryStorageCandidates(spec, rootPs, rootAutoAllocation);
        }
    }

    private void sortPrimaryStorages(final List<String> psUuids, String strategy, VmInstanceSpec.ImageSpec imageSpec) {
        List<PrimaryStorageVO> primaryStorageVOS = psUuids.stream().map(uuid -> dbf.findByUuid(uuid, PrimaryStorageVO.class)).collect(Collectors.toList());
        for (PrimaryStorageSortExtensionPoint ext : pluginRgty.getExtensionList(PrimaryStorageSortExtensionPoint.class)) {
            ext.sort(primaryStorageVOS, imageSpec, strategy);
        }
        psUuids.clear();
        psUuids.addAll(primaryStorageVOS.stream().map(ResourceVO::getUuid).collect(Collectors.toList()));
    }
}
