package org.zstack.header.vm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.MevocoVmSystemTags;
import org.zstack.compute.vm.VmHardwareSystemTags;
import org.zstack.compute.vm.VmNicParamBuilder;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.ha.HaSystemTags;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.APIMessage;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.volume.VolumeProvisioningStrategy;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.mevoco.MevocoSystemTags;
import org.zstack.network.securitygroup.*;
import org.zstack.resourceconfig.ResourceConfigVO;
import org.zstack.resourceconfig.ResourceConfigVO_;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.ResourceConfigSystemTag;
import org.zstack.tag.SystemTag;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.TagUtils;
import org.zstack.utils.WwnUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.zstack.storage.volume.VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CloneVmInstanceMessageBuilder {
    private static final List<SystemTag> volumeSystemTagsToCopy = Stream.of(
            VOLUME_PROVISIONING_STRATEGY, KVMSystemTags.VOLUME_VIRTIO_SCSI, KVMSystemTags.VOLUME_SCSI
    ).collect(Collectors.toList());

    private static final List<SystemTag> vmSystemTagsToCopy = Stream.of(
            VmSystemTags.VM_PRIORITY, VmSystemTags.RDP_ENABLE, VmSystemTags.USB_REDIRECT, VmSystemTags.SECURITY_ELEMENT_ENABLE,
            VmSystemTags.USERDATA, VmSystemTags.SSHKEY, VmSystemTags.CONSOLE_PASSWORD, VmSystemTags.ROOT_PASSWORD,
            VmSystemTags.VDI_MONITOR_NUMBER, VmSystemTags.MACHINE_TYPE, VmSystemTags.VM_VRING_BUFFER_SIZE, VmSystemTags.VIRTIO,
            VmSystemTags.QXL_MEMORY, VmSystemTags.DIRECTORY_UUID, VmSystemTags.BOOT_MODE, VmSystemTags.CLEAN_TRAFFIC,
            VmSystemTags.VM_GUEST_TOOLS,
            HaSystemTags.HA,
            VmHardwareSystemTags.CPU_SOCKETS, VmHardwareSystemTags.CPU_CORES, VmHardwareSystemTags.CPU_THREADS,
            MevocoVmSystemTags.VM_CPU_PINNING, MevocoVmSystemTags.VM_NUMA_ENABLE, MevocoVmSystemTags.VM_EMULATOR_PINNING,
            MevocoSystemTags.VM_CONSOLE_MODE
    ).collect(Collectors.toList());

    @Autowired
    private AccountManager acntMgr;

    APIMessage apiMessage;
    List<DiskAO> templatedVmVolumeDiskAOs = new ArrayList<>();
    List<DiskAO> newVolumeDiskAOs = new ArrayList<>();

    public CloneVmInstanceMessageBuilder(APIMessage apiMessage) {
        this.apiMessage = apiMessage;
    }

    public List<DiskAO> getNewVolumeDiskAOs() {
        return newVolumeDiskAOs;
    }

    public CloneVmInstanceMsg buildCloneVmInstanceMsg() {
        CloneVmInstanceMsg clmsg = new CloneVmInstanceMsg();
        if (apiMessage instanceof APICloneVmInstanceMsg) {
            APICloneVmInstanceMsg msg = (APICloneVmInstanceMsg) apiMessage;

            clmsg.setClusterUuid(msg.getClusterUuid());
            clmsg.setHostUuid(msg.getHostUuid());
            clmsg.setSession(msg.getSession());
            clmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
            clmsg.setNames(msg.getNames());
            clmsg.setStrategy(msg.getStrategy());
            clmsg.setSystemTags(buildResourceConfigsForClone(msg.getVmInstanceUuid(), VmInstanceVO.class.getSimpleName(), msg.getSystemTags()));
            clmsg.setUserTags(msg.getUserTags());
            clmsg.setFull(msg.getFull());
            clmsg.setDescription(msg.getDescription() != null ? msg.getDescription() : String.format("cloned from vm[uuid:%s]", msg.getVmInstanceUuid()));
            clmsg.setDiskAOs(buildDiskAOsForAPICloneVmInstanceMsg(msg));
            clmsg.setVmCustomSpecification(msg.getVmCustomSpecification());
            clmsg.setResetTpm(msg.getResetTpm());
            setL3NetworksForCloneVmInstanceMsg(clmsg, clmsg.getVmInstanceUuid(), null, null, msg.getVmNicParams());
        } else if (apiMessage instanceof APICreateTemplatedVmInstanceFromVmInstanceMsg) {
            APICreateTemplatedVmInstanceFromVmInstanceMsg msg = (APICreateTemplatedVmInstanceFromVmInstanceMsg) apiMessage;

            clmsg.setNames(Collections.singletonList(msg.getName()));
            clmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
            clmsg.setStrategy(VmCreationStrategy.CreateStopped.toString());
            clmsg.setClusterUuid(msg.getClusterUuid());
            clmsg.setHostUuid(msg.getHostUuid());
            clmsg.setSession(msg.getSession());
            clmsg.setUserTags(msg.getUserTags());
            clmsg.setFull(true);
            clmsg.setSystemTags(buildResourceConfigsForClone(msg.getVmInstanceUuid(), VmInstanceVO.class.getSimpleName(), msg.getSystemTags()));
            clmsg.getSystemTags().add(VolumeSystemTags.FAST_CREATE.getTagFormat());
            clmsg.getSystemTags().add(VolumeSystemTags.FLATTEN.getTagFormat());
            clmsg.setDiskAOs(buildVmDiskAOsForClone(msg.getVmInstanceUuid()));
            clmsg.setDescription(msg.getDescription() != null ? msg.getDescription() : String.format("cloned from vm[uuid:%s]", msg.getVmInstanceUuid()));
            setL3NetworksForCloneVmInstanceMsg(clmsg, msg.getVmInstanceUuid(), null, null, null);
        } else if (apiMessage instanceof APICreateVmInstanceFromTemplatedVmInstanceMsg) {
            APICreateVmInstanceFromTemplatedVmInstanceMsg msg = (APICreateVmInstanceFromTemplatedVmInstanceMsg) apiMessage;

            clmsg.setFull(true);
            clmsg.setClusterUuid(msg.getClusterUuid());
            clmsg.setHostUuid(msg.getHostUuid());
            clmsg.setSession(msg.getSession());
            clmsg.setUserTags(msg.getUserTags());
            clmsg.setResetTpm(msg.getResetTpm());
            if (msg.getTemplatedVmInstanceCache() == null) {
                clmsg.setVmInstanceUuid(msg.getTemplatedVmInstanceUuid());
                clmsg.setDiskAOs(buildVmDiskAOsForClone(clmsg.getVmInstanceUuid()));
                clmsg.getDiskAOs().forEach(diskAO -> {
                    diskAO.getSystemTags().removeIf(tag -> VOLUME_PROVISIONING_STRATEGY.isMatch(tag));
                    diskAO.getSystemTags().add(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY
                            .instantiateTag(map(e(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN, VolumeProvisioningStrategy.ThinProvisioning.toString()))));
                });
                clmsg.setSystemTags(buildResourceConfigsForClone(clmsg.getVmInstanceUuid(), VmInstanceVO.class.getSimpleName(), new ArrayList<>()));
                clmsg.getSystemTags().add(VolumeSystemTags.FAST_CREATE.getTagFormat());
                clmsg.getSystemTags().add(VolumeSystemTags.FLATTEN.getTagFormat());
                String name = String.format("cache for templated vmInstance %s", msg.getTemplatedVmInstanceUuid());
                clmsg.setNames(Collections.singletonList(name));
                clmsg.setStrategy(VmCreationStrategy.CreateStopped.toString());
                clmsg.getResourceUuidByName().put(name, Platform.getUuid());
                clmsg.setDescription(msg.getTemplatedVmInstance().getDescription());
                // cache vm will not create nics, so skip setting l3 networks
            } else {
                clmsg.setVmInstanceUuid(msg.getTemplatedVmInstanceCache().getCacheVmInstanceUuid());
                // build the diskAOs from templated vm
                List<DiskAO> templatedVmDiskAOs = buildVmDiskAOsForClone(msg.getTemplatedVmInstanceUuid(), msg.getDiskAOs());
                clmsg.setDiskAOs(mapTemplateDiskSourceUuidsToCacheVolumes(templatedVmDiskAOs, msg.getTemplatedVmInstanceUuid(), clmsg.getVmInstanceUuid()));
                // build systemTags from templated vm
                clmsg.setSystemTags(buildResourceConfigsForClone(msg.getTemplatedVmInstanceUuid(), VmInstanceVO.class.getSimpleName(), msg.getSystemTags()));
                clmsg.getSystemTags().add(VolumeSystemTags.FAST_CREATE.getTagFormat());
                clmsg.setNames(msg.getNames());
                clmsg.setCpuNum(msg.getCpuNum());
                clmsg.setMemorySize(msg.getMemorySize());
                clmsg.setReservedMemorySize(msg.getReservedMemorySize());
                clmsg.setVolumeSnapshotGroup(msg.getTemplatedCacheVolumeSnapshotGroup());
                clmsg.setDescription(msg.getDescription());
                DiskAO rootDiskAO = templatedVmVolumeDiskAOs
                        .stream().filter(DiskAO::isBoot).findFirst().orElse(null);
                if (rootDiskAO != null) {
                    clmsg.setPlatform(rootDiskAO.getPlatform());
                    clmsg.setGuestOsType(rootDiskAO.getGuestOsType());
                    clmsg.setArchitecture(rootDiskAO.getArchitecture());
                }
                clmsg.setStrategy(!CollectionUtils.isEmpty(newVolumeDiskAOs)
                        && ImagePlatform.Other.name().equals(msg.getTemplatedVmInstance().getPlatform())
                        ? VmCreationStrategy.CreateStopped.toString() : msg.getStrategy());
                clmsg.setVmCustomSpecification(msg.getVmCustomSpecification());
                setL3NetworksForCloneVmInstanceMsg(clmsg, msg.getTemplatedVmInstanceUuid(), msg.getL3NetworkUuids(), msg.getDefaultL3NetworkUuid(), msg.getVmNicParams());
            }
        }
        return clmsg;
    }

    // merge the configuration of the original vm with the incoming configuration
    private List<DiskAO> buildVmDiskAOsForClone(String vmUuid, List<DiskAO> diskAOs) {
        Tuple vmTuples = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.platform, VmInstanceVO_.guestOsType, VmInstanceVO_.architecture).findTuple();
        List<Tuple> volumeTuples = Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, vmUuid)
                .in(VolumeVO_.type, Arrays.asList(VolumeType.Root, VolumeType.Data))
                .select(VolumeVO_.uuid, VolumeVO_.type).listTuple();
        List<String> volumeUuids = volumeTuples.stream().map(t -> t.get(0, String.class)).collect(Collectors.toList());

        diskAOs = CollectionUtils.isEmpty(diskAOs) ? new ArrayList<>() : diskAOs;
        Map<String, DiskAO> templatedVmVolumeDiskAOByVolumeUuid = new HashMap<>();
        diskAOs.forEach(diskAO -> {
            if (volumeUuids.contains(diskAO.getSourceUuid())) {
                templatedVmVolumeDiskAOByVolumeUuid.put(diskAO.getSourceUuid(), diskAO);
            } else {
                this.newVolumeDiskAOs.add(diskAO);
            }
        });

        volumeTuples.forEach(volumeTuple -> {
            String volumeUuid = volumeTuple.get(0, String.class);
            VolumeType volumeType = volumeTuple.get(1, VolumeType.class);
            DiskAO diskAO = templatedVmVolumeDiskAOByVolumeUuid.get(volumeUuid);
            if (diskAO != null) {
                DiskAO newDiskAO = new DiskAO();
                newDiskAO.setBoot(volumeType.equals(VolumeType.Root));
                if (newDiskAO.isBoot()) {
                    newDiskAO.setPlatform(diskAO.getPlatform() != null ? diskAO.getPlatform() : vmTuples.get(0, String.class));
                    newDiskAO.setGuestOsType(diskAO.getGuestOsType() != null ? diskAO.getGuestOsType() : vmTuples.get(1, String.class));
                    newDiskAO.setArchitecture(diskAO.getArchitecture() != null ? diskAO.getArchitecture() : vmTuples.get(2, String.class));
                }
                newDiskAO.setSourceUuid(diskAO.getSourceUuid());
                newDiskAO.setPrimaryStorageUuid(diskAO.getPrimaryStorageUuid());
                newDiskAO.setSystemTags(buildResourceConfigsForClone(volumeUuid, VolumeVO.class.getSimpleName(), diskAO.getSystemTags()));
                newDiskAO.setSize(diskAO.getSize());
                this.templatedVmVolumeDiskAOs.add(newDiskAO);
                return;
            }

            diskAO = new DiskAO();
            diskAO.setSourceUuid(volumeUuid);
            diskAO.setSystemTags(buildResourceConfigsForClone(volumeUuid, VolumeVO.class.getSimpleName(), new ArrayList<>()));
            if (volumeType.equals(VolumeType.Root)) {
                diskAO.setBoot(true);
                diskAO.setPlatform(diskAO.getPlatform() != null ? diskAO.getPlatform() : vmTuples.get(0, String.class));
                diskAO.setGuestOsType(diskAO.getGuestOsType() != null ? diskAO.getGuestOsType() : vmTuples.get(1, String.class));
                diskAO.setArchitecture(diskAO.getArchitecture() != null ? diskAO.getArchitecture() : vmTuples.get(2, String.class));
            }
            this.templatedVmVolumeDiskAOs.add(diskAO);
        });

        return templatedVmVolumeDiskAOs;
    }

    // only the configurations of the original vm are copied
    private static List<DiskAO> buildVmDiskAOsForClone(String VmUuid) {
        Tuple vmTuples = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, VmUuid)
                .select(VmInstanceVO_.platform, VmInstanceVO_.guestOsType, VmInstanceVO_.architecture).findTuple();
        List<Tuple> volumeTuples = Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, VmUuid)
                .in(VolumeVO_.type, Arrays.asList(VolumeType.Root, VolumeType.Data))
                .select(VolumeVO_.uuid, VolumeVO_.type).listTuple();

        List<DiskAO> diskAOs = new ArrayList<>();
        volumeTuples.forEach(volumeTuple -> {
            String volumeUuid = volumeTuple.get(0, String.class);
            VolumeType volumeType = volumeTuple.get(1, VolumeType.class);
            DiskAO diskAO = new DiskAO();
            diskAO.setSourceUuid(volumeUuid);
            diskAO.setSystemTags(buildResourceConfigsForClone(volumeUuid, VolumeVO.class.getSimpleName(), new ArrayList<>()));
            if (volumeType.equals(VolumeType.Root)) {
                diskAO.setBoot(true);
                diskAO.setPlatform(vmTuples.get(0, String.class));
                diskAO.setGuestOsType(vmTuples.get(1, String.class));
                diskAO.setArchitecture(vmTuples.get(2, String.class));
            }
            diskAOs.add(diskAO);
        });
        return diskAOs;
    }

    private static List<String> buildResourceConfigsForClone(String resourceUuid, String resourceType, List<String> incomingConfigs) {
        List<String> incomingSystemTags = new ArrayList<>();
        List<String> incomingResourceConfigs = new ArrayList<>();
        if (!CollectionUtils.isEmpty(incomingConfigs)) {
            incomingConfigs.forEach(tag -> {
                if (tag.contains("resourceConfig::")) {
                    incomingResourceConfigs.add(tag);
                    return;
                }
                incomingSystemTags.add(tag);
            });
        }

        List<String> newConfigs = buildNewResourceConfigs(resourceUuid, incomingResourceConfigs);
        if (Objects.equals(resourceType, VmInstanceVO.class.getSimpleName())) {
            newConfigs.addAll(buildNewSystemTags(resourceUuid, incomingSystemTags, vmSystemTagsToCopy));
        } else if (Objects.equals(resourceType, VolumeVO.class.getSimpleName())) {
            newConfigs.addAll(buildNewSystemTags(resourceUuid, incomingSystemTags, volumeSystemTagsToCopy));
        }

        return newConfigs;
    }

    private static List<String> buildNewSystemTags(String resourceUuid, List<String> incomingSystemTags, List<SystemTag> systemTagsToCopy) {
        List<String> newSystemTags = new ArrayList<>();

        Map<String, String> systemTagByFormat = getSystemTagMap(resourceUuid, systemTagsToCopy);
        incomingSystemTags.forEach(existedSystemTag -> {
            final String[] tagFormat = new String[1];

            if (existedSystemTag.startsWith("-")) {
                systemTagsToCopy.stream()
                        .filter(tagToBeCopied -> TagUtils.isMatch(tagToBeCopied.getTagFormat(), existedSystemTag.replaceFirst("-", "")))
                        .findFirst().ifPresent(tagToBeCopied -> tagFormat[0] = tagToBeCopied.getTagFormat());
                if (tagFormat[0] != null) {
                    systemTagByFormat.remove(tagFormat[0]);
                }
                return;
            }

            systemTagsToCopy.stream()
                    .filter(tagToBeCopied -> TagUtils.isMatch(tagToBeCopied.getTagFormat(), existedSystemTag))
                    .findFirst().ifPresent(tagToBeCopied -> tagFormat[0] = tagToBeCopied.getTagFormat());

            if (tagFormat[0] == null) {
                newSystemTags.add(existedSystemTag);
                return;
            }

            if (systemTagByFormat.get(tagFormat[0]) == null) {
                newSystemTags.add(existedSystemTag);
                return;
            }

            newSystemTags.add(existedSystemTag);
            systemTagByFormat.remove(tagFormat[0]);
        });

        if (!systemTagByFormat.isEmpty()) {
            newSystemTags.addAll(systemTagByFormat.values());
        }

        afterBuildNewSystemTags(newSystemTags);
        return newSystemTags;
    }

    private static void afterBuildNewSystemTags(List<String> systemTags) {
        if (systemTags.stream().anyMatch(KVMSystemTags.VOLUME_VIRTIO_SCSI::isMatch)) {
            systemTags.add(String.format("kvm::volume::%s", new WwnUtils().getRandomWwn()));
        }
    }

    private static List<String> buildNewResourceConfigs(String resourceUuid, List<String> incomingResourceConfigs) {
        List<String> newResourceConfigs = new ArrayList<>();

        Map<String, String> resourceConfigByName = getResourceConfigMap(resourceUuid);
        incomingResourceConfigs.forEach(existedConfigName -> {
            String configName;
            if (existedConfigName.startsWith("-")) {
                configName = existedConfigName.replaceFirst("-", "").split("::")[2];
                resourceConfigByName.remove(configName);
                return;
            }

            configName = existedConfigName.split("::")[2];
            if (resourceConfigByName.get(configName) == null) {
                newResourceConfigs.add(existedConfigName);
                return;
            }

            newResourceConfigs.add(existedConfigName);
            resourceConfigByName.remove(configName);
        });

        if (!resourceConfigByName.isEmpty()) {
            newResourceConfigs.addAll(resourceConfigByName.values());
        }

        return newResourceConfigs;
    }

    private static Map<String, String> getSystemTagMap(String resourceUuid, List<SystemTag> systemTagsToCopy) {
        List<String> systemTags = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, resourceUuid).select(SystemTagVO_.tag).listValues();
        Map<String, String> tagByFormat = new HashMap<>();
        systemTags.forEach(tag -> systemTagsToCopy.forEach(systemTagToBeCopied -> {
            if (TagUtils.isMatch(systemTagToBeCopied.getTagFormat(), tag)) {
                tagByFormat.put(systemTagToBeCopied.getTagFormat(), tag);
            }
        }));
        return tagByFormat;
    }

    private static Map<String, String> getResourceConfigMap(String resourceUuid) {
        List<ResourceConfigVO> resourceConfigVOs = Q.New(ResourceConfigVO.class).eq(ResourceConfigVO_.resourceUuid, resourceUuid).list();
        Map<String, String> configByName = new HashMap<>();
        resourceConfigVOs.forEach(vo -> configByName.put(vo.getName(), ResourceConfigSystemTag.buildResourceConfig(vo.getCategory(), vo.getName(), vo.getValue())));
        return configByName;
    }

    private List<DiskAO> buildDiskAOsForAPICloneVmInstanceMsg(APICloneVmInstanceMsg msg) {
        Map<String, String> volumesPrimaryStorageUuidMap = new HashMap<>();
        msg.getVmInstanceInventory().getAllDiskVolumes()
                .forEach(volume -> volumesPrimaryStorageUuidMap.put(volume.getUuid(), volume.getPrimaryStorageUuid()));

        if (CollectionUtils.isEmpty(msg.getDiskAOs())) {
            List<DiskAO> diskAOs = new ArrayList<>();
            buildVmDiskAOsForClone(msg.getVmInstanceUuid()).forEach(diskAO -> {
                diskAO.setSystemTags(null);
                if (diskAO.isBoot()) {
                    diskAO.setPrimaryStorageUuid(msg.getPrimaryStorageUuidForRootVolume());
                    diskAO.setSystemTags(msg.getRootVolumeSystemTags());
                } else {
                    if (!msg.getFull()) {
                        return;
                    }
                    diskAO.setPrimaryStorageUuid(msg.getPrimaryStorageUuidForDataVolume() != null ?
                            msg.getPrimaryStorageUuidForDataVolume() :
                            volumesPrimaryStorageUuidMap.get(diskAO.getSourceUuid()));
                    diskAO.setSystemTags(msg.getDataVolumeSystemTags());
                }
                diskAOs.add(diskAO);
            });
            return buildVmDiskAOsForClone(msg.getVmInstanceUuid(), diskAOs);
        }

        return buildVmDiskAOsForClone(msg.getVmInstanceUuid(), msg.getDiskAOs());
    }

    private List<DiskAO> mapTemplateDiskSourceUuidsToCacheVolumes(List<DiskAO> templatedVmDiskAOs, String templatedVmUuid, String templatedCacheVmUuid) {
        List<String> templatedVmVolumeUuidsSortedByDeviceId = getVolumeUuidsSortedByDeviceId(templatedVmUuid);
        List<String> templatedVmCacheVolumeUuidsSortedByDeviceId = getVolumeUuidsSortedByDeviceId(templatedCacheVmUuid);

        IntStream.range(0, templatedVmDiskAOs.size()).forEach(i -> {
            final String templateUuid = templatedVmVolumeUuidsSortedByDeviceId.get(i);
            final String cacheUuid = templatedVmCacheVolumeUuidsSortedByDeviceId.get(i);
            templatedVmDiskAOs.stream()
                    .filter(dao -> Objects.equals(dao.getSourceUuid(), templateUuid))
                    .forEach(dao -> dao.setSourceUuid(cacheUuid));
        });
        return templatedVmDiskAOs;
    }

    private List<String> getVolumeUuidsSortedByDeviceId(String vmUuid) {
        List<Tuple> volumeTuples = Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, vmUuid)
                .in(VolumeVO_.type, Arrays.asList(VolumeType.Root, VolumeType.Data))
                .select(VolumeVO_.uuid, VolumeVO_.deviceId).listTuple();
        return volumeTuples.stream()
                .sorted(Comparator.comparing((Tuple t) -> t.get(1, Integer.class)))
                .map(t -> t.get(0, String.class)).collect(Collectors.toList());

    }
    @SuppressWarnings({"unchecked"})
    private void setL3NetworksForCloneVmInstanceMsg(CloneVmInstanceMsg clmsg, String vmInstanceUuid, List<String> l3Uuids, String defaultL3Uuid, String nicParams) {
        if (!CollectionUtils.isEmpty(l3Uuids)) {
            clmsg.setL3NetworkUuids(l3Uuids);
            clmsg.setDefaultL3NetworkUuid(defaultL3Uuid);
        }

        VmInstanceVO vm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmInstanceUuid)
                .find();
        List<VmNicVO> sortedVmNics = vm.getVmNics().stream()
                .sorted(Comparator.comparingInt(VmNicVO::getDeviceId))
                .collect(Collectors.toList());
        if (!StringUtils.isEmpty(nicParams)) {
            List<VmNicParam> vmNicParams = JSONObjectUtil.toCollection(nicParams, ArrayList.class, VmNicParam.class);

            clmsg.setVmNicParams(vmNicParams);
            if (CollectionUtils.isEmpty(l3Uuids)) {
                // for APICloneVmInstanceMsg with vmNicParams
                clmsg.setL3NetworkUuids(vmNicParams.stream().map(VmNicParam::getL3NetworkUuid).collect(Collectors.toList()));
                if (clmsg.getL3NetworkUuids().size() == 1) {
                    clmsg.setDefaultL3NetworkUuid(clmsg.getL3NetworkUuids().get(0));
                } else {
                    clmsg.setDefaultL3NetworkUuid(vmNicParams.stream().filter(VmNicParam::isDefaultNic)
                            .map(VmNicParam::getL3NetworkUuid).findFirst().orElse(defaultL3Uuid));
                }
            }
        } else {
            clmsg.setVmNicParams(new VmNicParamBuilder().buildByVmUuid(vmInstanceUuid));
            if (CollectionUtils.isEmpty(l3Uuids)) {
                // for APICreateTemplatedVmInstanceFromVmInstanceMsg and APICloneVmInstanceMsg without vmNicParams
                clmsg.setL3NetworkUuids(VmNicHelper.getL3Uuids(VmNicInventory.valueOf(sortedVmNics)));
                clmsg.setDefaultL3NetworkUuid(vm.getDefaultL3NetworkUuid());
            }
        }

        List<VmNicSecurityGroupRefVO> refVOS = Q.New(VmNicSecurityGroupRefVO.class)
                .eq(VmNicSecurityGroupRefVO_.vmInstanceUuid, vmInstanceUuid)
                .orderBy(VmNicSecurityGroupRefVO_.priority, SimpleQuery.Od.ASC)
                .list();

        Map<String, Set<String>> nicSgUuidsMap = new HashMap<>();
        // new list to solve one l3 with multiple nics
        List<VmNicParam> vmNicParams = clmsg.getVmNicParams() != null ? new ArrayList<>(clmsg.getVmNicParams()) : new ArrayList<>();
        for (String l3Uuid : clmsg.getL3NetworkUuids()) {
            VmNicVO oldNic = sortedVmNics.stream()
                    .filter(nic -> Objects.equals(nic.getL3NetworkUuid(), l3Uuid))
                    .findFirst()
                    .orElse(null);
            VmNicParam nicParam = vmNicParams.stream()
                    .filter(param -> Objects.equals(param.getL3NetworkUuid(), l3Uuid))
                    .findFirst()
                    .orElse(new VmNicParam());

            if (oldNic == null) {
                continue;
            }

            List<String> nicSgUuids = refVOS.stream()
                    .filter(ref -> Objects.equals(ref.getVmNicUuid(), oldNic.getUuid()))
                    .map(VmNicSecurityGroupRefVO::getSecurityGroupUuid)
                    .collect(Collectors.toList());
            if (nicParam.getSgUuids() == null) {
                nicSgUuidsMap.computeIfAbsent(oldNic.getUuid(), v -> new HashSet<>()).addAll(nicSgUuids);
            } else {
                nicSgUuidsMap.computeIfAbsent(oldNic.getUuid(), v -> new HashSet<>()).addAll(nicParam.getSgUuids());

                /* admin maybe adds some sg to user vm, and the user doesn't know it.
                 *  when clone vm, these sgs should be added to vm */
                if (!acntMgr.isAdmin(clmsg.getSession())) {
                    List<String> ownSgUuids = acntMgr.getResourceUuidsCanAccessByAccount(clmsg.getSession().getAccountUuid(),
                            SecurityGroupVO.class);

                    if (ownSgUuids == null) {
                        continue;
                    }

                    nicSgUuidsMap.computeIfAbsent(oldNic.getUuid(), v -> new HashSet<>()).addAll(
                            nicSgUuids.stream()
                                    .filter(sgUuid -> !ownSgUuids.contains(sgUuid))
                                    .collect(Collectors.toSet())
                    );
                }
            }

            sortedVmNics.remove(oldNic);
            vmNicParams.remove(nicParam);
        }

        for (Map.Entry<String, Set<String>> entry : nicSgUuidsMap.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

            VmNicVO vmNic = vm.getVmNics().stream()
                    .filter(nic -> Objects.equals(nic.getUuid(), entry.getKey()))
                    .findFirst()
                    .orElse(null);
            if (vmNic == null) {
                continue;
            }

            clmsg.getSystemTags().add(VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.instantiateTag(
                    map(e(VmSystemTags.L3_UUID_TOKEN, vmNic.getL3NetworkUuid()),
                            e(VmSystemTags.SECURITY_GROUP_UUIDS_TOKEN, String.join(",", entry.getValue())))));
            VmNicSecurityPolicyVO policyVO = Q.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, vmNic.getUuid()).find();
            /* vmnic default policy has been changed */
            if (policyVO != null && (!policyVO.getIngressPolicy().equals(VmNicSecurityPolicy.DENY.toString())
                    || !policyVO.getEgressPolicy().equals(VmNicSecurityPolicy.ALLOW.toString()))) {
                clmsg.getSystemTags().add(VmSystemTags.SECURITY_GROUP_POLICY.instantiateTag(
                        map(e(VmSystemTags.L3_UUID_TOKEN, vmNic.getL3NetworkUuid()),
                                e(VmSystemTags.SECURITY_GROUP_INGRESS_POLICY_TOKEN, policyVO.getIngressPolicy()),
                                e(VmSystemTags.SECURITY_GROUP_EGRESS_POLICY_TOKEN, policyVO.getEgressPolicy()))));
            }
        }
    }

}
