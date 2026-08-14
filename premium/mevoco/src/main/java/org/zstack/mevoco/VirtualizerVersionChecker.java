package org.zstack.mevoco;

import org.zstack.core.db.SQL;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.vm.VirtualizerInfo;
import org.zstack.header.vm.VirtualizerInfoInventory;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.hypervisor.KvmHypervisorInfoHelper;
import org.zstack.kvm.hypervisor.datatype.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Wenhao.Zhang on 23/03/06
 */
public class VirtualizerVersionChecker {
    private VirtualizerVersionChecker() {}

    public static List<VirtualizerInfoInventory> check(Collection<String> uuidSet) {
        return ResourceHypervisorInfo.from(uuidSet)
                .stream()
                .map(VirtualizerVersionChecker::mapResourceInfoToInventory)
                .collect(Collectors.toList());
    }

    private static VirtualizerInfoInventory mapResourceInfoToInventory(ResourceHypervisorInfo source) {
        VirtualizerInfoInventory inventory = new VirtualizerInfoInventory();
        inventory.setUuid(source.uuid);
        inventory.setResourceType(source.resourceType);

        VirtualizerInfo info = new VirtualizerInfo();
        info.setHypervisor(source.virtualizer);
        info.setCurrentVersion(source.version);
        info.setExpectVersion(source.matchTargetVersion);
        info.setMatchState(KvmHypervisorInfoHelper.isQemuVersionMatched(
                info.getCurrentVersion(),
                info.getExpectVersion()
        ));

        inventory.setInfoList(Collections.singletonList(info));
        return inventory;
    }

    public static List<VirtualizerInfoInventory> findHypervisorMismatchHostsInCluster(String clusterUuid) {
        // roughly narrow the selection range of host
        Function<String, String> sqlGenerator = fragment -> String.format( 
                "select " +
                    "%s " +
                "from " +
                    "HostVO host," +
                    "KvmHypervisorInfoVO hyper " +
                "where " +
                    "host.uuid = hyper.uuid " +
                    "and host.clusterUuid = :clusterUuid " +
                    "and host.state = :hostState " +
                    "and host.status = :hostStatus " +
                    "and hyper.hypervisor = :hyperHypervisor", fragment);

        Long count = SQL.New(sqlGenerator.apply("count(*)"), Long.class)
                .param("clusterUuid", clusterUuid)
                .param("hostState", HostState.Enabled)
                .param("hostStatus", HostStatus.Connected)
                .param("hyperHypervisor", KVMConstant.VIRTUALIZER_QEMU_KVM)
                .find();

        final List<VirtualizerInfoInventory> results = new ArrayList<>();

        SQL.New(sqlGenerator.apply("hyper.uuid"), String.class)
                .param("clusterUuid", clusterUuid)
                .param("hostState", HostState.Enabled)
                .param("hostStatus", HostStatus.Connected)
                .param("hyperHypervisor", KVMConstant.VIRTUALIZER_QEMU_KVM)
                .limit(100)
                .paginate(count, (List<String> hyperUuidList) ->
                        results.addAll(filterUnmatchedVirtualizerInfoInventories(hyperUuidList)));
        return results;
    }

    private static List<VirtualizerInfoInventory> filterUnmatchedVirtualizerInfoInventories(List<String> hyperUuidList) {
        return check(hyperUuidList)
                .stream()
                .filter(inventory -> inventory.getInfoList().stream()
                        .filter(info -> KVMConstant.VIRTUALIZER_QEMU_KVM.equals(info.getHypervisor()))
                        .anyMatch(info -> HypervisorVersionState.Unmatched == info.getMatchState()))
                .collect(Collectors.toList());
    }
}
