package org.zstack.kvm.hypervisor.datatype;

import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.Q;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.kvm.hypervisor.KvmHypervisorInfoHelper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.kvm.KVMAgentCommands.VirtualizerInfoTO;

/**
 * Created by Wenhao.Zhang on 23/07/06
 */
public class ResourceHypervisorInfo {
    public String uuid;
    public String resourceType;
    public String virtualizer;
    public String version;

    public String matchTargetUuid;
    public String matchTargetResourceType;
    public String matchTargetVersion;

    public KvmHypervisorInfoVO vo;

    public static ResourceHypervisorInfo fromVmVirtualizerInfo(VirtualizerInfoTO info) {
        return fromVmVirtualizerInfo(info, null);
    }

    public static ResourceHypervisorInfo fromVmVirtualizerInfo(VirtualizerInfoTO info, String hostUuid) {
        ResourceHypervisorInfo result = from(info);
        result.resourceType = VmInstanceVO.class.getSimpleName();
        result.matchTargetResourceType = HostVO.class.getSimpleName();
        result.matchTargetUuid = hostUuid;
        return result;
    }

    public static ResourceHypervisorInfo fromHostVirtualizerInfo(VirtualizerInfoTO info) {
        ResourceHypervisorInfo result = from(info);
        result.resourceType = HostVO.class.getSimpleName();
        result.matchTargetResourceType = KvmHostHypervisorMetadataVO.class.getSimpleName();
        return result;
    }

    public static ResourceHypervisorInfo from(VirtualizerInfoTO info) {
        ResourceHypervisorInfo result = new ResourceHypervisorInfo();
        result.uuid = info.getUuid();
        result.virtualizer = info.getVirtualizer();
        result.version = info.getVersion();
        return result;
    }

    @Transactional(readOnly = true)
    public static List<ResourceHypervisorInfo> from(Collection<String> uuidSet) {
        List<KvmHypervisorInfoVO> list = Q.New(KvmHypervisorInfoVO.class)
                .in(KvmHypervisorInfoVO_.uuid, uuidSet)
                .list();
        Map<String, KvmHypervisorInfoVO> uuidVoMap = list.stream()
                .collect(Collectors.toMap(KvmHypervisorInfoVO::getUuid, Function.identity()));

        List<ResourceHypervisorInfo> results = Q.New(ResourceVO.class)
                .select(ResourceVO_.uuid, ResourceVO_.resourceType)
                .in(ResourceVO_.uuid, uuidSet)
                .listTuple()
                .stream()
                .map(tuple -> {
                    String uuid = tuple.get(0, String.class);
                    String type = tuple.get(1, String.class);
                    return mapResourceHypervisorInfo(uuid, type, uuidVoMap.get(uuid));
                })
                .collect(Collectors.toList());

        fillMatchTargetInfo(results);
        return results;
    }

    private static ResourceHypervisorInfo mapResourceHypervisorInfo(String uuid, String type, KvmHypervisorInfoVO vo) {
        ResourceHypervisorInfo target = new ResourceHypervisorInfo();
        target.uuid = uuid;
        target.resourceType = type;
        target.virtualizer = vo.getHypervisor();
        target.version = vo.getVersion();
        // matchTargetUuid, matchTargetResourceType, matchTargetVersion is empty

        return target;
    }

    private static void fillMatchTargetInfo(List<ResourceHypervisorInfo> inventories) {
        // collect expect version of VM : from host Kvm hypervisor info
        List<ResourceHypervisorInfo> vmInventories = inventories.stream()
                .filter(inv -> inv.resourceType.equals(VmInstanceVO.class.getSimpleName()))
                .collect(Collectors.toList());
        if (!vmInventories.isEmpty()) {
            collectMatchTargetOfVm(vmInventories);
        }

        // collect expect version of Host : from HostOsCategoryVO
        List<ResourceHypervisorInfo> hostInventories = inventories.stream()
                .filter(inv -> inv.resourceType.equals(HostVO.class.getSimpleName()))
                .collect(Collectors.toList());
        if (!hostInventories.isEmpty()) {
            collectMatchTargetOfHost(hostInventories);
        }
    }

    private static void collectMatchTargetOfVm(List<ResourceHypervisorInfo> inventories) {
        inventories.forEach(info -> info.matchTargetResourceType = HostVO.class.getSimpleName());

        Set<String> vmUuidSet = inventories.stream()
                .map(info -> info.uuid)
                .collect(Collectors.toSet());
        Map<String, String> vmHostMap = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid, VmInstanceVO_.hostUuid)
                .in(VmInstanceVO_.uuid, vmUuidSet)
                .listTuple()
                .stream()
                .collect(Collectors.toMap(tuple -> tuple.get(0, String.class), tuple -> tuple.get(1, String.class)));

        List<KvmHypervisorInfoVO> hostInfoList = Q.New(KvmHypervisorInfoVO.class)
                .in(KvmHypervisorInfoVO_.uuid, vmHostMap.values())
                .list();
        for (ResourceHypervisorInfo vmInventory : inventories) {
            String hostUuid = vmHostMap.get(vmInventory.uuid);
            KvmHypervisorInfoVO hostVersion = hostInfoList.stream()
                    .filter(hostInfo -> hostInfo.getUuid().equals(hostUuid))
                    .filter(hostInfo -> hostInfo.getHypervisor().equals(vmInventory.virtualizer))
                    .findAny()
                    .orElse(null);
            if (hostVersion == null) {
                continue;
            }
            vmInventory.matchTargetVersion = hostVersion.getVersion();
            vmInventory.matchTargetUuid = hostVersion.getUuid();
        }
    }

    private static void collectMatchTargetOfHost(List<ResourceHypervisorInfo> inventories) {
        inventories.forEach(info -> info.matchTargetResourceType = KvmHostHypervisorMetadataVO.class.getSimpleName());

        Map<String, HostOsCategoryVO> hostExpectedMap =
                KvmHypervisorInfoHelper.collectExpectedHypervisorInfoForHosts(inventories.stream()
                        .map(info -> info.uuid)
                        .collect(Collectors.toSet()));

        for (ResourceHypervisorInfo hostInventory : inventories) {
            HostOsCategoryVO category = hostExpectedMap.get(hostInventory.uuid);
            if (category == null) {
                continue;
            }
            KvmHostHypervisorMetadataVO metadata = category.getMetadataList().stream()
                    .filter(m -> m.getHypervisor().equals(hostInventory.virtualizer))
                    .findAny()
                    .orElse(null);
            if (metadata == null) {
                continue;
            }
            hostInventory.matchTargetVersion = metadata.getVersion();
            hostInventory.matchTargetUuid = metadata.getUuid();
        }
    }

    public KvmHypervisorInfoVO generate() {
        if (vo == null) {
            vo = new KvmHypervisorInfoVO();
            vo.setUuid(uuid);
        }
        vo.setHypervisor(virtualizer);
        vo.setVersion(version);
        vo.setMatchState(KvmHypervisorInfoHelper.isQemuVersionMatched(version, matchTargetVersion));

        return vo;
    }
}
