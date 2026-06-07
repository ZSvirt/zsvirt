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
import org.zstack.header.apimediator.ApiMessageInterceptionException;
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
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

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
            clmsg.setVmEncryption(resolveVmEncryptionForClone(msg.getVmInstanceUuid(), msg.getVmEncryption()));
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
                clmsg.setVmEncryption(resolveVmEncryptionForClone(msg.getTemplatedVmInstanceUuid(), msg.getVmEncryption()));
                setL3NetworksForCloneVmInstanceMsg(clmsg, msg.getTemplatedVmInstanceUuid(), msg.getL3NetworkUuids(), msg.getDefaultL3NetworkUuid(), msg.getVmNicParams());
            }
        }
        return clmsg;
    }

    private static Boolean resolveVmEncryptionForClone(String sourceVmUuid, Boolean requestedVmEncryption) {
        if (requestedVmEncryption != null) {
            return requestedVmEncryption;
        }
        return isSourceVmEncryptionEnabled(sourceVmUuid);
    }

    private static boolean isSourceVmEncryptionEnabled(String sourceVmUuid) {
        if (VmSystemTags.VM_ENCRYPTION.hasTag(sourceVmUuid, VmInstanceVO.class)) {
            return true;
        }
        return Boolean.TRUE.equals(Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.vmEncryption)
                .eq(VmInstanceVO_.uuid, sourceVmUuid)
                .findValue());
    }

    // merge the configuration of the original vm with the incoming configuration
    private List<DiskAO> buildVmDiskAOsForClone(String vmUuid, List<DiskAO> diskAOs) {
        Tuple vmTuples = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.platform, VmInstanceVO_.guestOsType, VmInstanceVO_.architecture).findTuple();
        List<Tuple> volumeTuples = Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, vmUuid)
                .in(VolumeVO_.type, Arrays.asList(VolumeType.Root, VolumeType.Data))
                .select(VolumeVO_.uuid, VolumeVO_.type, VolumeVO_.encrypted).listTuple();
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
            Boolean encrypted = volumeTuple.get(2, Boolean.class);
            DiskAO diskAO = templatedVmVolumeDiskAOByVolumeUuid.get(volumeUuid);
            if (diskAO != null) {
                DiskAO newDiskAO = new DiskAO();
                newDiskAO.setBoot(volumeType.equals(VolumeType.Root));
                if (newDiskAO.isBoot()) {
                    newDiskAO.setPlatform(diskAO.getPlatform() != null ? diskAO.getPlatform() : vmTuples.get(0, String.class));
                    newDiskAO.setGuestOsType(diskAO.getGuestOsType() != null ? diskAO.getGuestOsType() : vmTuples.get(1, String.class));
                    newDiskAO.setArchitecture(diskAO.getArchitecture() != null ? diskAO.getArchitecture() : vmTuples.get(2, String.class));
                }
                newDiskAO.setSourceUuid(volumeUuid);
                newDiskAO.setPrimaryStorageUuid(diskAO.getPrimaryStorageUuid());
                newDiskAO.setSystemTags(buildResourceConfigsForClone(volumeUuid, VolumeVO.class.getSimpleName(), diskAO.getSystemTags()));
                newDiskAO.setSize(diskAO.getSize());
                newDiskAO.setEncrypted(diskAO.getEncrypted() != null ? diskAO.getEncrypted() : encrypted);
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
            diskAO.setEncrypted(encrypted);
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
                .select(VolumeVO_.uuid, VolumeVO_.type, VolumeVO_.encrypted).listTuple();

        List<DiskAO> diskAOs = new ArrayList<>();
        volumeTuples.forEach(volumeTuple -> {
            String volumeUuid = volumeTuple.get(0, String.class);
            VolumeType volumeType = volumeTuple.get(1, VolumeType.class);
            Boolean encrypted = volumeTuple.get(2, Boolean.class);
            DiskAO diskAO = new DiskAO();
            diskAO.setSourceUuid(volumeUuid);
            diskAO.setSystemTags(buildResourceConfigsForClone(volumeUuid, VolumeVO.class.getSimpleName(), new ArrayList<>()));
            if (volumeType.equals(VolumeType.Root)) {
                diskAO.setBoot(true);
                diskAO.setPlatform(vmTuples.get(0, String.class));
                diskAO.setGuestOsType(vmTuples.get(1, String.class));
                diskAO.setArchitecture(vmTuples.get(2, String.class));
            }
            diskAO.setEncrypted(encrypted);
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
                tagByFormat.put(systemTagToBeCopied.getTagFormat(), VmSystemTags.plainTagForCreateMessage(resourceUuid, systemTagToBeCopied, tag));
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
            List<DiskAO> requestedDiskAOs = new ArrayList<>();
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
                requestedDiskAOs.add(diskAO);
            });
            List<DiskAO> diskAOs = buildVmDiskAOsForClone(msg.getVmInstanceUuid(), requestedDiskAOs);
            inheritSourceVolumeEncryption(msg.getVmInstanceUuid(), diskAOs);
            return diskAOs;
        }

        List<DiskAO> diskAOs = buildVmDiskAOsForClone(msg.getVmInstanceUuid(), msg.getDiskAOs());
        inheritSourceVolumeEncryption(msg.getVmInstanceUuid(), diskAOs);
        return diskAOs;
    }

    private void inheritSourceVolumeEncryption(String vmUuid, List<DiskAO> diskAOs) {
        List<Tuple> volumeTuples = Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, vmUuid)
                .in(VolumeVO_.type, Arrays.asList(VolumeType.Root, VolumeType.Data))
                .select(VolumeVO_.uuid, VolumeVO_.encrypted).listTuple();
        Map<String, Boolean> encryptedByVolumeUuid = volumeTuples.stream()
                .collect(Collectors.toMap(t -> t.get(0, String.class), t -> t.get(1, Boolean.class)));

        diskAOs.stream()
                .filter(diskAO -> encryptedByVolumeUuid.containsKey(diskAO.getSourceUuid()))
                .forEach(diskAO -> diskAO.setEncrypted(encryptedByVolumeUuid.get(diskAO.getSourceUuid())));
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

        Map<String, List<VmNicVO>> sourceNicsByL3 = sortedVmNics.stream()
                .collect(Collectors.groupingBy(VmNicVO::getL3NetworkUuid, LinkedHashMap::new, Collectors.toList()));
        Map<String, VmNicSecurityPolicyVO> securityPolicyByNicUuid = new HashMap<>();
        if (!sortedVmNics.isEmpty()) {
            Set<String> sourceNicUuids = sortedVmNics.stream().map(VmNicVO::getUuid).collect(Collectors.toSet());
            List<VmNicSecurityPolicyVO> securityPolicyVOS = Q.New(VmNicSecurityPolicyVO.class)
                    .in(VmNicSecurityPolicyVO_.vmNicUuid, sourceNicUuids)
                    .list();
            for (VmNicSecurityPolicyVO policyVO : securityPolicyVOS) {
                securityPolicyByNicUuid.putIfAbsent(policyVO.getVmNicUuid(), policyVO);
            }
        }
        Map<String, Integer> targetNicIndexByL3 = new HashMap<>();
        if (clmsg.getVmNicParams() == null) {
            clmsg.setVmNicParams(new ArrayList<>());
        }
        List<VmNicParam> vmNicParams = clmsg.getVmNicParams() != null ? new ArrayList<>(clmsg.getVmNicParams()) : new ArrayList<>();
        for (int targetNicDeviceId = 0; targetNicDeviceId < clmsg.getL3NetworkUuids().size(); targetNicDeviceId++) {
            String l3Uuid = clmsg.getL3NetworkUuids().get(targetNicDeviceId);
            List<VmNicVO> sourceNics = sourceNicsByL3.getOrDefault(l3Uuid, Collections.emptyList());
            int targetNicIndex = targetNicIndexByL3.getOrDefault(l3Uuid, 0);
            targetNicIndexByL3.put(l3Uuid, targetNicIndex + 1);
            VmNicVO oldNic = targetNicIndex < sourceNics.size()
                    ? sourceNics.get(targetNicIndex)
                    : sourceNics.size() == 1 ? sourceNics.get(0) : null;
            VmNicParam nicParam = vmNicParams.stream()
                    .filter(param -> Objects.equals(param.getL3NetworkUuid(), l3Uuid))
                    .findFirst()
                    .orElse(null);
            if (nicParam == null) {
                nicParam = new VmNicParam();
                nicParam.setL3NetworkUuid(l3Uuid);
                clmsg.getVmNicParams().add(nicParam);
            }

            if (oldNic == null) {
                continue;
            }

            List<String> nicSgUuids = refVOS.stream()
                    .filter(ref -> Objects.equals(ref.getVmNicUuid(), oldNic.getUuid()))
                    .map(VmNicSecurityGroupRefVO::getSecurityGroupUuid)
                    .collect(Collectors.toList());
            Set<String> targetNicSgUuids = new LinkedHashSet<>();
            if (nicParam.getSgUuids() == null) {
                targetNicSgUuids.addAll(nicSgUuids);
            } else {
                targetNicSgUuids.addAll(nicParam.getSgUuids());

                /* admin maybe adds some sg to user vm, and the user doesn't know it.
                 *  when clone vm, these sgs should be added to vm */
                if (!acntMgr.isAdmin(clmsg.getSession())) {
                    List<String> ownSgUuids = acntMgr.getResourceUuidsCanAccessByAccount(clmsg.getSession().getAccountUuid(),
                            SecurityGroupVO.class);

                    if (ownSgUuids == null) {
                        continue;
                    }

                    targetNicSgUuids.addAll(nicSgUuids.stream()
                            .filter(sgUuid -> !ownSgUuids.contains(sgUuid))
                            .collect(Collectors.toSet()));
                }
            }

            vmNicParams.remove(nicParam);
            if (targetNicSgUuids.isEmpty()) {
                continue;
            }

            nicParam.setSgUuids(new ArrayList<>(targetNicSgUuids));
            VmNicSecurityPolicyVO policyVO = securityPolicyByNicUuid.get(oldNic.getUuid());
            /* vmnic default policy has been changed */
            if (policyVO != null && (!policyVO.getIngressPolicy().equals(VmNicSecurityPolicy.DENY.toString())
                    || !policyVO.getEgressPolicy().equals(VmNicSecurityPolicy.ALLOW.toString()))) {
                clmsg.getVmNicSecurityPolicyByDeviceId().put(String.valueOf(targetNicDeviceId),
                        map(e(VmSystemTags.SECURITY_GROUP_INGRESS_POLICY_TOKEN, policyVO.getIngressPolicy()),
                                e(VmSystemTags.SECURITY_GROUP_EGRESS_POLICY_TOKEN, policyVO.getEgressPolicy())));
            }
        }

        bindStaticIpsToVmNicParams(clmsg);
    }

    private void bindStaticIpsToVmNicParams(CloneVmInstanceMsg clmsg) {
        if (CollectionUtils.isEmpty(clmsg.getSystemTags()) || CollectionUtils.isEmpty(clmsg.getVmNicParams())) {
            return;
        }

        Map<String, StaticIpTagContext> staticIpTags =
                buildStaticIpTagContext(clmsg.getSystemTags(), clmsg.getVmNicParams());
        if (staticIpTags.isEmpty()) {
            return;
        }

        for (VmNicParam vmNicParam : clmsg.getVmNicParams()) {
            if (StringUtils.isEmpty(vmNicParam.getL3NetworkUuid())) {
                continue;
            }

            reserveExplicitStaticIps(vmNicParam, staticIpTags.get(vmNicParam.getL3NetworkUuid()));
        }

        for (VmNicParam vmNicParam : clmsg.getVmNicParams()) {
            if (StringUtils.isEmpty(vmNicParam.getL3NetworkUuid())) {
                continue;
            }

            bindStaticIpToVmNicParam(vmNicParam, staticIpTags.get(vmNicParam.getL3NetworkUuid()));
        }
    }

    private Map<String, StaticIpTagContext> buildStaticIpTagContext(List<String> systemTags,
                                                                    List<VmNicParam> vmNicParams) {
        Map<String, StaticIpTagContext> ret = new HashMap<>();
        for (String systemTag : systemTags) {
            if (StringUtils.isEmpty(systemTag) || !VmSystemTags.STATIC_IP.isMatch(systemTag)) {
                continue;
            }

            Map<String, String> tokens = TagUtils.parse(VmSystemTags.STATIC_IP.getTagFormat(), systemTag);
            String l3Uuid = tokens.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
            StaticIpTagContext context = ret.computeIfAbsent(l3Uuid, k -> new StaticIpTagContext());
            String ip = IPv6NetworkUtils.ipv6TagValueToAddress(tokens.get(VmSystemTags.STATIC_IP_TOKEN));
            if (NetworkUtils.isIpv4Address(ip)) {
                context.ipv4.add(ip);
                context.hasIpv4 = true;
            } else if (IPv6NetworkUtils.isIpv6Address(ip)) {
                context.ipv6.add(IPv6NetworkUtils.getIpv6AddressCanonicalString(ip));
                context.hasIpv6 = true;
            }
        }

        for (VmNicParam vmNicParam : vmNicParams) {
            if (StringUtils.isEmpty(vmNicParam.getL3NetworkUuid())) {
                continue;
            }

            StaticIpTagContext context =
                    ret.computeIfAbsent(vmNicParam.getL3NetworkUuid(), k -> new StaticIpTagContext());
            context.hasIpv4 |= !StringUtils.isEmpty(vmNicParam.getIp());
            context.hasIpv6 |= !StringUtils.isEmpty(vmNicParam.getIp6());
        }

        for (String systemTag : systemTags) {
            if (StringUtils.isEmpty(systemTag)) {
                continue;
            }

            if (VmSystemTags.IPV4_GATEWAY.isMatch(systemTag)) {
                Map<String, String> tokens = TagUtils.parse(VmSystemTags.IPV4_GATEWAY.getTagFormat(), systemTag);
                String l3Uuid = tokens.get(VmSystemTags.IPV4_GATEWAY_L3_UUID_TOKEN);
                StaticIpTagContext context = ret.get(l3Uuid);
                if (context != null && context.hasIpv4) {
                    context.ipv4Gateway = tokens.get(VmSystemTags.IPV4_GATEWAY_TOKEN);
                }
            } else if (VmSystemTags.IPV4_NETMASK.isMatch(systemTag)) {
                Map<String, String> tokens = TagUtils.parse(VmSystemTags.IPV4_NETMASK.getTagFormat(), systemTag);
                String l3Uuid = tokens.get(VmSystemTags.IPV4_NETMASK_L3_UUID_TOKEN);
                StaticIpTagContext context = ret.get(l3Uuid);
                if (context != null && context.hasIpv4) {
                    context.ipv4Netmask = tokens.get(VmSystemTags.IPV4_NETMASK_TOKEN);
                }
            } else if (VmSystemTags.IPV6_GATEWAY.isMatch(systemTag)) {
                Map<String, String> tokens = TagUtils.parse(VmSystemTags.IPV6_GATEWAY.getTagFormat(), systemTag);
                String l3Uuid = tokens.get(VmSystemTags.IPV6_GATEWAY_L3_UUID_TOKEN);
                StaticIpTagContext context = ret.get(l3Uuid);
                if (context != null && context.hasIpv6) {
                    context.ipv6Gateway = IPv6NetworkUtils.getIpv6AddressCanonicalString(
                            IPv6NetworkUtils.ipv6TagValueToAddress(tokens.get(VmSystemTags.IPV6_GATEWAY_TOKEN)));
                }
            } else if (VmSystemTags.IPV6_PREFIX.isMatch(systemTag)) {
                Map<String, String> tokens = TagUtils.parse(VmSystemTags.IPV6_PREFIX.getTagFormat(), systemTag);
                String l3Uuid = tokens.get(VmSystemTags.IPV6_PREFIX_L3_UUID_TOKEN);
                StaticIpTagContext context = ret.get(l3Uuid);
                if (context != null && context.hasIpv6) {
                    context.ipv6Prefix = parseIpv6Prefix(
                            tokens.get(VmSystemTags.IPV6_PREFIX_TOKEN), systemTag);
                }
            }
        }

        return ret;
    }

    private Integer parseIpv6Prefix(String prefix, String systemTag) {
        try {
            int prefixLen = Integer.parseInt(prefix);
            if (prefixLen > IPv6Constants.IPV6_PREFIX_LEN_MAX || prefixLen < IPv6Constants.IPV6_PREFIX_LEN_MIN) {
                throw new ApiMessageInterceptionException(Platform.argerr("ip range prefix length[%d] is out of range [%d - %d]. Please correct your system tag[%s] of %s",
                        prefixLen, IPv6Constants.IPV6_PREFIX_LEN_MIN, IPv6Constants.IPV6_PREFIX_LEN_MAX, systemTag, VmSystemTags.IPV6_PREFIX_TOKEN));
            }
            return prefixLen;
        } catch (NumberFormatException e) {
            throw new ApiMessageInterceptionException(Platform.argerr("ipv6 prefix length[%s] must be an integer. Please correct your system tag[%s] of %s",
                    prefix, systemTag, VmSystemTags.IPV6_PREFIX_TOKEN));
        }
    }

    private void reserveExplicitStaticIps(VmNicParam vmNicParam, StaticIpTagContext staticIpTags) {
        if (staticIpTags == null) {
            return;
        }

        if (!StringUtils.isEmpty(vmNicParam.getIp())) {
            staticIpTags.ipv4.removeFirstOccurrence(vmNicParam.getIp());
        }

        String ip6 = normalizeIpv6(vmNicParam.getIp6());
        if (!StringUtils.isEmpty(ip6)) {
            staticIpTags.ipv6.removeFirstOccurrence(ip6);
        }
    }

    private void bindStaticIpToVmNicParam(VmNicParam vmNicParam, StaticIpTagContext staticIpTags) {
        if (staticIpTags == null) {
            return;
        }

        if (StringUtils.isEmpty(vmNicParam.getIp()) && !staticIpTags.ipv4.isEmpty()) {
            vmNicParam.setIp(staticIpTags.ipv4.pollFirst());
        }
        if (!StringUtils.isEmpty(vmNicParam.getIp())) {
            fillIpv4NetworkInfo(vmNicParam, staticIpTags);
        }

        if (StringUtils.isEmpty(vmNicParam.getIp6()) && !staticIpTags.ipv6.isEmpty()) {
            vmNicParam.setIp6(staticIpTags.ipv6.pollFirst());
        }
        if (!StringUtils.isEmpty(vmNicParam.getIp6())) {
            fillIpv6NetworkInfo(vmNicParam, staticIpTags);
        }
    }

    private void fillIpv4NetworkInfo(VmNicParam vmNicParam, StaticIpTagContext staticIpTags) {
        if (StringUtils.isEmpty(vmNicParam.getGateway())) {
            vmNicParam.setGateway(staticIpTags.ipv4Gateway);
        }
        if (StringUtils.isEmpty(vmNicParam.getNetmask())) {
            vmNicParam.setNetmask(staticIpTags.ipv4Netmask);
        }
    }

    private void fillIpv6NetworkInfo(VmNicParam vmNicParam, StaticIpTagContext staticIpTags) {
        if (StringUtils.isEmpty(vmNicParam.getIpv6Gateway())) {
            vmNicParam.setIpv6Gateway(staticIpTags.ipv6Gateway);
        }
        if (vmNicParam.getIpv6Prefix() == null) {
            vmNicParam.setIpv6Prefix(staticIpTags.ipv6Prefix);
        }
    }

    private String normalizeIpv6(String ip6) {
        if (StringUtils.isEmpty(ip6)) {
            return null;
        }

        ip6 = IPv6NetworkUtils.ipv6TagValueToAddress(ip6);
        return IPv6NetworkUtils.isIpv6Address(ip6) ? IPv6NetworkUtils.getIpv6AddressCanonicalString(ip6) : null;
    }

    private static class StaticIpTagContext {
        Deque<String> ipv4 = new ArrayDeque<>();
        Deque<String> ipv6 = new ArrayDeque<>();
        String ipv4Gateway;
        String ipv4Netmask;
        String ipv6Gateway;
        Integer ipv6Prefix;
        boolean hasIpv4;
        boolean hasIpv6;
    }

}
