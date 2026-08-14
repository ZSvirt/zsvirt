package org.zstack.kvm.hypervisor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.GetVirtualizerInfoMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.UpdateVmCpuQuotaMsg;
import org.zstack.header.vm.VmCanonicalEvents;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.kvm.hypervisor.datatype.*;
import org.zstack.resourceconfig.*;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Map.Entry;
import static org.zstack.core.Platform.operr;
import static org.zstack.kvm.KVMAgentCommands.GetVirtualizerInfoRsp;
import static org.zstack.kvm.KVMAgentCommands.VirtualizerInfoTO;
import static org.zstack.kvm.hypervisor.HypervisorMetadataCollector.HypervisorMetadataDefinition;

/**
 * Created by Wenhao.Zhang on 23/02/23
 */
public class KvmHypervisorInfoManagerImpl implements KvmHypervisorInfoManager, Component {
    private static final CLogger logger = Utils.getLogger(KvmHypervisorInfoManagerImpl.class);

    @Autowired
    private DatabaseFacade db;
    @Autowired
    private HypervisorMetadataCollector collector;
    @Autowired
    private EventFacade events;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public void save(GetVirtualizerInfoRsp rsp) {
        final String hostUuid = rsp.getHostInfo().getUuid();
        final List<ResourceHypervisorInfo> list = rsp.getVmInfoList().stream()
                .map(info -> ResourceHypervisorInfo.fromVmVirtualizerInfo(info, hostUuid))
                .collect(Collectors.toList());
        list.add(ResourceHypervisorInfo.fromHostVirtualizerInfo(rsp.getHostInfo()));
        save(list);

        logger.debug(String.format("save GetVirtualizerInfoRsp for host[uuid:%s] successfully",
                rsp.getHostInfo().getUuid()));
    }

    @Override
    public void saveHostInfo(VirtualizerInfoTO info) {
        save(Collections.singletonList(ResourceHypervisorInfo.fromHostVirtualizerInfo(info)));
        logger.debug(String.format("save VirtualizerInfoTO for host[uuid:%s] successfully", info.getUuid()));
    }

    @Override
    public void saveVmInfo(VirtualizerInfoTO info) {
        save(Collections.singletonList(ResourceHypervisorInfo.fromVmVirtualizerInfo(info)));
        logger.debug(String.format("save VirtualizerInfoTO for vm[uuid:%s] successfully", info.getUuid()));
    }

    @Transactional
    private void save(List<ResourceHypervisorInfo> list) {
        Map<String, ResourceHypervisorInfo> uuidInfoMap = list.stream()
                .collect(Collectors.toMap(info -> info.uuid, Function.identity()));

        collectVmMatchTargetUuid(uuidInfoMap);
        collectVmMatchTargetVersion(uuidInfoMap);
        collectHostMatchTargetInfo(uuidInfoMap);
        collectHypervisorInfoVo(uuidInfoMap);

        // Save
        List<ResourceHypervisorInfo> toUpdateList = new ArrayList<>();
        List<ResourceHypervisorInfo> toPersistList = new ArrayList<>();

        uuidInfoMap.forEach((uuid, info) -> {
            List<ResourceHypervisorInfo> targets = (info.vo == null) ? toPersistList : toUpdateList;
            targets.add(info);
        });

        if (!toUpdateList.isEmpty()) {
            db.updateCollection(toUpdateList.stream()
                    .map(ResourceHypervisorInfo::generate)
                    .collect(Collectors.toList()));
        }

        if (!toPersistList.isEmpty()) {
            db.persistCollection(toPersistList.stream()
                    .map(ResourceHypervisorInfo::generate)
                    .collect(Collectors.toList()));
        }
    }

    private void collectVmMatchTargetUuid(Map<String, ResourceHypervisorInfo> uuidInfoMap) {
        List<String> vmUuidListNeedFindHost = uuidInfoMap.values().stream()
                .filter(info -> HostVO.class.getSimpleName().equals(info.matchTargetResourceType))
                .filter(info -> VmInstanceVO.class.getSimpleName().equals(info.resourceType))
                .filter(info -> info.matchTargetUuid == null)
                .map(info -> info.uuid)
                .collect(Collectors.toList());
        if (vmUuidListNeedFindHost.isEmpty()) {
            return;
        }

        final List<Tuple> vmHostTuples = Q.New(VmInstanceVO.class)
                .in(VmInstanceVO_.uuid, vmUuidListNeedFindHost)
                .select(VmInstanceVO_.uuid, VmInstanceVO_.hostUuid)
                .listTuple();
        vmHostTuples.forEach(tuple ->
                uuidInfoMap.get(tuple.get(0, String.class)).matchTargetUuid = tuple.get(1, String.class));
    }

    private void collectVmMatchTargetVersion(Map<String, ResourceHypervisorInfo> uuidInfoMap) {
        Map<String, List<ResourceHypervisorInfo>> targetUuidInfoMap = uuidInfoMap.values().stream()
                .filter(info -> info.matchTargetVersion == null)
                .filter(info -> HostVO.class.getSimpleName().equals(info.matchTargetResourceType))
                .filter(info -> VmInstanceVO.class.getSimpleName().equals(info.resourceType))
                .collect(Collectors.toMap(info -> info.matchTargetUuid, CollectionDSL::list, CollectionDSL::concat));
        if (targetUuidInfoMap.isEmpty()) {
            return;
        }

        // find match info from uuidInfoMap
        for (Iterator<Entry<String, List<ResourceHypervisorInfo>>> it = targetUuidInfoMap.entrySet().iterator(); it.hasNext();) {
            final Entry<String, List<ResourceHypervisorInfo>> next = it.next();
            final ResourceHypervisorInfo matchInfo = uuidInfoMap.get(next.getKey());
            if (matchInfo == null) {
                continue;
            }

            next.getValue().forEach(info -> info.matchTargetVersion = matchInfo.version);
            it.remove();
        }
        if (targetUuidInfoMap.isEmpty()) {
            return;
        }

        // find match info from database
        List<Tuple> tuples = Q.New(KvmHypervisorInfoVO.class)
                .in(KvmHypervisorInfoVO_.uuid, targetUuidInfoMap.keySet())
                .select(KvmHypervisorInfoVO_.uuid, KvmHypervisorInfoVO_.version)
                .listTuple();
        for (Tuple tuple : tuples) {
            String targetUuid = tuple.get(0, String.class);
            targetUuidInfoMap.get(targetUuid).forEach(info -> info.matchTargetVersion = tuple.get(1, String.class));
        }
    }

    private void collectHostMatchTargetInfo(Map<String, ResourceHypervisorInfo> uuidInfoMap) {
        Set<String> hostUuidSet = uuidInfoMap.values().stream()
                .filter(info -> info.matchTargetVersion == null)
                .filter(info -> HostVO.class.getSimpleName().equals(info.resourceType))
                .filter(info -> KvmHostHypervisorMetadataVO.class.getSimpleName().equals(info.matchTargetResourceType))
                .map(info -> info.uuid)
                .collect(Collectors.toSet());
        if (hostUuidSet.isEmpty()) {
            return;
        }

        final Map<String, HostOsCategoryVO> uuidCategoryMap =
                KvmHypervisorInfoHelper.collectExpectedHypervisorInfoForHosts(hostUuidSet);
        uuidCategoryMap.forEach((uuid, category) -> {
            if (category == null) {
                return;
            }

            ResourceHypervisorInfo info = uuidInfoMap.get(uuid);
            final KvmHostHypervisorMetadataVO metadata = category.getMetadataList()
                    .stream()
                    .filter(m -> m.getHypervisor().equals(info.virtualizer))
                    .findAny()
                    .orElse(null);
            if (metadata == null) {
                return;
            }

            info.matchTargetUuid = metadata.getUuid();
            info.matchTargetVersion = metadata.getVersion();
        });
    }

    private void collectHypervisorInfoVo(Map<String, ResourceHypervisorInfo> uuidInfoMap) {
        List<KvmHypervisorInfoVO> voList = Q.New(KvmHypervisorInfoVO.class)
                .in(KvmHypervisorInfoVO_.uuid, uuidInfoMap.keySet())
                .list();

        for (KvmHypervisorInfoVO vo : voList) {
            final ResourceHypervisorInfo info = uuidInfoMap.get(vo.getUuid());
            if (info != null) {
                info.vo = vo;
            }
        }
    }

    private KvmHypervisorInfoVO updateKvmHypervisorInfoVO(String uuid, KvmHypervisorInfoVO vo, VirtualizerInfoTO to) {
        if (vo == null) {
            vo = new KvmHypervisorInfoVO();
            vo.setUuid(uuid);
        }
        vo.setHypervisor(to.getVirtualizer());
        vo.setVersion(to.getVersion());
        return vo;
    }

    @Override
    public void clean(String uuid) {
        SQL.New(KvmHypervisorInfoVO.class).eq(KvmHypervisorInfoVO_.uuid, uuid).delete();
    }

    @Override
    public void refreshMetadata() {
        List<HypervisorMetadataDefinition> collected = collector.collect();
        boolean anyRecordUpdated = saveMetadataList(collected);
        if (anyRecordUpdated) {
            refreshHostMatchState();
        }
    }

    private void registerRefreshVmHypervisorHooks() {
        events.on(VmCanonicalEvents.VM_LIBVIRT_REPORT_REBOOT, new EventCallback<Object>() {
            @Override
            protected void run(Map<String, String> tokens, Object data) {
                onVmStart(data.toString());
            }
        });
        events.on(VmCanonicalEvents.VM_LIBVIRT_REPORT_START, new EventCallback<Object>() {
            @Override
            protected void run(Map<String, String> tokens, Object data) {
                onVmStart(data.toString());
            }
        });
    }

    private void onVmStart(String vmUuid) {
        final String hostUuid = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .eq(VmInstanceVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE)
                .select(VmInstanceVO_.hostUuid)
                .findValue();
        if (hostUuid == null) {
            // This is not a KVM VM (maybe bare-metal)
            return;
        }

        final GetVirtualizerInfoMsg message = new GetVirtualizerInfoMsg();
        message.setHostUuid(hostUuid);
        message.setVmInstanceUuids(Collections.singletonList(vmUuid));
        bus.makeTargetServiceIdByResourceUuid(message, HostConstant.SERVICE_ID, hostUuid);
        bus.send(message); // no need to reply
    }

    private boolean saveMetadataList(List<HypervisorMetadataDefinition> definitions) {
        List<HostOsCategoryVO> categoryVOS = definitions.stream()
                .map(this::mapToHostOsCategory)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(Comparator
                                .comparing(HostOsCategoryVO::getArchitecture)
                                .thenComparing(HostOsCategoryVO::getOsReleaseVersion))), ArrayList::new));
        return saveHostOsCategoryList(categoryVOS);
    }

    @Transactional
    protected boolean saveHostOsCategoryList(List<HostOsCategoryVO> categoryVOS) {
        // refresh all metadata with current management node
        SQL.New(KvmHostHypervisorMetadataVO.class)
                .eq(KvmHostHypervisorMetadataVO_.managementNodeUuid, Platform.getManagementServerId())
                .delete();
        if (CollectionUtils.isEmpty(categoryVOS)) {
            return false;
        }

        Set<String> requestArchitectures = categoryVOS.stream()
                .map(HostOsCategoryVO::getArchitecture)
                .collect(Collectors.toSet());
        Set<String> requestOsReleaseVersions = categoryVOS.stream()
                .map(HostOsCategoryVO::getOsReleaseVersion)
                .collect(Collectors.toSet());
        List<HostOsCategoryVO> existsCategories = Q.New(HostOsCategoryVO.class)
                .in(HostOsCategoryVO_.architecture, requestArchitectures)
                .in(HostOsCategoryVO_.osReleaseVersion, requestOsReleaseVersions)
                .list();

        List<HostOsCategoryVO> needPersistCategories = new ArrayList<>();
        List<KvmHostHypervisorMetadataVO> metadataList = new ArrayList<>();

        // fill KvmHostHypervisorMetadataVO.categoryUuid
        // fill HostOsCategoryVO.uuid if it is new
        for (HostOsCategoryVO category : categoryVOS) {
            HostOsCategoryVO realCategory = existsCategories.stream()
                    .filter(c -> c.getArchitecture().equals(category.getArchitecture()))
                    .filter(c -> c.getOsReleaseVersion().equals(category.getOsReleaseVersion()))
                    .findAny().orElse(null);
            
            if (realCategory == null) {
                String newUuid = Platform.getUuid();
                category.setUuid(newUuid);
                category.getMetadataList().forEach(m -> m.setCategoryUuid(newUuid));
                needPersistCategories.add(category);
            } else {
                String uuid = realCategory.getUuid();
                category.getMetadataList().forEach(m -> m.setCategoryUuid(uuid));
            }

            metadataList.addAll(category.getMetadataList());
        }

        boolean anyRecordUpdated = false;
        if (!needPersistCategories.isEmpty()) {
            db.persistCollection(needPersistCategories);
            anyRecordUpdated = true;
        }
        if (!metadataList.isEmpty()) {
            db.persistCollection(metadataList);
            anyRecordUpdated = true;
        }
        return anyRecordUpdated;
    }

    private void refreshHostMatchState() {
        Function<String, String> sqlGenerator = fragment -> String.format(
                "select " +
                    "%s " +
                "from " +
                    "KvmHypervisorInfoVO hyper," +
                    "ResourceVO resource " +
                "where " +
                    "hyper.uuid = resource.uuid " +
                    "and resource.resourceType = :resourceType", fragment);

        Long count = SQL.New(sqlGenerator.apply("count(*)"), Long.class)
                .param("resourceType", HostVO.class.getSimpleName())
                .find();

        Map<String, HypervisorVersionState> originStateMap = new HashMap<>();
        Map<String, HypervisorVersionState> stateMap = new HashMap<>();
        SQL.New(sqlGenerator.apply("hyper.uuid, hyper.matchState "), Tuple.class)
                .param("resourceType", HostVO.class.getSimpleName())
                .limit(100)
                .paginate(count, (List<Tuple> tuples) -> {
                        final Map<String, HypervisorVersionState> map = tuples.stream().collect(Collectors.toMap(
                                tuple -> tuple.get(0, String.class),
                                tuple -> tuple.get(1, HypervisorVersionState.class)
                        ));
                        originStateMap.putAll(map);
                        stateMap.putAll(refreshHostMatchStateFragment(map.keySet()));
                });

        // key: state, value: [KvmHypervisorInfoVO.uuid]
        Map<HypervisorVersionState, List<String>> updatedMap = new HashMap<>();
        originStateMap.forEach((uuid, originState) -> {
            final HypervisorVersionState state = stateMap.get(uuid);
            if (originState == state) {
                return;
            }
            updatedMap.compute(state, (v, list) -> {
                if (list == null) {
                    return new ArrayList<>(Arrays.asList(uuid));
                }
                list.add(uuid);
                return list;
            });
        });

        updatedMap.forEach((state, uuidList) ->
            SQL.New(KvmHypervisorInfoVO.class)
                    .in(KvmHypervisorInfoVO_.uuid, uuidList)
                    .set(KvmHypervisorInfoVO_.matchState, state)
                    .update()
        );
    }

    private Map<String, HypervisorVersionState> refreshHostMatchStateFragment(Set<String> hyperUuidSet) {
        return ResourceHypervisorInfo.from(hyperUuidSet).stream()
                .collect(Collectors.toMap(
                        hyper -> hyper.uuid,
                        hyper -> KvmHypervisorInfoHelper.isQemuVersionMatched(hyper.version, hyper.matchTargetVersion)
                ));
    }

    /**
     * note:
     *   HostOsCategoryVO.uuid is empty
     *   KvmHostHypervisorMetadataVO.categoryUuid is empty
     */
    private HostOsCategoryVO mapToHostOsCategory(HypervisorMetadataDefinition definition) {
        HostOsCategoryVO vo = new HostOsCategoryVO();

        vo.setArchitecture(definition.getArchitecture());
        vo.setOsReleaseVersion(definition.getOsReleaseVersion());

        KvmHostHypervisorMetadataVO metadata = new KvmHostHypervisorMetadataVO();
        metadata.setUuid(Platform.getUuid());
        metadata.setManagementNodeUuid(Platform.getManagementServerId());
        metadata.setHypervisor(definition.getHypervisor());
        metadata.setVersion(definition.getVersion());
        vo.setMetadataList(Arrays.asList(metadata));

        return vo;
    }

    private void installKVMGlobalConfigExtension() {
        ResourceConfig vmCpuQuotaConfig = rcf.getResourceConfig(KVMGlobalConfig.VM_CPU_QUOTA.getIdentity());
        vmCpuQuotaConfig.installUpdateExtension(new ResourceConfigUpdateExtensionPoint() {
            @Override
            public void updateResourceConfig(ResourceConfig config, String resourceUuid, String resourceType, String oldValue, String newValue) throws GlobalConfigException {
                FutureCompletion completion = new FutureCompletion(null);

                updateCpuQuota(resourceUuid, newValue, completion);
                completion.await(TimeUnit.SECONDS.toMillis(60));

                if (!completion.isSuccess()) {
                    SQL.New(ResourceConfigVO.class).eq(ResourceConfigVO_.category, KVMGlobalConfig.CATEGORY)
                            .eq(ResourceConfigVO_.name, KVMGlobalConfig.VM_CPU_QUOTA.getName()).eq(ResourceConfigVO_.resourceType, resourceType)
                            .eq(ResourceConfigVO_.resourceUuid, resourceUuid).set(ResourceConfigVO_.value, oldValue).update();
                    throw new GlobalConfigException(completion.getErrorCode().getDetails());
                }
            }
        });
    }

    private void updateCpuQuota(String resourceUuid, String newValue, Completion completion) {
        VmInstanceVO vm = db.findByUuid(resourceUuid, VmInstanceVO.class);
        if (vm.getHostUuid() == null) {
            completion.success();
            return;
        }

        UpdateVmCpuQuotaMsg updateVmCpuQuotaMsg = new UpdateVmCpuQuotaMsg();
        updateVmCpuQuotaMsg.setVmCpuQuota(Long.parseLong(newValue));
        updateVmCpuQuotaMsg.setVmInstanceUuid(vm.getUuid());
        updateVmCpuQuotaMsg.setHostUuid(vm.getHostUuid());

        bus.makeTargetServiceIdByResourceUuid(updateVmCpuQuotaMsg, HostConstant.SERVICE_ID, vm.getUuid());
        bus.send(updateVmCpuQuotaMsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) throws GlobalConfigException {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                completion.success();
            }
        });
    }

    @Override
    public boolean start() {
        refreshMetadata();
        registerRefreshVmHypervisorHooks();
        installKVMGlobalConfigExtension();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
