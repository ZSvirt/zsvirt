package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.allocator.*;
import org.zstack.header.allocator.datatypes.CpuMemoryCapacityData;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.cluster.ReportHostCapacityMessage;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.PrimaryStorageType;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmAbnormalLifeCycleExtensionPoint;
import org.zstack.header.vm.VmAbnormalLifeCycleStruct;
import org.zstack.header.vm.VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.zone.ZoneVO;
import org.zstack.query.QueryFacade;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

public class HostAllocatorManagerImpl extends AbstractService implements HostAllocatorManager, VmAbnormalLifeCycleExtensionPoint {
    private static final CLogger logger = Utils.getLogger(HostAllocatorManagerImpl.class);

    private Map<String, HostAllocatorStrategyFactory> factories = Collections.synchronizedMap(new HashMap<String, HostAllocatorStrategyFactory>());
    private Set<String> unsupportedVmTypeForCapacityCalculation = new HashSet<>();
    private Map<String, List<String>> backupStoragePrimaryStorageMetrics;
    private Map<String, List<String>> primaryStorageBackupStorageMetrics = new HashMap<>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private HostCapacityReserveManager reserveMgr;
    @Autowired
    private HostCapacityOverProvisioningManager ratioMgr;
    @Autowired
    private HostCpuOverProvisioningManager cpuRatioMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private QueryFacade qf;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof AllocateHostMsg) {
            handle((AllocateHostMsg) msg);
        } else if (msg instanceof ReportHostCapacityMessage) {
            handle((ReportHostCapacityMessage) msg);
        } else if (msg instanceof ReturnHostCapacityMsg) {
            handle((ReturnHostCapacityMsg) msg);
        } else if (msg instanceof RecalculateHostCapacityMsg) {
            handle((RecalculateHostCapacityMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Transactional(readOnly = true)
    private List<BackupStorageInventory> getBackupStorageFromPs(String psUuid, List<String> backupStorageTypes) {
        String sql = "select bs from BackupStorageVO bs, BackupStorageZoneRefVO ref, PrimaryStorageVO ps" +
                " where bs.uuid = ref.backupStorageUuid and ps.zoneUuid = ref.zoneUuid and ps.uuid = :psUuid" +
                " and bs.type in (:bsTypes) and bs.state = :bsState and bs.status = :bsStatus";

        TypedQuery<BackupStorageVO> q = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
        q.setParameter("psUuid", psUuid);
        q.setParameter("bsTypes", backupStorageTypes);
        q.setParameter("bsState", BackupStorageState.Enabled);
        q.setParameter("bsStatus", BackupStorageStatus.Connected);

        List<BackupStorageInventory> candidates = BackupStorageInventory.valueOf(q.getResultList());
        for (BackupStorageAllocatorFilterExtensionPoint ext : pluginRgty.getExtensionList(BackupStorageAllocatorFilterExtensionPoint.class)) {
            candidates = ext.filterBackupStorageCandidatesByPS(candidates, psUuid);
        }
        return candidates;
    }

    @Transactional(readOnly = true)
    private void handle(APIGetCandidateBackupStorageForCreatingImageMsg msg) {
        PrimaryStorageVO ps;
        if (msg.getVolumeUuid() != null) {
            String sql = "select ps from PrimaryStorageVO ps, VolumeVO vol where ps.uuid = vol.primaryStorageUuid" +
                    " and vol.uuid = :uuid";
            TypedQuery<PrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            q.setParameter("uuid", msg.getVolumeUuid());
            List<PrimaryStorageVO> pss = q.getResultList();
            ps = pss.isEmpty() ? null : pss.get(0);
        } else if (msg.getVolumeSnapshotUuid() != null) {
            String sql = "select ps from PrimaryStorageVO ps, VolumeSnapshotVO sp where ps.uuid = sp.primaryStorageUuid" +
                    " and sp.uuid = :uuid";
            TypedQuery<PrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            q.setParameter("uuid", msg.getVolumeSnapshotUuid());
            List<PrimaryStorageVO> pss = q.getResultList();
            ps = pss.isEmpty() ? null : pss.get(0);
        } else {
            throw new CloudRuntimeException("cannot be there");
        }

        if (ps == null) {
            throw new CloudRuntimeException("cannot find primary storage");
        }

        List<String> backupStorageTypes = getBackupStorageTypesByPrimaryStorageTypeFromMetrics(ps.getType());

        List<BackupStorageInventory> candidates = getBackupStorageFromPs(ps.getUuid(),backupStorageTypes);

        APIGetCandidateBackupStorageForCreatingImageReply reply = new APIGetCandidateBackupStorageForCreatingImageReply();
        reply.setInventories(candidates);
        bus.reply(msg, reply);
    }

    private void handle(APIGetCandidateImagesForCreatingVmMsg msg) {
        PrimaryStorageVO ps = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid()).find();
        List<String> backupStorageTypes = getBackupStorageTypesByPrimaryStorageTypeFromMetrics(ps.getType());

        APIGetCandidateImagesForCreatingVmReply reply = new APIGetCandidateImagesForCreatingVmReply();
        reply.setInventories(ImageInventory.valueOf(getCandidateImages(ps.getUuid(), backupStorageTypes)));
        bus.reply(msg, reply);
    }

    @Transactional(readOnly = true)
    private List<ImageVO> getCandidateImages(String psUuid, List<String> backupStorageTypes) {
        List<BackupStorageInventory> backupStorageInventories = getBackupStorageFromPs(psUuid, backupStorageTypes);
        String sql2 = "select image from ImageVO image,ImageBackupStorageRefVO ref,BackupStorageVO bs where" +
                " image.uuid=ref.imageUuid and ref.backupStorageUuid=bs.uuid and" +
                " image.state=:imageState and image.status=:imageStatus and" +
                " bs.uuid in (:backupStorageUuids)";
        return SQL.New(sql2)
                .param("imageState", ImageState.Enabled)
                .param("imageStatus", ImageStatus.Ready)
                .param("backupStorageUuids", backupStorageInventories.stream().map(BackupStorageInventory::getUuid).collect(Collectors.toList()))
                .list();
    }

    private void handle(RecalculateHostCapacityMsg msg) {
        final List<String> hostUuids = new ArrayList<>();
        if (msg.getHostUuid() != null) {
            hostUuids.add(msg.getHostUuid());
        } else if (msg.getClusterUuid() != null) {
            hostUuids.addAll(Q.New(HostVO.class).select(HostVO_.uuid)
                    .eq(HostVO_.clusterUuid, msg.getClusterUuid())
                    .listValues());
        } else {
            SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
            q.select(HostVO_.uuid);
            q.add(HostVO_.zoneUuid, Op.EQ, msg.getZoneUuid());
            hostUuids.addAll(q.listValue());
        }

        if (hostUuids.isEmpty()) {
            return;
        }

        class HostUsedCpuMem {
            String hostUuid;
            Long usedMemory;
            Long usedCpu;
        }

        List<HostUsedCpuMem> hostUsedCpuMemList = new Callable<List<HostUsedCpuMem>>() {
            @Override
            @Transactional(readOnly = true)
            public List<HostUsedCpuMem> call() {
                String sql = "select sum(vm.memorySize), vm.hostUuid, sum(vm.cpuNum)" +
                        " from VmInstanceVO vm" +
                        " where vm.hostUuid in (:hostUuids)" +
                        " and vm.state not in (:vmStates)";

                if (!unsupportedVmTypeForCapacityCalculation.isEmpty()) {
                    sql += " and vm.type not in (:vmTypes)";
                }

                sql += " group by vm.hostUuid";
                TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("hostUuids", hostUuids);
                q.setParameter("vmStates", list(
                        VmInstanceState.Destroyed,
                        VmInstanceState.Created,
                        VmInstanceState.Destroying,
                        VmInstanceState.Stopped));

                if (!unsupportedVmTypeForCapacityCalculation.isEmpty()) {
                    q.setParameter("vmTypes", unsupportedVmTypeForCapacityCalculation);
                }

                List<Tuple> ts = q.getResultList();

                List<HostUsedCpuMem> ret = new ArrayList<>();
                for (Tuple t : ts) {
                    HostUsedCpuMem s = new HostUsedCpuMem();
                    s.hostUuid = t.get(1, String.class);

                    if (t.get(0, Long.class) == null) {
                        continue;
                    }

                    s.usedMemory = ratioMgr.calculateMemoryByRatio(s.hostUuid, t.get(0, Long.class));
                    s.usedCpu = t.get(2, Long.class);
                    ret.add(s);
                }
                return ret;
            }
        }.call();

        List<String> hostHasVms = transformAndRemoveNull(hostUsedCpuMemList, arg -> arg.hostUuid);

        hostUuids.stream().filter(huuid -> !hostHasVms.contains(huuid)).forEach(huuid -> {
            HostUsedCpuMem s = new HostUsedCpuMem();
            s.hostUuid = huuid;
            hostUsedCpuMemList.add(s);
        });

        for (final HostUsedCpuMem s : hostUsedCpuMemList) {
            new HostCapacityUpdater(s.hostUuid).run(cap -> {
                long before = cap.getAvailableMemory();
                long avail = s.usedMemory == null ? cap.getTotalMemory() : cap.getTotalMemory() - s.usedMemory;
                cap.setAvailableMemory(avail);

                long totalCpu = cpuRatioMgr.calculateHostCpuByRatio(s.hostUuid, cap.getCpuNum());
                long totalCpuBefore = cap.getTotalCpu();
                cap.setTotalCpu(totalCpu);

                long beforeCpu = cap.getAvailableCpu();
                long availCpu = s.usedCpu == null ? cap.getTotalCpu() : cap.getTotalCpu() - s.usedCpu;
                cap.setAvailableCpu(availCpu);

                logger.debug(String.format("re-calculated available capacity on the host[uuid:%s]:" +
                                "\n[available memory] before: %s, now: %s" +
                                "\n[total cpu] before: %s, now: %s" +
                                "\n[available cpu] before: %s, now :%s",
                        s.hostUuid,
                        before, avail,
                        totalCpuBefore, totalCpu,
                        beforeCpu, availCpu));
                return cap;
            });
        }
    }

    private void handle(ReturnHostCapacityMsg msg) {
        returnComputeResourceCapacity(msg.getHostUuid(), msg.getCpuCapacity(), msg.getMemoryCapacity());
    }

    private void handle(ReportHostCapacityMessage msg) {
        long totalCpu = cpuRatioMgr.calculateHostCpuByRatio(msg.getHostUuid(), msg.getCpuNum());
        long availMem = msg.getTotalMemory() - msg.getUsedMemory();
        availMem = availMem > 0 ? availMem : 0;
        long availCpu = totalCpu - msg.getUsedCpu();
        availCpu = availCpu > 0 ? availCpu : 0;

        HostCapacityVO vo = dbf.findByUuid(msg.getHostUuid(), HostCapacityVO.class);
        if (vo == null) {
            vo = new HostCapacityVO();
            vo.setUuid(msg.getHostUuid());
            vo.setTotalCpu(totalCpu);
            vo.setAvailableCpu(availCpu);
            vo.setTotalMemory(msg.getTotalMemory());
            vo.setAvailableMemory(availMem);
            vo.setTotalPhysicalMemory(msg.getTotalMemory());
            vo.setAvailablePhysicalMemory(availMem);
            vo.setCpuNum(msg.getCpuNum());
            vo.setCpuSockets(msg.getCpuSockets());
            vo.setCpuCoreNum(msg.getCpuCoreNum());

            HostCapacityStruct s = new HostCapacityStruct();
            s.setCpuSockets(vo.getCpuSockets());
            s.setCapacityVO(vo);
            s.setCpuNum(msg.getCpuNum());
            s.setTotalCpu(totalCpu);
            s.setTotalMemory(msg.getTotalMemory());
            s.setUsedCpu(msg.getUsedCpu());
            s.setUsedMemory(msg.getUsedMemory());
            s.setInit(true);
            for (ReportHostCapacityExtensionPoint ext : pluginRgty.getExtensionList(ReportHostCapacityExtensionPoint.class)) {
                vo = ext.reportHostCapacity(s);
            }
            dbf.persist(vo);
        } else if (needUpdateCapacity(vo, msg, totalCpu, availCpu, availMem)) {
            vo.setCpuNum(msg.getCpuNum());
            vo.setTotalCpu(totalCpu);
            vo.setAvailableCpu(availCpu);
            vo.setTotalPhysicalMemory(msg.getTotalMemory());
            vo.setAvailablePhysicalMemory(availMem);
            vo.setTotalMemory(msg.getTotalMemory());
            vo.setCpuSockets(msg.getCpuSockets());
            vo.setCpuCoreNum(msg.getCpuCoreNum());

            HostCapacityStruct s = new HostCapacityStruct();
            s.setCapacityVO(vo);
            s.setCpuSockets(msg.getCpuSockets());
            s.setTotalCpu(totalCpu);
            s.setTotalMemory(msg.getTotalMemory());
            s.setUsedCpu(msg.getUsedCpu());
            s.setUsedMemory(msg.getUsedMemory());
            s.setInit(false);
            for (ReportHostCapacityExtensionPoint ext : pluginRgty.getExtensionList(ReportHostCapacityExtensionPoint.class)) {
                vo = ext.reportHostCapacity(s);
            }
            dbf.update(vo);
        }

        bus.reply(msg, new MessageReply());
    }

    private boolean needUpdateCapacity(HostCapacityVO vo, ReportHostCapacityMessage msg, long totalCpu, long avaliCpu, long availMem) {
        return vo.getCpuNum() != msg.getCpuNum() || vo.getTotalCpu() != totalCpu 
                || vo.getAvailableCpu() != avaliCpu || vo.getTotalPhysicalMemory() != msg.getTotalMemory()
                || vo.getAvailablePhysicalMemory() != availMem || vo.getTotalMemory() != msg.getTotalMemory()
                || vo.getCpuSockets() != msg.getCpuSockets() || vo.getCpuCoreNum() != msg.getCpuCoreNum();
    }

    private void handle(final AllocateHostMsg msg) {
        if (HostAllocatorGlobalConfig.HOST_ALLOCATOR_ALLOW_CONCURRENT.value(Boolean.class)) {
            thdf.chainSubmit(new ChainTask(msg) {
                @Override
                public String getSyncSignature() {
                    return "host-allocator";
                }

                @Override
                public void run(SyncTaskChain chain) {
                    doHandleAllocateHost(msg, new Completion(chain) {
                        @Override
                        public void success() {
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            chain.next();
                        }
                    });
                }

                @Override
                public String getName() {
                    return "allocate-host-for-vm-" + msg.getVmInstance().getUuid();
                }

                @Override
                protected int getSyncLevel() {
                    return HostAllocatorGlobalConfig.HOST_ALLOCATOR_CONCURRENT_LEVEL.value(Integer.class);
                }
            });

            return;
        }

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "host-allocator";
            }

            @Override
            public void run(SyncTaskChain chain) {
                doHandleAllocateHost(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "allocate-host-for-vm-" + msg.getVmInstance().getUuid();
            }
        });
    }

    private void doHandleAllocateHost(final AllocateHostMsg msg, Completion completion) {
        HostAllocatorSpec spec = HostAllocatorSpec.fromAllocationMsg(msg);
        spec.setBackupStoragePrimaryStorageMetrics(backupStoragePrimaryStorageMetrics);

        String hvType = spec.getHypervisorType();
        if (hvType == null && msg instanceof DesignatedAllocateHostMsg) {
            DesignatedAllocateHostMsg dmsg = (DesignatedAllocateHostMsg) msg;
            if (dmsg.getHostUuid() != null) {
                hvType = Q.New(HostVO.class).eq(HostVO_.uuid, dmsg.getHostUuid()).select(HostVO_.hypervisorType).findValue();
            } else if (!CollectionUtils.isEmpty(dmsg.getClusterUuids())) {
                List<String> hvTypes = Q.New(ClusterVO.class).in(ClusterVO_.uuid, dmsg.getClusterUuids())
                        .groupBy(ClusterVO_.hypervisorType).select(ClusterVO_.hypervisorType).listValues();
                hvType = hvTypes.size() == 1 ? hvTypes.get(0) : null;
            }
        }

        if (hvType == null && msg.getImage() != null && !msg.isDryRun()) {
            HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getImage().getFormat());
            if (type != null) {
                hvType = type.toString();
            }
        }
        spec.setHypervisorType(hvType);

        String allocatorStrategyType = null;
        for (HostAllocatorStrategyExtensionPoint ext : pluginRgty.getExtensionList(HostAllocatorStrategyExtensionPoint.class)) {
            allocatorStrategyType = ext.getHostAllocatorStrategyName(spec);
            if (allocatorStrategyType != null) {
                logger.debug(String.format("%s returns allocator strategy type[%s]", ext.getClass(), allocatorStrategyType));
                break;
            }
        }

        if (allocatorStrategyType == null) {
            allocatorStrategyType = msg.getAllocatorStrategy();
        }

        HostAllocatorStrategyFactory factory = getHostAllocatorStrategyFactory(HostAllocatorStrategyType.valueOf(allocatorStrategyType));
        logger.debug("found strategy factory: " + factory.getClass().getSimpleName());
        HostAllocatorStrategy strategy = factory.getHostAllocatorStrategy();
        HostSortorStrategy sortors = factory.getHostSortorStrategy();

        factory.marshalSpec(spec, msg);

        if (msg.isDryRun()) {
            final AllocateHostDryRunReply reply = new AllocateHostDryRunReply();
            strategy.dryRun(spec, new ReturnValueCompletion<List<HostInventory>>(msg) {
                @Override
                public void success(List<HostInventory> hosts) {
                    if (hosts.isEmpty()){
                        reply.setHosts(new ArrayList<>());
                        bus.reply(msg, reply);
                        completion.success();
                        return;
                    }

                    sortors.dryRunSort(spec, hosts, new ReturnValueCompletion<List<HostInventory>>(msg) {
                        @Override
                        public void success(List<HostInventory> returnValue) {
                            reply.setHosts(returnValue);
                            bus.reply(msg, reply);
                            completion.success();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            reply.setError(errorCode);
                            bus.reply(msg, reply);
                            completion.fail(errorCode);
                        }
                    });
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                    completion.fail(errorCode);
                }
            });
        } else {
            final AllocateHostReply reply = new AllocateHostReply();
            FlowChain chain = FlowChainBuilder.newSimpleFlowChain();

            String allocatedHosts = "HOST_CANDIDATES";
            chain.setName("do-handle-allocate-host-flow");
            chain.then(new NoRollbackFlow() {
                String __name__ = "allocate-host-candidates";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    strategy.allocate(spec, new ReturnValueCompletion<List<HostInventory>>(trigger) {
                        @Override
                        public void success(List<HostInventory> returnValue) {
                            data.put(allocatedHosts, returnValue);
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            }).then(new Flow() {
                String __name__ = "sort-and-reserve-host-capacity";

                @Override
                public boolean skip(Map data) {
                    return reply.getHost() != null;
                }

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    sortors.sort(spec, (List<HostInventory>) data.get(allocatedHosts), new ReturnValueCompletion<HostInventory>(completion, msg) {
                        @Override
                        public void success(HostInventory returnValue) {
                            reply.setHost(returnValue);
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
                    ReturnHostCapacityMsg rmsg = new ReturnHostCapacityMsg();
                    rmsg.setHostUuid(reply.getHost().getUuid());
                    rmsg.setMemoryCapacity(spec.getMemoryCapacity());
                    rmsg.setCpuCapacity(spec.getCpuCapacity());
                    bus.makeTargetServiceIdByResourceUuid(rmsg, HostAllocatorConstant.SERVICE_ID, rmsg.getHostUuid());
                    bus.send(rmsg);
                    trigger.rollback();
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    for (HostAllocateExtensionPoint exp: pluginRgty.getExtensionList(HostAllocateExtensionPoint.class)) {
                        exp.beforeAllocateHostSuccessReply(spec, reply.getHost().getUuid());
                    }
                    trigger.next();
                }
            }).done(new FlowDoneHandler(completion, msg) {
                @Override
                public void handle(Map data) {
                    bus.reply(msg, reply);
                    completion.success();
                }
            }).error(new FlowErrorHandler(completion, msg) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    reply.setError(errCode);
                    bus.reply(msg, reply);
                    completion.fail(errCode);
                }
            }).start();
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetCpuMemoryCapacityMsg) {
            handle((APIGetCpuMemoryCapacityMsg) msg);
        } else if (msg instanceof APIGetHostAllocatorStrategiesMsg) {
            handle((APIGetHostAllocatorStrategiesMsg) msg);
        } else if (msg instanceof APIGetCandidateBackupStorageForCreatingImageMsg) {
            handle((APIGetCandidateBackupStorageForCreatingImageMsg) msg);
        } else if (msg instanceof APIGetCandidateImagesForCreatingVmMsg) {
            handle((APIGetCandidateImagesForCreatingVmMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetHostAllocatorStrategiesMsg msg) {
        APIGetHostAllocatorStrategiesReply reply = new APIGetHostAllocatorStrategiesReply();
        reply.setHostAllocatorStrategies(HostAllocatorStrategyType.getAllExposedTypeNames());
        bus.reply(msg, reply);
    }


    private void handle(final APIGetCpuMemoryCapacityMsg msg) {
        APIGetCpuMemoryCapacityReply reply = new APIGetCpuMemoryCapacityReply();

        class CpuMemCapacity {
            Map<String, CpuMemCapacity> elements;
            long totalCpu;
            long availCpu;
            long totalMem;
            long availMem;
            long managedCpu;
        }

        CpuMemCapacity res = new Callable<CpuMemCapacity>() {
            private void calcElementCap(List<Tuple> tuples, CpuMemCapacity res) {
                if (res == null) {
                    return;
                }

                if (res.elements == null) {
                    res.elements = new HashMap<>();
                }

                for (Tuple tuple : tuples) {
                    Long totalCpu = tuple.get(0, Long.class);
                    Long availCpu = tuple.get(1, Long.class);
                    Long availMemory = tuple.get(2, Long.class);
                    Long totalMemory = tuple.get(3, Long.class);
                    Long managedCpuNum = tuple.get(4, Long.class);
                    String elementUuid = tuple.get(5, String.class);

                    CpuMemCapacity element = res.elements.getOrDefault(elementUuid, new CpuMemCapacity());
                    res.elements.put(elementUuid, element);
                    element.totalCpu = totalCpu == null ? 0L : totalCpu;
                    element.availCpu = availCpu == null ? 0L : availCpu;
                    element.totalMem = totalMemory == null ? 0L : totalMemory;
                    element.availMem = availMemory == null ? 0L : availMemory;
                    element.managedCpu = managedCpuNum == null ? 0L : managedCpuNum;
                }


                res.elements.forEach((eUuid, cap) -> {
                    ReservedHostCapacity rc;
                    if (msg.getHostUuids() != null) {
                        rc = reserveMgr.getReservedHostCapacityByHosts(list(eUuid));
                    } else if (msg.getClusterUuids() != null) {
                        rc = reserveMgr.getReservedHostCapacityByClusters(list(eUuid), msg.getHypervisorType());
                    } else if (msg.getZoneUuids() != null) {
                        rc = reserveMgr.getReservedHostCapacityByZones(list(eUuid), msg.getHypervisorType());
                    } else {
                        throw new CloudRuntimeException("should not be here");
                    }
                    cap.availMem = Math.max(0, cap.availMem - rc.getReservedMemoryCapacity());
                    res.totalCpu += cap.totalCpu;
                    res.availCpu += cap.availCpu;
                    res.totalMem += cap.totalMem;
                    res.availMem += cap.availMem;
                    res.managedCpu += cap.managedCpu;
                });
            }

            @Override
            @Transactional(readOnly = true)
            public CpuMemCapacity call() {
                boolean checkHypervisor = false;
                String addHypervisorSqlString = "";
                if (msg.getHypervisorType() != null) {
                    checkHypervisor = true;
                    addHypervisorSqlString = " and host.hypervisorType = :hhtype";
                }

                CpuMemCapacity res = new CpuMemCapacity();

                if (msg.getHostUuids() != null && !msg.getHostUuids().isEmpty()) {
                    reply.setResourceType(HostVO.class.getSimpleName());
                    String sql = "select sum(hc.totalCpu), sum(hc.availableCpu), sum(hc.availableMemory), sum(hc.totalMemory), sum(hc.cpuNum), host.uuid" +
                            " from HostCapacityVO hc, HostVO host" +
                            " where hc.uuid in (:hostUuids)" +
                            " and hc.uuid = host.uuid" +
                            " and host.state = :hstate" +
                            " and host.status = :hstatus" +
                            addHypervisorSqlString +
                            " group by hc.uuid";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("hostUuids", msg.getHostUuids());
                    q.setParameter("hstate", HostState.Enabled);
                    q.setParameter("hstatus", HostStatus.Connected);
                    if (checkHypervisor) {
                        q.setParameter("hhtype", msg.getHypervisorType());
                    }
                    List<Tuple> ts = q.getResultList();
                    calcElementCap(ts, res);
                    return res;
                } else if (msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty()) {
                    reply.setResourceType(ClusterVO.class.getSimpleName());
                    String sql = "select sum(hc.totalCpu), sum(hc.availableCpu), sum(hc.availableMemory), sum(hc.totalMemory), sum(hc.cpuNum), host.clusterUuid" +
                            " from HostCapacityVO hc, HostVO host" +
                            " where hc.uuid = host.uuid" +
                            " and host.clusterUuid in (:clusterUuids)" +
                            " and host.state = :hstate" +
                            " and host.status = :hstatus" +
                            addHypervisorSqlString +
                            " group by host.clusterUuid";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("clusterUuids", msg.getClusterUuids());
                    q.setParameter("hstate", HostState.Enabled);
                    q.setParameter("hstatus", HostStatus.Connected);
                    if (checkHypervisor) {
                        q.setParameter("hhtype", msg.getHypervisorType());
                    }
                    List<Tuple> ts = q.getResultList();
                    calcElementCap(ts, res);
                    return res;
                } else if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
                    reply.setResourceType(ZoneVO.class.getSimpleName());
                    String sql = "select sum(hc.totalCpu), sum(hc.availableCpu), sum(hc.availableMemory), sum(hc.totalMemory), sum(hc.cpuNum), host.zoneUuid" +
                            " from HostCapacityVO hc, HostVO host" +
                            " where hc.uuid = host.uuid" +
                            " and host.zoneUuid in (:zoneUuids)" +
                            " and host.state = :hstate" +
                            " and host.status = :hstatus" +
                            addHypervisorSqlString +
                            " group by host.zoneUuid";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("zoneUuids", msg.getZoneUuids());
                    q.setParameter("hstate", HostState.Enabled);
                    q.setParameter("hstatus", HostStatus.Connected);
                    if (checkHypervisor) {
                        q.setParameter("hhtype", msg.getHypervisorType());
                    }
                    List<Tuple> ts = q.getResultList();
                    calcElementCap(ts, res);
                    return res;
                }

                throw new CloudRuntimeException("should not be here");
            }
        }.call();

        if (res.elements != null) {
            List<CpuMemoryCapacityData> dataList = new ArrayList<>();
            res.elements.forEach((uuid, element) -> {
                CpuMemoryCapacityData data = new CpuMemoryCapacityData();
                dataList.add(data);
                data.setResourceUuid(uuid);
                data.setTotalCpu(element.totalCpu);
                data.setAvailableCpu(element.availCpu);
                data.setTotalMemory(element.totalMem);
                data.setAvailableMemory(element.availMem);
                data.setManagedCpuNum(element.managedCpu);
            });
            reply.setCapacityData(dataList);
        }

        reply.setTotalCpu(res.totalCpu);
        reply.setTotalMemory(res.totalMem);
        reply.setAvailableCpu(res.availCpu);
        reply.setAvailableMemory(res.availMem);
        reply.setManagedCpuNum(res.managedCpu);
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID);
    }

    private void populateHostAllocatorStrategyFactory() {
        for (HostAllocatorStrategyFactory ext : pluginRgty.getExtensionList(HostAllocatorStrategyFactory.class)) {
            HostAllocatorStrategyFactory old = factories.get(ext.getHostAllocatorStrategyType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate HostAllocatorStrategyFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getHostAllocatorStrategyType()));
            }
            factories.put(ext.getHostAllocatorStrategyType().toString(), ext);
        }
    }

    @Override
    public boolean start() {
        populateHostAllocatorStrategyFactory();
        populatePrimaryStorageBackupStorageMetrics();
        installPrimaryStorageTypeDefaultField();
        populateVmTypeNeedSkipCapacityCalculate();
        return true;
    }

    private void populateVmTypeNeedSkipCapacityCalculate() {
        for (CollectCapacityUnsupportedVmTypeExtensionPoint ext : pluginRgty.getExtensionList(CollectCapacityUnsupportedVmTypeExtensionPoint.class)) {
            unsupportedVmTypeForCapacityCalculation.add(ext.getCapacityUnsupportedVmTypeString());
        }
    }

    private void populatePrimaryStorageBackupStorageMetrics() {
        for (Map.Entry<String, List<String>> e : backupStoragePrimaryStorageMetrics.entrySet()) {
            String bsType = e.getKey();
            List<String> psTypes = e.getValue();
            for (String psType : psTypes) {
                List<String> bsTypes = primaryStorageBackupStorageMetrics.get(psType);
                if (bsTypes == null) {
                    bsTypes = new ArrayList<>();
                    primaryStorageBackupStorageMetrics.put(psType, bsTypes);
                }
                bsTypes.add(bsType);
            }
        }
    }

    private void installPrimaryStorageTypeDefaultField() {
        // TODO: move all like it into storage module.
        PrimaryStorageType.getAllTypes().forEach(it -> {
            if (it.getPrimaryStorageFindBackupStorage() == null) {
                List<String> types = primaryStorageBackupStorageMetrics.get(it.toString());
                it.setPrimaryStorageFindBackupStorage(primaryStorageUuid -> Q.New(BackupStorageVO.class)
                        .in(BackupStorageVO_.type, types)
                        .select(BackupStorageVO_.uuid)
                        .listValues());
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public HostAllocatorStrategyFactory getHostAllocatorStrategyFactory(HostAllocatorStrategyType type) {
        HostAllocatorStrategyFactory factory = factories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Unable to find HostAllocatorStrategyFactory with type[%s]", type));
        }

        return factory;
    }

    @Override
    public void returnComputeResourceCapacity(final String hostUuid, final long cpu, final long memory) {
        new HostCapacityUpdater(hostUuid).run(new HostCapacityUpdaterRunnable() {
            @Override
            public HostCapacityVO call(HostCapacityVO cap) {
                {
                    long availCpu = cap.getAvailableCpu() + cpu;
                    availCpu = availCpu > cap.getTotalCpu() ? cap.getTotalCpu() : availCpu;
                /*
                if (availCpu > cap.getTotalCpu()) {
                    throw new CloudRuntimeException(String.format("invalid cpu capacity of the host[uuid:%s], available cpu[%s]" +
                            " is larger than the total cpu[%s]", hostUuid, availCpu, cap.getTotalCpu()));
                }
                */

                    cap.setAvailableCpu(availCpu);
                }

                {
                    long deltaMemory = ratioMgr.calculateMemoryByRatio(hostUuid, memory);
                    long availMemory = cap.getAvailableMemory() + deltaMemory;
                    if (availMemory > cap.getTotalMemory()) {
                        throw new CloudRuntimeException(
                                String.format("invalid memory capacity of host[uuid:%s]," +
                                                " available memory[%s] is greater than total memory[%s]." +
                                                " Available Memory before is [%s], Delta Memory is [%s].",
                                        hostUuid,
                                        new DecimalFormat(",###").format(availMemory),
                                        new DecimalFormat(",###").format(cap.getTotalMemory()),
                                        new DecimalFormat(",###").format(cap.getAvailableMemory()),
                                        new DecimalFormat(",###").format(deltaMemory)
                                )
                        );
                    }

                    cap.setAvailableMemory(availMemory);
                }
                logger.debug(String.format("[Host Allocation]: successfully return cpu[%s], memory[%s bytes] on host[uuid:%s]", cpu, memory, hostUuid));
                return cap;
            }
        });
    }

    @Override
    public Flow createVmAbnormalLifeCycleHandlingFlow(final VmAbnormalLifeCycleStruct struct) {
        return new Flow() {
            String __name__ = "allocate-host-capacity";

            VmAbnormalLifeCycleOperation operation = struct.getOperation();
            Runnable rollback;

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (operation == VmAbnormalLifeCycleOperation.VmRunningOnTheHost) {
                    vmRunningOnHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmMigrateToAnotherHost) {
                    vmMigrateToAnotherHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmRunningFromIntermediateState) {
                    vmRunningFromIntermediateState(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmNoStateFromIntermediateState) {
                    vmRunningFromIntermediateState(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmStoppedOnTheSameHost) {
                    vmStoppedOnTheSameHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmRunningFromUnknownStateHostChanged) {
                    vmRunningFromUnknownStateHostChanged(trigger);
                } else {
                    trigger.next();
                }
            }

            private void vmRunningFromUnknownStateHostChanged(FlowTrigger trigger) {
                // resync the capacity on the current host
                resyncHostCapacity();
                trigger.next();
            }

            private void resyncHostCapacity() {
                //TODO
            }

            private void vmStoppedOnTheSameHost(FlowTrigger trigger) {
                // return the capacity to the current host
                returnComputeCapacity(struct.getCurrentHostUuid());
                rollback = new Runnable() {
                    @Override
                    public void run() {
                        long cpu = struct.getVmInstance().getCpuNum();
                        reserveMgr.reserveCapacity(struct.getCurrentHostUuid(), cpu, struct.getVmInstance().getMemorySize(), false);
                    }
                };
                trigger.next();
            }

            private void returnComputeCapacity(String hostUuid) {
                ReturnHostCapacityMsg msg = new ReturnHostCapacityMsg();
                msg.setCpuCapacity(struct.getVmInstance().getCpuNum());
                msg.setMemoryCapacity(struct.getVmInstance().getMemorySize());
                msg.setHostUuid(hostUuid);
                bus.makeLocalServiceId(msg, HostAllocatorConstant.SERVICE_ID);
                bus.send(msg);
            }

            private void vmRunningFromIntermediateState(FlowTrigger trigger) {
                // resync the capacity on the current host
                resyncHostCapacity();
                trigger.next();
            }

            private void vmMigrateToAnotherHost(FlowTrigger trigger) {
                // allocate the capacity on the current host
                // return the capacity to the original host
                try {
                    final long cpu = struct.getVmInstance().getCpuNum();
                    reserveMgr.reserveCapacity(
                            struct.getCurrentHostUuid(), cpu, struct.getVmInstance().getMemorySize(), false);
                    returnComputeCapacity(struct.getOriginalHostUuid());

                    rollback = new Runnable() {
                        @Override
                        public void run() {
                            returnComputeCapacity(struct.getCurrentHostUuid());
                            reserveMgr.reserveCapacity(
                                    struct.getOriginalHostUuid(), cpu, struct.getVmInstance().getMemorySize(), false);
                        }
                    };

                    trigger.next();
                } catch (UnableToReserveHostCapacityException e) {
                    trigger.fail(operr(e.getMessage()));
                }
            }

            private void vmRunningOnHost(FlowTrigger trigger) {
                // allocate capacity on the current host
                // vm already in running state
                // do not need to check the reservation
                // just update capacity.
                try {
                    long cpu = struct.getVmInstance().getCpuNum();
                    reserveMgr.reserveCapacity(struct.getCurrentHostUuid(),
                            cpu, struct.getVmInstance().getMemorySize(), true);

                    rollback = new Runnable() {
                        @Override
                        public void run() {
                            returnComputeCapacity(struct.getCurrentHostUuid());
                        }
                    };

                    trigger.next();
                } catch (UnableToReserveHostCapacityException e) {
                    trigger.fail(operr(e.getMessage()));
                }
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (rollback != null) {
                    rollback.run();
                }

                trigger.rollback();
            }
        };
    }

    public void setBackupStoragePrimaryStorageMetrics(Map<String, List<String>> backupStoragePrimaryStorageMetrics) {
        this.backupStoragePrimaryStorageMetrics = backupStoragePrimaryStorageMetrics;
    }

    public Map<String, List<String>> getBackupStoragePrimaryStorageMetrics() {
        return backupStoragePrimaryStorageMetrics;
    }

    @Override
    public List<String> getPrimaryStorageTypesByBackupStorageTypeFromMetrics(String backupStorageType) {
        List<String> psTypes = backupStoragePrimaryStorageMetrics.get(backupStorageType);
        if (psTypes == null) {
            throw new CloudRuntimeException(String.format("cannot find supported primary storage types by the backup storage type[%s]", backupStorageType));
        }

        return psTypes;
    }

    @Override
    public List<String> getBackupStorageTypesByPrimaryStorageTypeFromMetrics(String psType) {
        List<String> bsTypes = primaryStorageBackupStorageMetrics.get(psType);
        if (bsTypes == null) {
            throw new CloudRuntimeException(String.format("cannot find supported backup storage types by the primary storage type[%s]", psType));
        }

        return bsTypes;
    }
}
