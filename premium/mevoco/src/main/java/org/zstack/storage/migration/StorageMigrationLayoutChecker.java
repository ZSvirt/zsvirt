package org.zstack.storage.migration;

import org.zstack.core.db.Q;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageHostStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.local.LocalStorageHostRefVO;
import org.zstack.storage.primary.local.LocalStorageHostRefVO_;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_;
import org.zstack.storage.primary.local.LocalStorageSystemTags;
import org.zstack.tag.SystemTag;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class StorageMigrationLayoutChecker {
    private StorageMigrationLayoutChecker() {
    }

    static boolean shouldValidate(VmInstanceVO vm, Map<String, String> dstPsByVolumeUuid) {
        return !dstPsByVolumeUuid.isEmpty();
    }

    static String validateFinalLayout(VmInstanceVO vm, Map<String, String> dstPsByVolumeUuid,
                                      String dstHostUuid, List<String> systemTags) {
        return validateFinalLayout(vm, dstPsByVolumeUuid, dstHostUuid, systemTags,
                VmInstanceState.Stopped.equals(vm.getState()), true, true);
    }

    static CandidateContext createCandidateContext(
            VmInstanceVO vm, Collection<PrimaryStorageVO> primaryStorages) {
        return new CandidateContext(vm, primaryStorages);
    }

    static final class CandidateContext {
        private final VmInstanceVO vm;
        private final Map<String, String> primaryStorageTypes;
        private final Map<String, String> currentPrimaryStorageUuidByVolumeUuid;
        private final Map<String, Set<String>> primaryStorageUuidsByCluster;
        private final Set<String> vmCompatibleClusterUuids;
        private final Map<String, Set<String>> currentLocalHostUuidsByVolume;
        private final Map<String, Set<String>> localPrimaryStorageUuidsByHost;
        private final Map<String, Set<String>> connectedSharedPrimaryStorageUuidsByHost;
        private final Map<String, String> clusterUuidByHostUuid;

        private CandidateContext(VmInstanceVO vm, Collection<PrimaryStorageVO> primaryStorages) {
            this.vm = vm;
            primaryStorageTypes = primaryStorages.stream().collect(Collectors.toMap(
                    PrimaryStorageVO::getUuid,
                    PrimaryStorageVO::getType,
                    (left, right) -> left,
                    LinkedHashMap::new));
            currentPrimaryStorageUuidByVolumeUuid = vm.getAllDiskVolumes().stream().collect(Collectors.toMap(
                    VolumeVO::getUuid,
                    VolumeVO::getPrimaryStorageUuid,
                    (left, right) -> left,
                    LinkedHashMap::new));
            primaryStorageUuidsByCluster = getPrimaryStorageUuidsByCluster(primaryStorageTypes.keySet());
            vmCompatibleClusterUuids = getVmCompatibleClusterUuids(
                    vm, primaryStorageUuidsByCluster.keySet());

            List<VolumeVO> currentLocalVolumes = vm.getAllDiskVolumes().stream()
                    .filter(volume -> isLocalStorage(volume.getPrimaryStorageUuid(), primaryStorageTypes))
                    .collect(Collectors.toList());
            currentLocalHostUuidsByVolume = getCurrentLocalHostUuidsByVolume(currentLocalVolumes);

            Set<String> localPrimaryStorageUuids = primaryStorageTypes.entrySet().stream()
                    .filter(entry -> LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            localPrimaryStorageUuidsByHost = getLocalPrimaryStorageUuidsByHost(localPrimaryStorageUuids);

            List<HostVO> compatibleHosts = vmCompatibleClusterUuids.isEmpty()
                    ? Collections.emptyList()
                    : Q.New(HostVO.class)
                    .in(HostVO_.clusterUuid, vmCompatibleClusterUuids)
                    .eq(HostVO_.state, HostState.Enabled)
                    .eq(HostVO_.status, HostStatus.Connected)
                    .list();
            clusterUuidByHostUuid = compatibleHosts.stream().collect(Collectors.toMap(
                    HostVO::getUuid,
                    HostVO::getClusterUuid,
                    (left, right) -> left,
                    LinkedHashMap::new));

            Set<String> sharedPrimaryStorageUuids = primaryStorageTypes.entrySet().stream()
                    .filter(entry -> !LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            connectedSharedPrimaryStorageUuidsByHost = getConnectedSharedPrimaryStorageUuidsByHost(
                    clusterUuidByHostUuid.keySet(), sharedPrimaryStorageUuids);
        }

        String validate(String volumeUuid, String dstPrimaryStorageUuid) {
            String currentPrimaryStorageUuid = currentPrimaryStorageUuidByVolumeUuid.get(volumeUuid);
            boolean excludeCurrentHost = isRunningOrPaused(vm)
                    && (isLocalStorage(dstPrimaryStorageUuid, primaryStorageTypes)
                    || !dstPrimaryStorageUuid.equals(currentPrimaryStorageUuid));
            Map<String, String> finalPsByVolumeUuid = getFinalPrimaryStorageByVolumeUuid(
                    vm, Collections.singletonMap(volumeUuid, dstPrimaryStorageUuid));
            Set<String> finalPrimaryStorageUuids = new LinkedHashSet<>(finalPsByVolumeUuid.values());
            Set<String> finalSharedPrimaryStorageUuids = finalPrimaryStorageUuids.stream()
                    .filter(psUuid -> !isLocalStorage(psUuid, primaryStorageTypes))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<String> finalLocalPrimaryStorageUuids = finalPrimaryStorageUuids.stream()
                    .filter(psUuid -> isLocalStorage(psUuid, primaryStorageTypes))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<String> finalLocalHostUuids = new LinkedHashSet<>();

            for (VolumeVO volume : vm.getAllDiskVolumes()) {
                String finalPsUuid = finalPsByVolumeUuid.get(volume.getUuid());
                if (!isLocalStorage(finalPsUuid, primaryStorageTypes) || volumeUuid.equals(volume.getUuid())) {
                    continue;
                }

                Set<String> currentHostUuids = currentLocalHostUuidsByVolume.getOrDefault(
                        volume.getUuid(), Collections.emptySet());
                if (currentHostUuids.size() != 1) {
                    return String.format("cannot determine the current LocalStorage host of volume[uuid:%s]",
                            volume.getUuid());
                }
                finalLocalHostUuids.addAll(currentHostUuids);
            }

            if (finalLocalHostUuids.size() > 1) {
                return String.format("LocalStorage volumes would be placed on different hosts%s after migration",
                        finalLocalHostUuids);
            }

            Set<String> runnableClusterUuids = primaryStorageUuidsByCluster.entrySet().stream()
                    .filter(entry -> entry.getValue().containsAll(finalPrimaryStorageUuids))
                    .map(Map.Entry::getKey)
                    .filter(vmCompatibleClusterUuids::contains)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (runnableClusterUuids.isEmpty()) {
                return "no cluster satisfies primary storage, architecture and L2 requirements after migration";
            }

            if (!finalLocalHostUuids.isEmpty()) {
                String finalLocalHostUuid = finalLocalHostUuids.iterator().next();
                if (excludeCurrentHost && finalLocalHostUuid.equals(vm.getHostUuid())) {
                    return String.format("current host[uuid:%s] is the only host for the final LocalStorage layout",
                            vm.getHostUuid());
                }
                if (!localPrimaryStorageUuidsByHost
                        .getOrDefault(finalLocalHostUuid, Collections.emptySet())
                        .containsAll(finalLocalPrimaryStorageUuids)) {
                    return String.format("host[uuid:%s] cannot access all LocalStorage primary storages%s after migration",
                            finalLocalHostUuid, finalLocalPrimaryStorageUuids);
                }
                if (!runnableClusterUuids.contains(clusterUuidByHostUuid.get(finalLocalHostUuid))) {
                    return String.format("host[uuid:%s] is not in a cluster that satisfies primary storage, architecture and L2 requirements after migration",
                            finalLocalHostUuid);
                }
                if (!connectedSharedPrimaryStorageUuidsByHost
                        .getOrDefault(finalLocalHostUuid, Collections.emptySet())
                        .containsAll(finalSharedPrimaryStorageUuids)) {
                    return String.format("host[uuid:%s] cannot access all shared primary storages%s after migration",
                            finalLocalHostUuid, finalSharedPrimaryStorageUuids);
                }
                return null;
            }

            boolean hasRunnableHost = clusterUuidByHostUuid.entrySet().stream()
                    .filter(entry -> runnableClusterUuids.contains(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .filter(hostUuid -> !excludeCurrentHost || !hostUuid.equals(vm.getHostUuid()))
                    .filter(hostUuid -> connectedSharedPrimaryStorageUuidsByHost
                            .getOrDefault(hostUuid, Collections.emptySet())
                            .containsAll(finalSharedPrimaryStorageUuids))
                    .anyMatch(hostUuid -> finalLocalPrimaryStorageUuids.isEmpty()
                            || localPrimaryStorageUuidsByHost
                            .getOrDefault(hostUuid, Collections.emptySet())
                            .containsAll(finalLocalPrimaryStorageUuids));
            if (!hasRunnableHost) {
                if (excludeCurrentHost) {
                    return String.format("no enabled and connected host other than current host[uuid:%s] satisfies primary storage, architecture and L2 requirements after migration",
                            vm.getHostUuid());
                }
                return "no host satisfies primary storage, architecture and L2 requirements after migration";
            }
            return null;
        }

        boolean shareCluster(String firstPrimaryStorageUuid, String secondPrimaryStorageUuid) {
            return primaryStorageUuidsByCluster.values().stream()
                    .anyMatch(primaryStorageUuids -> primaryStorageUuids.contains(firstPrimaryStorageUuid)
                            && primaryStorageUuids.contains(secondPrimaryStorageUuid));
        }
    }

    static boolean hasFinalLocalStorage(VmInstanceVO vm, Map<String, String> dstPsByVolumeUuid) {
        Map<String, String> finalPsByVolumeUuid = getFinalPrimaryStorageByVolumeUuid(vm, dstPsByVolumeUuid);
        Map<String, String> primaryStorageTypes = getPrimaryStorageTypes(finalPsByVolumeUuid.values());
        return finalPsByVolumeUuid.values().stream().anyMatch(psUuid -> isLocalStorage(psUuid, primaryStorageTypes));
    }

    private static String validateFinalLayout(VmInstanceVO vm, Map<String, String> dstPsByVolumeUuid,
                                              String dstHostUuid, List<String> systemTags,
                                              boolean requireDstHostForMigratedLocalVolume,
                                              boolean requireDstHostForFixedLocalLiveVolume,
                                              boolean excludeCurrentHostForLiveWithoutDstHost) {
        if (!shouldValidate(vm, dstPsByVolumeUuid)) {
            return null;
        }

        String resolvedDstHostUuid = resolveDestinationHostUuid(dstHostUuid, systemTags);
        if (resolvedDstHostUuid != null) {
            String unavailableReason = getHostUnavailableReason(resolvedDstHostUuid);
            if (unavailableReason != null) {
                return unavailableReason;
            }
        }
        Map<String, String> finalPsByVolumeUuid = getFinalPrimaryStorageByVolumeUuid(vm, dstPsByVolumeUuid);
        Map<String, String> primaryStorageTypes = getPrimaryStorageTypes(finalPsByVolumeUuid.values());
        Set<String> finalPrimaryStorageUuids = new LinkedHashSet<>(finalPsByVolumeUuid.values());
        Set<String> finalSharedPrimaryStorageUuids = finalPrimaryStorageUuids.stream()
                .filter(psUuid -> !isLocalStorage(psUuid, primaryStorageTypes))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> finalLocalPrimaryStorageUuids = new LinkedHashSet<>();
        Set<String> finalLocalHostUuids = new LinkedHashSet<>();

        for (VolumeVO volume : vm.getAllDiskVolumes()) {
            String finalPsUuid = finalPsByVolumeUuid.get(volume.getUuid());
            if (!LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(primaryStorageTypes.get(finalPsUuid))) {
                continue;
            }

            finalLocalPrimaryStorageUuids.add(finalPsUuid);
            if (dstPsByVolumeUuid.containsKey(volume.getUuid())) {
                if (resolvedDstHostUuid == null && requireDstHostForMigratedLocalVolume) {
                    return String.format("LocalStorage migration requires a destination host for volume[uuid:%s]",
                            volume.getUuid());
                }
                if (resolvedDstHostUuid == null) {
                    continue;
                }
                finalLocalHostUuids.add(resolvedDstHostUuid);
                continue;
            }

            Set<String> currentHostUuids = getCurrentLocalHostUuids(volume.getUuid(), finalPsUuid);
            if (currentHostUuids.size() != 1) {
                return String.format("cannot determine the current LocalStorage host of volume[uuid:%s]",
                        volume.getUuid());
            }
            finalLocalHostUuids.addAll(currentHostUuids);
        }

        if (finalLocalHostUuids.size() > 1) {
            return String.format("LocalStorage volumes would be placed on different hosts%s after migration",
                    finalLocalHostUuids);
        }

        Set<String> runnableClusterUuids = getRunnableClusterUuids(vm,
                finalPrimaryStorageUuids);
        if (runnableClusterUuids.isEmpty()) {
            return "no cluster satisfies primary storage, architecture and L2 requirements after migration";
        }

        if (!finalLocalHostUuids.isEmpty()) {
            String finalLocalHostUuid = finalLocalHostUuids.iterator().next();
            if (resolvedDstHostUuid == null && requireDstHostForFixedLocalLiveVolume && isRunningOrPaused(vm)) {
                return String.format("LocalStorage final layout requires destination host[uuid:%s]",
                        finalLocalHostUuid);
            }
            if (resolvedDstHostUuid != null && !resolvedDstHostUuid.equals(finalLocalHostUuid)) {
                return String.format("LocalStorage volumes would be placed on different hosts[%s, %s] after migration",
                        finalLocalHostUuid, resolvedDstHostUuid);
            }
            if (!hostCanAccessAllLocalPrimaryStorages(finalLocalHostUuid, finalLocalPrimaryStorageUuids)) {
                return String.format("host[uuid:%s] cannot access all LocalStorage primary storages%s after migration",
                        finalLocalHostUuid, finalLocalPrimaryStorageUuids);
            }
            String clusterUuid = getHostClusterUuids(Collections.singleton(finalLocalHostUuid))
                    .get(finalLocalHostUuid);
            if (!runnableClusterUuids.contains(clusterUuid)) {
                return String.format("host[uuid:%s] is not in a cluster that satisfies primary storage, architecture and L2 requirements after migration",
                        finalLocalHostUuid);
            }
            if (!hostCanAccessAllSharedPrimaryStorages(finalLocalHostUuid, finalSharedPrimaryStorageUuids)) {
                return String.format("host[uuid:%s] cannot access all shared primary storages%s after migration",
                        finalLocalHostUuid, finalSharedPrimaryStorageUuids);
            }
            return null;
        }

        if (resolvedDstHostUuid != null) {
            String clusterUuid = getHostClusterUuids(Collections.singleton(resolvedDstHostUuid))
                    .get(resolvedDstHostUuid);
            if (!runnableClusterUuids.contains(clusterUuid)) {
                return String.format("host[uuid:%s] is not in a cluster that satisfies primary storage, architecture and L2 requirements after migration",
                        resolvedDstHostUuid);
            }
            if (!hostCanAccessAllSharedPrimaryStorages(resolvedDstHostUuid, finalSharedPrimaryStorageUuids)) {
                return String.format("host[uuid:%s] cannot access all shared primary storages%s after migration",
                        resolvedDstHostUuid, finalSharedPrimaryStorageUuids);
            }
            return null;
        }

        Set<String> runnableHostUuids = getRunnableHostUuids(runnableClusterUuids, finalSharedPrimaryStorageUuids);
        if (excludeCurrentHostForLiveWithoutDstHost && isRunningOrPaused(vm)) {
            runnableHostUuids.remove(vm.getHostUuid());
        }
        if (!finalLocalPrimaryStorageUuids.isEmpty()) {
            runnableHostUuids.retainAll(getHostsAccessingAllLocalPrimaryStorages(finalLocalPrimaryStorageUuids));
        }
        if (runnableHostUuids.isEmpty()) {
            return "no host satisfies primary storage, architecture and L2 requirements after migration";
        }
        return null;
    }

    private static Map<String, String> getFinalPrimaryStorageByVolumeUuid(
            VmInstanceVO vm, Map<String, String> dstPsByVolumeUuid) {
        return vm.getAllDiskVolumes().stream().collect(Collectors.toMap(
                VolumeVO::getUuid,
                volume -> dstPsByVolumeUuid.getOrDefault(volume.getUuid(), volume.getPrimaryStorageUuid()),
                (left, right) -> left,
                LinkedHashMap::new));
    }

    private static Map<String, String> getPrimaryStorageTypes(Collection<String> primaryStorageUuids) {
        if (primaryStorageUuids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PrimaryStorageVO> primaryStorages = Q.New(PrimaryStorageVO.class)
                .in(PrimaryStorageVO_.uuid, new HashSet<>(primaryStorageUuids))
                .list();
        return primaryStorages.stream().collect(
                Collectors.toMap(PrimaryStorageVO::getUuid, PrimaryStorageVO::getType));
    }

    private static Set<String> getRunnableClusterUuids(VmInstanceVO vm, Set<String> finalPrimaryStorageUuids) {
        Set<String> clusterUuids = getPrimaryStorageUuidsByCluster(finalPrimaryStorageUuids).entrySet().stream()
                .filter(entry -> entry.getValue().containsAll(finalPrimaryStorageUuids))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return getVmCompatibleClusterUuids(vm, clusterUuids);
    }

    private static Set<String> getVmCompatibleClusterUuids(
            VmInstanceVO vm, Collection<String> candidateClusterUuids) {
        Set<String> clusterUuids = new LinkedHashSet<>(candidateClusterUuids);
        if (clusterUuids.isEmpty()) {
            return clusterUuids;
        }
        if (vm.getArchitecture() != null) {
            List<String> architectureMatchedClusters = Q.New(ClusterVO.class)
                    .in(ClusterVO_.uuid, clusterUuids)
                    .eq(ClusterVO_.architecture, vm.getArchitecture())
                    .select(ClusterVO_.uuid)
                    .listValues();
            clusterUuids.retainAll(architectureMatchedClusters);
        }
        if (clusterUuids.isEmpty()) {
            return clusterUuids;
        }

        List<String> l3NetworkUuids = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, vm.getUuid())
                .select(VmNicVO_.l3NetworkUuid)
                .listValues();
        if (l3NetworkUuids.isEmpty()) {
            return clusterUuids;
        }
        List<String> l2Networks = Q.New(L3NetworkVO.class)
                .in(L3NetworkVO_.uuid, l3NetworkUuids)
                .select(L3NetworkVO_.l2NetworkUuid)
                .listValues();
        for (String l2NetworkUuid : new LinkedHashSet<>(l2Networks)) {
            List<String> l2Clusters = Q.New(L2NetworkClusterRefVO.class)
                    .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2NetworkUuid)
                    .select(L2NetworkClusterRefVO_.clusterUuid)
                    .listValues();
            clusterUuids.retainAll(l2Clusters);
            if (clusterUuids.isEmpty()) {
                return clusterUuids;
            }
        }
        return clusterUuids;
    }

    private static Map<String, Set<String>> getPrimaryStorageUuidsByCluster(Collection<String> primaryStorageUuids) {
        if (primaryStorageUuids.isEmpty()) {
            return Collections.emptyMap();
        }

        List<PrimaryStorageClusterRefVO> refs = Q.New(PrimaryStorageClusterRefVO.class)
                .in(PrimaryStorageClusterRefVO_.primaryStorageUuid, primaryStorageUuids)
                .list();
        return refs.stream().collect(Collectors.groupingBy(
                PrimaryStorageClusterRefVO::getClusterUuid,
                Collectors.mapping(PrimaryStorageClusterRefVO::getPrimaryStorageUuid, Collectors.toSet())));
    }

    private static Set<String> getCurrentLocalHostUuids(String volumeUuid, String primaryStorageUuid) {
        List<String> hostUuids = Q.New(LocalStorageResourceRefVO.class)
                .eq(LocalStorageResourceRefVO_.resourceUuid, volumeUuid)
                .eq(LocalStorageResourceRefVO_.resourceType, VolumeVO.class.getSimpleName())
                .eq(LocalStorageResourceRefVO_.primaryStorageUuid, primaryStorageUuid)
                .select(LocalStorageResourceRefVO_.hostUuid)
                .listValues();
        return new LinkedHashSet<>(hostUuids);
    }

    private static Map<String, Set<String>> getCurrentLocalHostUuidsByVolume(List<VolumeVO> volumes) {
        if (volumes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> primaryStorageUuidByVolumeUuid = volumes.stream().collect(Collectors.toMap(
                VolumeVO::getUuid, VolumeVO::getPrimaryStorageUuid));
        List<LocalStorageResourceRefVO> resourceRefs = Q.New(LocalStorageResourceRefVO.class)
                .in(LocalStorageResourceRefVO_.resourceUuid, volumes.stream()
                        .map(VolumeVO::getUuid).collect(Collectors.toSet()))
                .eq(LocalStorageResourceRefVO_.resourceType, VolumeVO.class.getSimpleName())
                .in(LocalStorageResourceRefVO_.primaryStorageUuid, volumes.stream()
                        .map(VolumeVO::getPrimaryStorageUuid).collect(Collectors.toSet()))
                .list();
        return resourceRefs.stream()
                .filter(ref -> ref.getPrimaryStorageUuid().equals(
                        primaryStorageUuidByVolumeUuid.get(ref.getResourceUuid())))
                .collect(Collectors.groupingBy(
                        LocalStorageResourceRefVO::getResourceUuid,
                        Collectors.mapping(LocalStorageResourceRefVO::getHostUuid, Collectors.toSet())));
    }

    private static Map<String, Set<String>> getLocalPrimaryStorageUuidsByHost(
            Set<String> primaryStorageUuids) {
        if (primaryStorageUuids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<LocalStorageHostRefVO> hostRefs = Q.New(LocalStorageHostRefVO.class)
                .in(LocalStorageHostRefVO_.primaryStorageUuid, primaryStorageUuids)
                .list();
        return hostRefs.stream().collect(Collectors.groupingBy(
                LocalStorageHostRefVO::getHostUuid,
                Collectors.mapping(LocalStorageHostRefVO::getPrimaryStorageUuid, Collectors.toSet())));
    }

    private static Set<String> getHostsAccessingAllLocalPrimaryStorages(Set<String> primaryStorageUuids) {
        return getLocalPrimaryStorageUuidsByHost(primaryStorageUuids).entrySet().stream()
                .filter(entry -> entry.getValue().containsAll(primaryStorageUuids))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean hostCanAccessAllLocalPrimaryStorages(String hostUuid, Set<String> primaryStorageUuids) {
        if (primaryStorageUuids.isEmpty()) {
            return true;
        }
        List<String> accessiblePrimaryStorages = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, hostUuid)
                .in(LocalStorageHostRefVO_.primaryStorageUuid, primaryStorageUuids)
                .select(LocalStorageHostRefVO_.primaryStorageUuid)
                .listValues();
        return new HashSet<>(accessiblePrimaryStorages).containsAll(primaryStorageUuids);
    }

    private static Set<String> getRunnableHostUuids(
            Set<String> clusterUuids, Set<String> sharedPrimaryStorageUuids) {
        Set<String> hostUuids = getHostUuidsByClusters(clusterUuids);
        if (!sharedPrimaryStorageUuids.isEmpty()) {
            hostUuids.retainAll(getHostsAccessingAllSharedPrimaryStorages(
                    hostUuids, sharedPrimaryStorageUuids));
        }
        return hostUuids;
    }

    private static boolean hostCanAccessAllSharedPrimaryStorages(String hostUuid, Set<String> primaryStorageUuids) {
        if (primaryStorageUuids.isEmpty()) {
            return true;
        }
        return getHostsAccessingAllSharedPrimaryStorages(
                Collections.singleton(hostUuid), primaryStorageUuids).contains(hostUuid);
    }

    private static Set<String> getHostsAccessingAllSharedPrimaryStorages(
            Collection<String> hostUuids, Set<String> primaryStorageUuids) {
        if (hostUuids.isEmpty() || primaryStorageUuids.isEmpty()) {
            return Collections.emptySet();
        }
        Map<String, Set<String>> connectedPrimaryStorageUuidsByHost =
                getConnectedSharedPrimaryStorageUuidsByHost(hostUuids, primaryStorageUuids);
        return hostUuids.stream()
                .filter(hostUuid -> connectedPrimaryStorageUuidsByHost
                        .getOrDefault(hostUuid, Collections.emptySet())
                        .containsAll(primaryStorageUuids))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Map<String, Set<String>> getConnectedSharedPrimaryStorageUuidsByHost(
            Collection<String> hostUuids, Set<String> primaryStorageUuids) {
        if (hostUuids.isEmpty() || primaryStorageUuids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PrimaryStorageHostRefVO> connectedHostRefs = Q.New(PrimaryStorageHostRefVO.class)
                .in(PrimaryStorageHostRefVO_.hostUuid, hostUuids)
                .in(PrimaryStorageHostRefVO_.primaryStorageUuid, primaryStorageUuids)
                .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Connected)
                .list();
        return connectedHostRefs.stream().collect(Collectors.groupingBy(
                PrimaryStorageHostRefVO::getHostUuid,
                Collectors.mapping(PrimaryStorageHostRefVO::getPrimaryStorageUuid, Collectors.toSet())));
    }

    private static Set<String> getHostUuidsByClusters(Collection<String> clusterUuids) {
        if (clusterUuids.isEmpty()) {
            return Collections.emptySet();
        }
        List<String> hostUuids = Q.New(HostVO.class)
                .in(HostVO_.clusterUuid, clusterUuids)
                .eq(HostVO_.state, HostState.Enabled)
                .eq(HostVO_.status, HostStatus.Connected)
                .select(HostVO_.uuid)
                .listValues();
        return new LinkedHashSet<>(hostUuids);
    }

    private static Map<String, String> getHostClusterUuids(Collection<String> hostUuids) {
        if (hostUuids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<HostVO> hosts = Q.New(HostVO.class)
                .in(HostVO_.uuid, hostUuids)
                .eq(HostVO_.state, HostState.Enabled)
                .eq(HostVO_.status, HostStatus.Connected)
                .list();
        return hosts.stream().collect(Collectors.toMap(HostVO::getUuid, HostVO::getClusterUuid));
    }

    private static String getHostUnavailableReason(String hostUuid) {
        HostVO host = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).find();
        if (host == null) {
            return String.format("destination host[uuid:%s] does not exist", hostUuid);
        }
        if (host.getState() != HostState.Enabled) {
            return String.format("destination host[uuid:%s] state is %s", hostUuid, host.getState());
        }
        if (host.getStatus() != HostStatus.Connected) {
            return String.format("destination host[uuid:%s] status is %s", hostUuid, host.getStatus());
        }
        return null;
    }

    private static String resolveDestinationHostUuid(String dstHostUuid, List<String> systemTags) {
        if (dstHostUuid != null || systemTags == null || systemTags.isEmpty()) {
            return dstHostUuid;
        }

        SystemTag hostTag = LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME;
        return systemTags.stream().filter(hostTag::isMatch)
                .map(tag -> hostTag.getTokenByTag(
                        tag, LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN))
                .findFirst().orElse(null);
    }

    private static boolean isRunningOrPaused(VmInstanceVO vm) {
        return VmInstanceState.Running.equals(vm.getState()) || VmInstanceState.Paused.equals(vm.getState());
    }

    private static boolean isLocalStorage(String primaryStorageUuid, Map<String, String> primaryStorageTypes) {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(primaryStorageTypes.get(primaryStorageUuid));
    }
}
