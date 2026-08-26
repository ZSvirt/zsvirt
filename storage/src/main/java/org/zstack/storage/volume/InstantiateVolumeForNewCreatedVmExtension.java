package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.core.Completion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.InstantiateRootVolumeForRecoveryMsg;
import org.zstack.header.storage.primary.InstantiateRootVolumeForRecoveryReply;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.storage.encrypt.EncryptedVolumePrimaryStorageAllocatorExtension;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.zstack.core.Platform.operr;

public class InstantiateVolumeForNewCreatedVmExtension implements PreVmInstantiateResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(InstantiateVolumeForNewCreatedVmExtension.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    private EncryptedVolumePrimaryStorageAllocatorExtension encryptedVolumePrimaryStorageAllocatorExtension;

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
    }

    private static Integer parseDeviceId(String url) {
        if (url == null) {
            return null;
        }

        try {
            URI uri = new URI(url);
            MultiValueMap<String, String> m = UriComponentsBuilder.fromUri(uri)
                    .build()
                    .getQueryParams();
            List<String> vals = m.getOrDefault("deviceId", Collections.emptyList());
            return vals.isEmpty() ? null : Integer.valueOf(vals.get(0));
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    protected void doInstantiate(final Iterator<NeedReplyMessage> it, final VmInstanceSpec spec, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        NeedReplyMessage msg = it.next();
        bus.send(msg, new CloudBusCallBack(completion) {
            private int allocateDeviceId(String volumeUuid, String imageUuid) {
                Integer deviceId = new SQLBatchWithReturn<Integer>() {
                    @Override
                    protected Integer scripts() {
                        String url = q(ImageVO.class).eq(ImageVO_.uuid, imageUuid).select(ImageVO_.url).findValue();
                        Integer devId = parseDeviceId(url);
                        if (devId == null) {
                            return null;
                        }

                        boolean allocated = q(VolumeVO.class)
                                .eq(VolumeVO_.vmInstanceUuid, spec.getVmInventory().getUuid())
                                .eq(VolumeVO_.deviceId, devId)
                                .notEq(VolumeVO_.uuid, volumeUuid)
                                .isExists();
                        return allocated ? null : devId;
                    }
                }.execute();

                return deviceId != null ? deviceId : getNextDeviceId();
            }

            private int getNextDeviceId(String volumeUuid, String imageUuid) {
                return imageUuid == null ? getNextDeviceId() : allocateDeviceId(volumeUuid, imageUuid);
            }

            private int getNextDeviceId() {
                List<Integer> devIds = Q.New(VolumeVO.class)
                        .select(VolumeVO_.deviceId)
                        .eq(VolumeVO_.vmInstanceUuid, spec.getVmInventory().getUuid())
                        .notNull(VolumeVO_.deviceId)
                        .orderByAsc(VolumeVO_.deviceId)
                        .listValues();

                for (BeforeGetNextVolumeDeviceIdExtensionPoint e : pluginRgty.getExtensionList(BeforeGetNextVolumeDeviceIdExtensionPoint.class)) {
                    e.beforeGetNextVolumeDeviceId(spec.getVmInventory().getUuid(), devIds);
                }

                BitSet full = new BitSet(devIds.size() + 1);
                for (Integer id : devIds) {
                    full.set(id);
                }
                return full.nextClearBit(0);
            }

            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                String volumeUuid;
                String imageUuid = null;
                if (reply instanceof InstantiateVolumeReply) {
                    VolumeInventory inv = ((InstantiateVolumeReply) reply).getVolume();
                    volumeUuid = inv.getUuid();
                    imageUuid = inv.getRootImageUuid();
                } else if (reply instanceof InstantiateRootVolumeForRecoveryReply) {
                    volumeUuid = ((InstantiateRootVolumeForRecoveryReply) reply).getVolume().getUuid();
                } else if (reply instanceof CreateDataVolumeFromVolumeTemplateReply) {
                    VolumeInventory inv = ((CreateDataVolumeFromVolumeTemplateReply) reply).getInventory();
                    volumeUuid = inv.getUuid();
                    imageUuid = inv.getRootImageUuid();
                } else {
                    throw new CloudRuntimeException("can not be here");
                }

                VolumeVO vo = dbf.findByUuid(volumeUuid, VolumeVO.class);
                if (vo.getType() == VolumeType.Data) {
                    vo.setVmInstanceUuid(vo.isShareable() ? null : spec.getVmInventory().getUuid());
                    vo.setDeviceId(getNextDeviceId(volumeUuid, imageUuid));
                    vo.setActualSize(vo.getActualSize() == null ? 0L : vo.getActualSize());
                } else if (spec.getImageSpec().getInventory() != null) {
                    vo.setActualSize(vo.getActualSize() == null ?
                            spec.getImageSpec().getInventory().getActualSize() : vo.getActualSize());
                }

                vo = dbf.updateAndRefresh(vo);
                List<VmAttachVolumeExtensionPoint> exts = pluginRgty.getExtensionList(VmAttachVolumeExtensionPoint.class);
                for (VmAttachVolumeExtensionPoint ext : exts) {
                    ext.afterInstantiateVolumeForNewCreatedVm(spec.getVmInventory(), VolumeInventory.valueOf(vo));
                }

                vo = dbf.reload(vo);
                VolumeInventory vinv = VolumeInventory.valueOf(vo);
                if (spec.getDestRootVolume().getUuid().equals(vinv.getUuid())) {
                    spec.setDestRootVolume(vinv);
                } else if (VolumeType.Data.toString().equals(vinv.getType())) {
                    // Delete the original volumeInventory, and then re-add latest volumeInventory, the latest volumeInventory contains more attributes
                    spec.getDestDataVolumes().removeIf(volumeInventory -> volumeUuid.equals(volumeInventory.getUuid()));
                    spec.getDestDataVolumes().add(vinv);
                } else if (VolumeType.Cache.toString().equals(vinv.getType())) {
                    spec.getDestCacheVolumes().removeIf(volumeInventory -> volumeUuid.equals(volumeInventory.getUuid()));
                    spec.getDestCacheVolumes().add(vinv);
                }

                logger.debug(String.format("successfully instantiated volume%s", JSONObjectUtil.toJsonString(vinv)));

                doInstantiate(it, spec, completion);
            }
        });
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        if (VmInstanceConstant.VmOperation.NewCreate != spec.getCurrentVmOperation()) {
            completion.success();
            return;
        }

        List<NeedReplyMessage> msgs = new ArrayList<>();
        for (VolumeInventory volume : spec.getDestDataVolumes()) {
            msgs.add(fillMsg(new InstantiateVolumeMsg(), volume, spec));
        }

        for (VolumeInventory volume : spec.getDestCacheVolumes()) {
            msgs.add(fillMsg(new InstantiateVolumeMsg(), volume, spec));
        }

        ImageSpec image = spec.getImageSpec();
        boolean recovering = false;
        try {
            recovering = image.getSelectedBackupStorage().getInstallPath().startsWith("nbd://");
        } catch (NullPointerException ignored) {
        }

        LinkedHashSet<String> systemTags = new LinkedHashSet<>();
        if (spec.getRootVolumeSystemTags() != null) {
            systemTags.addAll(spec.getRootVolumeSystemTags());
        }
        if (spec.getRootDisk().getSystemTags() != null) {
            systemTags.addAll(spec.getRootDisk().getSystemTags());
        }

        if (recovering) {
            InstantiateVolumeMsg cmsg = fillMsg(new InstantiateRootVolumeForRecoveryMsg(), spec.getDestRootVolume(), spec);
            ((InstantiateRootVolumeForRecoveryMsg) cmsg).setSelectedBackupStorage(image.getSelectedBackupStorage());
            cmsg.setSystemTags(new ArrayList<>(systemTags));
            msgs.add(cmsg);
        } else if (image.getInventory() != null && ImageMediaType.RootVolumeTemplate.toString().equals(image.getInventory().getMediaType())) {
            InstantiateVolumeMsg rmsg = fillMsg(new InstantiateRootVolumeMsg(), spec.getDestRootVolume(), spec);
            ((InstantiateRootVolumeMsg) rmsg).setTemplateSpec(image);
            ((InstantiateRootVolumeMsg) rmsg).setEncryptedVolumeBackupRestore(
                    spec.getRootDisk().isVolumeBackupTemplate()
                            && Boolean.TRUE.equals(spec.getRootDisk().getEncrypted()));
            rmsg.setSystemTags(new ArrayList<>(systemTags));
            msgs.add(rmsg);
        } else {
            InstantiateVolumeMsg rmsg = fillMsg(new InstantiateVolumeMsg(), spec.getDestRootVolume(), spec);
            rmsg.setSystemTags(new ArrayList<>(systemTags));
            msgs.add(rmsg);
        }

        List<DiskAO> templateDataDisks = spec.getTemplateDeprecatedDisksSpecs();
        if (spec.getDataVolumeTemplateUuids() != null || !templateDataDisks.isEmpty()) {
            String accountUuid = acntMgr.getOwnerAccountUuidOfResource(spec.getVmInventory().getUuid());
            if (accountUuid == null) {
                throw new CloudRuntimeException(String.format("accountUuid for vm[uuid:%s] is null", spec.getVmInventory().getUuid()));
            }

            String defaultPsUuid = spec.getRequiredPrimaryStorageUuidForDataVolume();
            if (defaultPsUuid == null) {
                defaultPsUuid = spec.getDestRootVolume().getPrimaryStorageUuid();
            }

            if (spec.getDataVolumeTemplateUuids() != null) {
                for (String imageUuid : spec.getDataVolumeTemplateUuids()) {
                    List<String> perImageTags = spec.getDataVolumeFromTemplateSystemTags() == null
                            ? null : spec.getDataVolumeFromTemplateSystemTags().get(imageUuid);
                    msgs.add(buildCreateDataVolumeFromTemplateMsg(spec, imageUuid, defaultPsUuid, perImageTags,
                            accountUuid, null, false));
                }
            }

            for (DiskAO diskAO : templateDataDisks) {
                String psUuid = diskAO.getPrimaryStorageUuid() != null ? diskAO.getPrimaryStorageUuid() : defaultPsUuid;
                if (diskAO.getPrimaryStorageUuid() == null && Boolean.TRUE.equals(diskAO.getEncrypted())
                        && encryptedVolumePrimaryStorageAllocatorExtension
                        .isEncryptedVolumeUnsupportedPrimaryStorage(psUuid)) {
                    completion.fail(operr("no primary storage candidate is available"));
                    return;
                }
                msgs.add(buildCreateDataVolumeFromTemplateMsg(spec, diskAO.getTemplateUuid(), psUuid,
                        diskAO.getSystemTags(), accountUuid, diskAO.getEncrypted(),
                        diskAO.isVolumeBackupTemplate() && Boolean.TRUE.equals(diskAO.getEncrypted())));
            }
        }

        doInstantiate(msgs.iterator(), spec, completion);
    }

    private CreateDataVolumeFromVolumeTemplateMsg buildCreateDataVolumeFromTemplateMsg(
            VmInstanceSpec spec, String imageUuid, String psUuid, List<String> systemTags, String accountUuid,
            Boolean encrypted, boolean encryptedVolumeBackupRestore) {
        CreateDataVolumeFromVolumeTemplateMsg cmsg = new CreateDataVolumeFromVolumeTemplateMsg();
        cmsg.setHostUuid(spec.getDestHost().getUuid());
        cmsg.setImageUuid(imageUuid);
        cmsg.setPrimaryStorageUuid(psUuid);

        Tuple t = Q.New(ImageVO.class).eq(ImageVO_.uuid, imageUuid).select(ImageVO_.name, ImageVO_.description).findTuple();

        cmsg.setName("data-volume-" + t.get(0, String.class));
        cmsg.setDescription(t.get(1, String.class));
        cmsg.setAccountUuid(accountUuid);
        cmsg.setSystemTags(systemTags);
        cmsg.setEncrypted(encrypted);
        cmsg.setEncryptedVolumeBackupRestore(encryptedVolumeBackupRestore);
        bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
        return cmsg;
    }

    private InstantiateVolumeMsg fillMsg(InstantiateVolumeMsg msg, VolumeInventory volume, VmInstanceSpec spec) {
        msg.setVolumeUuid(volume.getUuid());
        msg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
        msg.setHostUuid(spec.getDestHost().getUuid());
        msg.setPrimaryStorageAllocated(true);
        msg.setSkipIfExisting(spec.isInstantiateResourcesSkipExisting());
        msg.setAllocatedInstallUrl(spec.getAllocatedUrlFromVolumeSpecs(volume.getUuid()));
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volume.getUuid());
        return msg;
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        /* volumes will be deleted when VmAllocateVolumeFlow rolls back */
        completion.success();
    }
}
