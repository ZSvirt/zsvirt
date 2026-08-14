package org.zstack.ovf;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.*;
import org.zstack.header.longjob.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.ExportImageFromBackupStorageMsg;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.image.ImageSystemTags;
import org.zstack.longjob.LongJobUtils;
import org.zstack.ovf.api.*;
import org.zstack.ovf.datatype.*;
import org.zstack.ovf.message.*;
import org.zstack.portal.apimediator.ApiMediator;
import org.zstack.storage.backup.imagestore.*;
import org.zstack.storage.snapshot.VolumeSnapshotSystemTags;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.header.image.ImageConstant.ImageMediaType.DataVolumeTemplate;
import static org.zstack.header.image.ImageConstant.ImageMediaType.RootVolumeTemplate;
import static org.zstack.header.image.ImageConstant.VMDK_FORMAT_STRING;
import static org.zstack.ovf.CreateVmInstanceFromOvfLongJob.isCurrentOvfLongJobCancelled;
import static org.zstack.ovf.datatype.CreateVmFromOvfCanonicalEvents.STAGE_READY;
import static org.zstack.ovf.datatype.CreateVmFromOvfCanonicalEvents.STAGE_VM_CREATING;
import static org.zstack.ovf.datatype.CreateVmFromOvfParamType.*;
import static org.zstack.ovf.datatype.OvfErrors.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by Qi Le on 2022/3/3
 */
public class OvfManagerImpl extends AbstractService implements OvfManager {
    private static final CLogger logger = Utils.getLogger(OvfManagerImpl.class);

    @Autowired
    private CloudBus bus;

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private ThreadFacade thdf;

    @Autowired
    private TagManager tagMgr;

    @Autowired
    private ApiMediator apiMediator;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof ParseOvfMsg) {
            handle((ParseOvfMsg) msg);
        } else if (msg instanceof CreateVmInstanceFromOvfMsg) {
            handle((CreateVmInstanceFromOvfMsg) msg);
        } else if (msg instanceof DeleteOvaPackageMsg) {
            handle((DeleteOvaPackageMsg) msg);
        } else if (msg instanceof ExportVmOvaPackageMsg) {
            handle((ExportVmOvaPackageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(ExportVmOvaPackageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("ova-package-%s", msg.getResourceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                ExportVmOvaPackageReply reply = new ExportVmOvaPackageReply();
                doExportVmOva(msg, new ReturnValueCompletion<ImagePackageInventory>(chain) {
                    @Override
                    public void success(ImagePackageInventory returnValue) {
                        reply.setInventory(returnValue);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("export-vm-%s", msg.getVmUuid());
            }
        });
    }

    private void doExportVmOva(ExportVmOvaPackageMsg msg, ReturnValueCompletion<ImagePackageInventory> exportCompletion) {
        VmInstanceVO vm = dbf.findByUuid(msg.getVmUuid(), VmInstanceVO.class);
        final Map<String, ImageInventory> volumeImageMap = Collections.synchronizedMap(new LinkedHashMap<>());
        vm.getAllDiskVolumes().stream()
                .sorted(Comparator.comparing(VolumeAO::getDeviceId))
                .forEachOrdered(vol -> volumeImageMap.put(vol.getUuid(), null));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("export-vm-%s", msg.getVmUuid()));
        chain.enableProgressReport();
        chain.then(new ShareFlow() {
            Document ovf = null;
            ImagePackageVO ova = null;
            final AtomicLong diskTotalSize = new AtomicLong(0L);
            final List<String> createdImageUuids = new ArrayList<>();

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    final String __name__ = "calculate-all-volumes-size";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(vm.getAllDiskVolumes()).step((volume, completion) -> {
                            SyncVolumeSizeMsg syncMsg = new SyncVolumeSizeMsg();
                            syncMsg.setVolumeUuid(volume.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(syncMsg, VolumeConstant.SERVICE_ID,
                                    volume.getPrimaryStorageUuid()
                            );
                            bus.send(syncMsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        completion.addError(reply.getError());
                                    } else {
                                        SyncVolumeSizeReply sr = reply.castReply();
                                        diskTotalSize.addAndGet(sr.getActualSize());
                                    }

                                    completion.done();
                                }
                            });
                        }, 3).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.isEmpty()) {
                                    dbf.reload(vm);
                                    trigger.next();
                                } else {
                                    trigger.fail(multiErr(errorCodeList));
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    final String __name__ = "check-bs-capacity";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ImageStoreBackupStorageVO imageStore = dbf.findByUuid(
                                msg.getBackupStorageUuid(),
                                ImageStoreBackupStorageVO.class
                        );
                        long totalSize = diskTotalSize.get();
                        if (imageStore.getAvailableCapacity() < totalSize) {
                            trigger.fail(operr("backup storage[uuid: %s] does not have enough available capacity " +
                                            "for exporting vm[uuid: %s], required capacity is: %d",
                                    msg.getBackupStorageUuid(), msg.getVmUuid(), totalSize
                            ));
                            return;
                        }
                        trigger.next();
                    }
                });

                flow(new Flow() {
                    final String __name__ = "prepare-export-record";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ova = new ImagePackageVO();
                        ova.setAccountUuid(msg.getSession().getAccountUuid());
                        ova.setUuid(msg.getResourceUuid());
                        ova.setName(msg.getName());
                        ova.setDescription(msg.getDescription());
                        ova.setVmUuid(msg.getVmUuid());
                        ova.setBackupStorageUuid(msg.getBackupStorageUuid());
                        ova.setState(ImagePackageState.Exporting);
                        ova.setFormat(OvfConstant.OVA_FORMAT);
                        ova = dbf.persistAndRefresh(ova);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (dbf.isExist(ova.getUuid(), ImagePackageVO.class)) {
                            dbf.remove(ova);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    final String __name__ = String.format("generate-ovf-for-vm-%s", msg.getVmUuid());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ovf = OvfHelper.generateOvfFromVm(VmInstanceInventory.valueOf(vm));
                        trigger.next();
                    }
                });

                flow(new Flow() {
                    final String __name__ = String.format("create-volumes-images-for-exporting-vm-%s", msg.getVmUuid());

                    final List<String> rollbackImages = Collections.synchronizedList(new ArrayList<>());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(vm.getAllDiskVolumes()).step((disk, completion) -> {
                            AddImageMessage cmsg = buildMsg(disk, vm);
                            rollbackImages.add(cmsg.getResourceUuid());
                            bus.makeLocalServiceId((Message) cmsg, ImageConstant.SERVICE_ID);
                            bus.send((NeedReplyMessage) cmsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        completion.addError(reply.getError());
                                        completion.done();
                                        return;
                                    }
                                    ImageReply creply = reply.castReply();
                                    ImageInventory image = creply.getInventory();
                                    volumeImageMap.put(disk.getUuid(), image);
                                    tagMgr.createNonInherentSystemTag(image.getUuid(),
                                            ImageSystemTags.IMAGE_CREATED_BY_SYSTEM.getTagFormat(),
                                            ImageVO.class.getSimpleName()
                                    );
                                    // TODO refactor
                                    VolumeSnapshotVO snapshot = Q.New(VolumeSnapshotVO.class)
                                            .eq(VolumeSnapshotVO_.volumeUuid, disk.getUuid())
                                            .orderBy(VolumeSnapshotVO_.createDate, SimpleQuery.Od.DESC)
                                            .limit(1).find();
                                    if (snapshot != null) {
                                        tagMgr.createNonInherentSystemTag(snapshot.getUuid(),
                                                VolumeSnapshotSystemTags.VOLUMESNAPSHOT_CREATED_BY_SYSTEM.getTagFormat(),
                                                VolumeSnapshotVO.class.getSimpleName()
                                        );
                                    }

                                    completion.done();
                                }
                            });
                        }, 3).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.isEmpty()) {
                                    volumeImageMap.values().forEach(img -> createdImageUuids.add(img.getUuid()));
                                    trigger.next();
                                } else {
                                    trigger.fail(multiErr(errorCodeList));
                                }
                            }
                        });
                    }
                    private AddImageMessage buildMsg(VolumeVO disk, VmInstanceVO vm) {
                        AddImageMessage cmsg;
                        if (disk.getType() == VolumeType.Root) {
                            CreateRootVolumeTemplateFromRootVolumeMsg csmsg = new CreateRootVolumeTemplateFromRootVolumeMsg();
                            csmsg.setRootVolumeUuid(disk.getUuid());
                            csmsg.setGuestOsType(vm.getGuestOsType());
                            csmsg.setArchitecture(vm.getArchitecture());
                            csmsg.setPlatform(vm.getPlatform());
                            csmsg.setVirtio(VmSystemTags.VIRTIO.hasTag(vm.getUuid()));
                            cmsg = csmsg;
                        } else {
                            CreateDataVolumeTemplateFromVolumeMsg csmsg = new CreateDataVolumeTemplateFromVolumeMsg();
                            csmsg.setVolumeUuid(disk.getUuid());
                            cmsg = csmsg;
                        }

                        cmsg.setName(String.format("for-export-vm-%s-disk-%s", vm.getUuid(), disk.getDeviceId()));
                        cmsg.setDescription(cmsg.getName());
                        cmsg.setSession(msg.getSession());
                        cmsg.setResourceUuid(getUuid());

                        cmsg.setBackupStorageUuids(Collections.singletonList(msg.getBackupStorageUuid()));
                        bus.makeLocalServiceId((NeedReplyMessage) cmsg, ImageConstant.SERVICE_ID);
                        return cmsg;
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        new While<>(rollbackImages).each((imageUuid, completion) -> {
                            ImageDeletionMsg dmsg = new ImageDeletionMsg();
                            dmsg.setImageUuid(imageUuid);
                            dmsg.setForceDelete(true);
                            dmsg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
                            bus.makeTargetServiceIdByResourceUuid(dmsg, ImageConstant.SERVICE_ID, dmsg.getImageUuid());
                            bus.send(dmsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    completion.done();
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

                flow(new Flow() {
                    final String __name__ = "export-volume-images";

                    final List<String> rollbackExportedImages = Collections.synchronizedList(new ArrayList<>());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(createdImageUuids).step((imgUuid, completion) -> {
                            ExportImageFromBackupStorageMsg emsg = new ExportImageFromBackupStorageMsg();
                            emsg.setExportFormat(VMDK_FORMAT_STRING);
                            emsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                            emsg.setImageUuid(imgUuid);
                            rollbackExportedImages.add(imgUuid);
                            bus.makeTargetServiceIdByResourceUuid(emsg, BackupStorageConstant.SERVICE_ID, imgUuid);
                            bus.send(emsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        completion.addError(reply.getError());
                                    }
                                    completion.done();
                                }
                            });
                        }, 3).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(multiErr(errorCodeList));
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        new While<>(rollbackExportedImages).step((imageUuid, completion) -> {
                            DeleteExportedImageFromImageStoreBackupStorageMsg dmsg = new DeleteExportedImageFromImageStoreBackupStorageMsg();
                            dmsg.setImageUuid(imageUuid);
                            dmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                            dmsg.setExportFormat(VMDK_FORMAT_STRING);
                            bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, imageUuid);
                            bus.send(dmsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    completion.done();
                                }
                            });
                        }, 3).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.rollback();
                            }
                        });

                    }
                });

                flow(new NoRollbackFlow() {
                    final String __name__ = "fill-ovf-file-reference";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<ImageBackupStorageRefVO> refs = Q.New(ImageBackupStorageRefVO.class)
                                .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                                .in(ImageBackupStorageRefVO_.imageUuid, createdImageUuids)
                                .list();
                        Map<String, ImageBackupStorageRefVO> refMap = refs.stream()
                                .collect(Collectors.toMap(ImageBackupStorageRefVO::getImageUuid, ref -> ref));
                        List<ImageBackupStorageRefInventory> refInventories = createdImageUuids.stream()
                                .map(imgUuid -> ImageBackupStorageRefInventory.valueOf(refMap.get(imgUuid)))
                                .collect(Collectors.toList());
                        OvfHelper.fixReferences(ovf, refInventories);
                        trigger.next();
                    }
                });

                flow(new Flow() {
                    final String __name__ = "pack-images-as-ova-package";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        PackExportedImagesOnImageStoreMsg packageMsg = new PackExportedImagesOnImageStoreMsg();
                        packageMsg.setConfigFileContent(OvfHelper.writeOvfString(ovf));
                        packageMsg.setConfigFileFormat(OvfConstant.OVF_FILE_SUFFIX);
                        packageMsg.setPackageFormat(OvfConstant.OVA_FILE_SUFFIX);
                        packageMsg.setPackageName(vm.getUuid());
                        packageMsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        packageMsg.setImageUuids(createdImageUuids);
                        packageMsg.setImageExportFormat(VMDK_FORMAT_STRING);
                        bus.makeTargetServiceIdByResourceUuid(packageMsg, BackupStorageConstant.SERVICE_ID,
                                ova.getUuid());
                        bus.send(packageMsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }
                                PackExportedImagesOnImageStoreReply packageReply = reply.castReply();
                                // it is the caller's responsibility to update export url.
                                String vmName = Q.New(VmInstanceVO.class).select(VmInstanceVO_.name)
                                        .eq(VmInstanceVO_.uuid, msg.getVmUuid()).findValue();
                                ova.setExportUrl(ImageStoreHelper.ImageStoreExportUrl.
                                        addNameToExportUrl(packageReply.getExportUrl(), vmName));
                                ova.setSize(packageReply.getSize());
                                ova.setMd5Sum(packageReply.getMd5Sum());
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        DeleteOvaPackageMsg dmsg = new DeleteOvaPackageMsg();
                        dmsg.setUuid(ova.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, SERVICE_ID, ova.getUuid());
                        bus.send(dmsg);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    final String __name__ = "delete-temp-images-and-exported-images";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        createdImageUuids.forEach(imgUuid -> {
                            ImageDeletionMsg msg = new ImageDeletionMsg();
                            msg.setImageUuid(imgUuid);
                            msg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
                            msg.setForceDelete(true);
                            bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, imgUuid);
                            bus.send(msg);
                        });
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        ova.setState(ImagePackageState.Exported);
                        ova = dbf.updateAndRefresh(ova);
                        exportCompletion.success(ImagePackageInventory.valueOf(ova));
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        exportCompletion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(ParseOvfMsg msg) {
        ParseOvfReply reply = new ParseOvfReply();
        reply.setOvfInfo(parseOvf(msg.getXmlBase64()));
        bus.reply(msg, reply);
    }

    private void handle(CreateVmInstanceFromOvfMsg msg) {
        CreateVmInstanceFromOvfReply createReply = new CreateVmInstanceFromOvfReply();

        ParseOvfMsg message = new ParseOvfMsg();
        message.setXmlBase64(msg.getXmlBase64());
        bus.makeLocalServiceId(message, SERVICE_ID);
        bus.send(message, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    createReply.setError(err(FAIL_TO_PARSE_OVF_XML, "failed to parse OVF XML string")
                            .withCause(reply.getError()));
                    bus.reply(msg, createReply);
                    return;
                }

                ParseOvfReply parseOvfReply = reply.castReply();
                ErrorableValue<CreateVmFromOvfBundle> bundleHolder = CreateVmFromOvfBundle.builder()
                        .withCreateMessage(msg)
                        .withOvfInfo(parseOvfReply.getOvfInfo())
                        .withApiCreateVmMessageValidator(this::validateApiMessage)
                        .build();
                if (!bundleHolder.isSuccess()) {
                    createReply.setError(bundleHolder.error);
                    bus.reply(msg, createReply);
                    return;
                }

                createVmInstanceFromOvfBundle(bundleHolder.result, new ReturnValueCompletion<VmInstanceInventory>(msg) {
                    @Override
                    public void success(VmInstanceInventory returnValue) {
                        createReply.setInventory(returnValue);
                        bus.reply(msg, createReply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        createReply.setError(errorCode);
                        bus.reply(msg, createReply);
                    }
                });
            }

            private void validateApiMessage(APICreateVmInstanceMsg message) {
                apiMediator.getProcesser().process(message);
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIParseOvfMsg) {
            handle((APIParseOvfMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromOvfMsg) {
            handle((APICreateVmInstanceFromOvfMsg) msg);
        } else if (msg instanceof APIUpdateImagePackageMsg) {
            handle((APIUpdateImagePackageMsg) msg);
        } else if (msg instanceof APIDeleteImagePackageMsg) {
            handle((APIDeleteImagePackageMsg) msg);
        } else if (msg instanceof APIExportVmOvaPackageMsg) {
            handle((APIExportVmOvaPackageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIExportVmOvaPackageMsg msg) {
        ExportVmOvaPackageMsg emsg = new ExportVmOvaPackageMsg(msg);
        bus.makeTargetServiceIdByResourceUuid(emsg, SERVICE_ID, msg.getResourceUuid());
        bus.send(emsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                APIExportVmOvaPackageEvent event = new APIExportVmOvaPackageEvent(msg.getId());
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                } else {
                    ExportVmOvaPackageReply eReply = reply.castReply();
                    event.setInventory(eReply.getInventory());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(DeleteOvaPackageMsg msg) {
        DeleteOvaPackageReply reply = new DeleteOvaPackageReply();
        ImagePackageVO ova = dbf.findByUuid(msg.getUuid(), ImagePackageVO.class);
        if (ova == null) {
            reply.setError(operr("ova package[uuid: %s] not found.", msg.getUuid()));
            bus.reply(msg, reply);
            return;
        } else if (ova.getExportUrl() == null) {
            dbf.remove(ova);
            bus.reply(msg, reply);
            return;
        }
        DeleteImagePackageOnImageStoreMsg dmsg = new DeleteImagePackageOnImageStoreMsg();
        dmsg.setExportUrl(ova.getExportUrl());
        dmsg.setBackupStorageUuid(ova.getBackupStorageUuid());
        bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, ova.getUuid());
        bus.send(dmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply rpl) {
                if (!rpl.isSuccess()) {
                    reply.setError(rpl.getError());
                    bus.reply(msg, reply);
                    return;
                }
                dbf.remove(ova);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIDeleteImagePackageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("ova-package-%s", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                DeleteOvaPackageMsg dmsg = new DeleteOvaPackageMsg();
                dmsg.setUuid(msg.getUuid());
                bus.makeTargetServiceIdByResourceUuid(dmsg, SERVICE_ID, msg.getUuid());
                bus.send(dmsg, new CloudBusCallBack(msg) {
                    @Override
                    public void run(MessageReply reply) {
                        APIDeleteImagePackageEvent event = new APIDeleteImagePackageEvent(msg.getId());
                        if (reply.isSuccess()) {
                            bus.publish(event);
                            chain.next();
                            return;
                        }
                        event.setError(reply.getError());
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-%s", getSyncSignature());
            }
        });
    }

    private void handle(APIUpdateImagePackageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("ova-package-%s", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateImagePackageEvent event = new APIUpdateImagePackageEvent(msg.getId());
                ImagePackageVO ova = dbf.findByUuid(msg.getUuid(), ImagePackageVO.class);
                boolean updated = false;
                if (StringUtils.isNotBlank(msg.getName())) {
                    ova.setName(msg.getName());
                    updated = true;
                }
                if (StringUtils.isNotBlank(msg.getDescription())) {
                    ova.setDescription(msg.getDescription());
                    updated = true;
                }
                if (updated) {
                    ova = dbf.updateAndRefresh(ova);
                }
                event.setInventory(ImagePackageInventory.valueOf(ova));
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("delete-%s", getSyncSignature());
            }
        });

    }

    private void handle(APIParseOvfMsg msg) {
        APIParseOvfReply reply = new APIParseOvfReply();
        reply.setOvfInfo(parseOvf(msg.getXmlBase64()));
        bus.reply(msg, reply);
    }

    private void handle(APICreateVmInstanceFromOvfMsg msg) {
        APICreateVmInstanceFromOvfEvent event = new APICreateVmInstanceFromOvfEvent(msg.getId());
        CreateVmInstanceFromOvfMsg innerMessage = CreateVmInstanceFromOvfMsg.fromApiMessage(msg);
        bus.makeTargetServiceIdByResourceUuid(innerMessage, SERVICE_ID, msg.getResourceUuid());
        bus.send(innerMessage, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }
                CreateVmInstanceFromOvfReply caseReply = reply.castReply();
                event.setInventory(caseReply.getInventory());
                bus.publish(event);
            }
        });
    }

    private OvfInfo parseOvf(String xmlBase64) {
        String xmlContent = new String(Base64.decodeBase64(xmlBase64));
        Document document;
        try {
            document = DocumentHelper.parseText(xmlContent);
        } catch (DocumentException e) {
            throw new OperationFailureException(Platform.operr("Failed to read ovf file."));
        }
        return OvfHelper.parseOvf(document);
    }

    /**
     * VM instance creation step:
     * 1. map image info which from APICreateVmInstanceFromOvfMsg to OVF disk spec
     * 2. create add image long jobs
     * 3. track and wait the long jobs
     * 4. create VM instance with data volume from CreateVmFromOvfBundle
     * 5. delete image if needed
     */
    @SuppressWarnings("rawtypes")
    private void createVmInstanceFromOvfBundle(final CreateVmFromOvfBundle bundle,
            ReturnValueCompletion<VmInstanceInventory> completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("create-vm-from-ovf-bundle");
        chain.then(new NoRollbackFlow() {
            String __name__ = "report-ready-state";

            @Override
            public void run(FlowTrigger trigger, Map map) {
                new OvfImageUploadTracker.OvfTrackCanonicalEvent(
                        STAGE_READY, bundle.getVmInstanceUuid(), Collections.emptyList()).fire();
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "map-image-info-to-ovf-disk-info";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                OvfInfo ovf = bundle.getOvfInfo();
                List<OvfDiskInfo> disks = ovf.getDisks();

                List<String> diskIds = disks.stream().map(OvfDiskInfo::getFileRef).collect(Collectors.toList());
                List<CreateVmFromOvfImageParam> invalidParams = bundle.getParams().stream()
                        .filter(param -> !diskIds.contains(param.getOvfId()))
                        .collect(Collectors.toList());
                if (!invalidParams.isEmpty()) {
                    trigger.fail(err(INVALID_IMAGE_INFO, "invalid ovfId: %s", invalidParams));
                    return;
                }

                disks.sort(Comparator.comparingInt(OvfDiskInfo::getIndex));
                disks.forEach(this::mapDiskResourceInfo);
                bundle.setHasDataVolumes(disks.size() > 1);
                trigger.next();
            }

            private void mapDiskResourceInfo(OvfDiskInfo disk) {
                Optional<CreateVmFromOvfImageParam> optional = bundle.getParams().stream()
                        .filter(param -> Objects.equals(param.getOvfId(), disk.getFileRef()))
                        .findFirst();
                String ovfId = disk.getFileRef(); // disk.fileRef = file.ovfId
                String fileName = disk.getFileName();
                CreateVmFromOvfImageParam param = optional.orElse(null);
                if (param == null) {
                    param = createDefaultUploadParam(ovfId);
                    bundle.getParams().add(param);
                }
                param.setFileName(fileName);
                param.setIndex(disk.getIndex());
                bundle.getDiskParamMap().put(ovfId, param);
            }

            private CreateVmFromOvfImageParam createDefaultUploadParam(String ovfId) {
                CreateVmFromOvfImageParam param = new CreateVmFromOvfImageParam();
                param.setOvfId(ovfId);
                param.setType(CreateVmFromOvfParamType.Upload);
                return param;
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "check-cancel-signal-before-adding-images";

            @Override
            public boolean skip(Map data) {
                return bundle.getCurrentApiId() == null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                boolean cancelled = isCurrentOvfLongJobCancelled(bundle.getCurrentApiId());
                if (cancelled) {
                    trigger.fail(err(JOB_CANCELLED, "cancel create OVF VM process before adding images"));
                    return;
                }

                trigger.next();
            }
        }).then(new Flow() {
            String __name__ = "create-add-image-long-jobs";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<CreateVmFromOvfImageParam> needAddLongJobParams = bundle.getParams().stream()
                        .filter(param -> param.getType() == Upload || param.getType() == Download)
                        .collect(Collectors.toList());
                if (needAddLongJobParams.isEmpty()) {
                    trigger.next();
                    return;
                }

                logger.debug(String.format("create %d add image long jobs", needAddLongJobParams.size()));

                new While<>(needAddLongJobParams).step((param, completion1) -> {
                    SubmitLongJobMsg longJobMsg = createAddImageLongJobMsg(param);

                    String imageUuid = longJobMsg.getTargetResourceUuid();
                    bus.makeTargetServiceIdByResourceUuid(longJobMsg, LongJobConstants.SERVICE_ID, imageUuid);
                    bus.send(longJobMsg, new CloudBusCallBack(completion1) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                completion1.addError(reply.getError());
                                return;
                            }
                            SubmitLongJobReply submitReply = reply.castReply();
                            param.setLongJobUuid(submitReply.getInventory().getUuid());
                            completion1.done();
                        }
                    });
                }, 3).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.hasError()) {
                            trigger.fail(multiErr(errorCodeList));
                            return;
                        }
                        trigger.next();
                    }
                });
            }

            @SuppressWarnings("unchecked")
            SubmitLongJobMsg createAddImageLongJobMsg(CreateVmFromOvfImageParam param) {
                String imageUuid = Platform.getUuid();
                OvfInfo ovf = bundle.getOvfInfo();
                param.setUuid(imageUuid);

                APIAddImageMsg addImageMsg = new APIAddImageMsg();
                addImageMsg.setName(param.getOvfId());
                addImageMsg.setUrl(Download.equals(param.getType()) ?
                        param.getUrl() : "upload://ovf-" + param.getIndex());
                addImageMsg.setBackupStorageUuids(Arrays.asList(bundle.getBackupStorageUuid()));
                addImageMsg.setFormat(VMDK_FORMAT_STRING);
                if (param.isImageOfRootVolume()) {
                    addImageMsg.setMediaType(RootVolumeTemplate.toString());

                    Map<String, String> map = map(e(ImageSystemTags.BOOT_MODE_TOKEN, getBootModeFromOvf().name()));
                    addImageMsg.addSystemTag(ImageSystemTags.BOOT_MODE.instantiateTag(map));
                } else {
                    addImageMsg.setMediaType(DataVolumeTemplate.toString());
                }
                addImageMsg.setResourceUuid(imageUuid);
                addImageMsg.setSession(bundle.getSession());
                addImageMsg.setPlatform(ovf.getPreAnalysisInfo().getInferredPlatform().toString());
                addImageMsg.setGuestOsType(OvfHelper.getGuestOsTypeFromOVF(ovf));
                // Most VM imported from VMWare are booted with "bus = ide" disk driver, without VirtIO driver.
                addImageMsg.setVirtio(false);

                ImageArchitecture arch = ovf.getPreAnalysisInfo().getInferredArchitecture();
                if (arch != null) {
                    addImageMsg.setArchitecture(arch.toString());
                }

                SubmitLongJobMsg msg = new SubmitLongJobMsg();
                msg.setTargetResourceUuid(imageUuid);
                msg.setName(String.format("add image: %s for creating VM from OVF", imageUuid));
                msg.setJobName(APIAddImageMsg.class.getSimpleName());
                msg.setJobData(JSONObjectUtil.toJsonString(addImageMsg));
                msg.setAccountUuid(bundle.getInnerMessage().getAccountUuid());
                msg.setParentUuid(CreateVmInstanceFromOvfLongJob.findCurrentLongJobUuid(bundle.getCurrentApiId()));
                return msg;
            }

            private ImageBootMode getBootModeFromOvf() {
                String firmware = bundle.getOvfInfo().getSystemInfo().getFirmwareType();
                if (firmware == null) {
                    return ImageBootMode.Legacy;
                }

                try {
                    return OvfFirmware.valueOf(firmware.toUpperCase()).toImageBootMode();
                } catch (IllegalArgumentException e) {
                    throw new OperationFailureException(err(INVALID_OVF_XML,
                            "invalid firmware parameter: %s", firmware
                    ));
                }
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                List<CreateVmFromOvfImageParam> longJobParam = bundle.getParams().stream()
                        .filter(param -> param.getLongJobUuid() != null)
                        .collect(Collectors.toList());
                boolean cancelManually = trigger.getErrorCode().isError(JOB_CANCELLED);

                new While<>(longJobParam).each(this::cancelAddImageLongJob)
                        .run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.hasError()) {
                                    logger.warn(
                                            String.format("failed to cancel add image long jobs but still continue: %s",
                                                    multiErr(errorCodeList).getReadableDetails()
                                            ));
                                }

                                if (cancelManually || !bundle.isDeleteImageOnFail()) {
                                    trigger.rollback();
                                    return;
                                }
                                // ignore error code list and continue rollback
                                List<CreateVmFromOvfImageParam> imageParams = bundle.getParams().stream()
                                        .filter(param -> param.getUuid() != null)
                                        .collect(Collectors.toList());
                                deleteImages(imageParams, new WhileDoneCompletion(trigger) {
                                    @Override
                                    public void done(ErrorCodeList errorCodeList) {
                                        if (!errorCodeList.getCauses().isEmpty()) {
                                            logger.warn(String.format(
                                                    "failed to delete image but the rollback continues: %s",
                                                    errorCodeList.getCauses()
                                            ));
                                        }
                                        // ignore error code list and continue rollback
                                        trigger.rollback();
                                    }
                                });
                            }
                        });
            }

            void cancelAddImageLongJob(CreateVmFromOvfImageParam param, WhileCompletion completion) {
                String longJobUuid = param.getLongJobUuid();
                LongJobVO job = Q.New(LongJobVO.class).eq(LongJobVO_.uuid, longJobUuid).find();
                if (job == null || LongJobUtils.jobCompleted(job)) {
                    completion.done();
                    return;
                }

                CancelLongJobMsg msg = new CancelLongJobMsg();
                msg.setUuid(longJobUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, LongJobConstants.SERVICE_ID, longJobUuid);
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.addError(reply.getError());
                            logger.warn(String.format(
                                    "failed to cancel long job[uuid:%s] but the rollback continues", longJobUuid));
                        }
                        completion.done();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "wait-and-track-add-image-long-jobs";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                OvfImageUploadTracker tracker = new OvfImageUploadTracker(bundle);
                if (bundle.getCurrentApiId() != null) {
                    tracker.registerCancelChecker(() -> isCurrentOvfLongJobCancelled(bundle.getCurrentApiId()));
                }

                tracker.waitForCompleted(new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
                tracker.startTrack();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "check-cancel-signal-before-creating-VM";

            @Override
            public boolean skip(Map data) {
                return bundle.getCurrentApiId() == null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                boolean cancelled = isCurrentOvfLongJobCancelled(bundle.getCurrentApiId());
                if (cancelled) {
                    trigger.fail(err(JOB_CANCELLED, "cancel create OVF VM process before creating VM"));
                    return;
                }

                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "create-VM-instance-from-OVF-template";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                emitVMCreatingStateEvent();

                CreateVmInstanceMsg vmMsg = bundle.getInnerMessage();
                vmMsg.setResourceUuid(bundle.getVmInstanceUuid());
                vmMsg.setAccountUuid(bundle.getSession().getAccountUuid());

                String rootImageUuid = bundle.getParams().stream()
                        .filter(CreateVmFromOvfImageParam::isImageOfRootVolume)
                        .map(CreateVmFromOvfImageParam::getUuid)
                        .findAny()
                        .orElseThrow(() -> new OperationFailureException(operr(
                                "failed to create VM from OVF because the root disk of the VM cannot be found")));
                vmMsg.setImageUuid(rootImageUuid);

                // create OVF VM with data volumes
                if (bundle.isHasDataVolumes()) {
                    List<String> dataImageUuidList = bundle.getParams().stream()
                            .filter(CreateVmFromOvfImageParam::isImageOfDataVolume)
                            .map(CreateVmFromOvfImageParam::getUuid)
                            .collect(Collectors.toList());
                    vmMsg.setDataVolumeTemplateUuids(dataImageUuidList);
                    vmMsg.setDataVolumeFromTemplateSystemTags(new HashMap<>());
                }

                bus.makeTargetServiceIdByResourceUuid(vmMsg, VmInstanceConstant.SERVICE_ID, bundle.getVmInstanceUuid());
                bus.send(vmMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }
                        CreateVmInstanceReply caseReply = reply.castReply();
                        bundle.setVmInventory(caseReply.getInventory());
                        trigger.next();
                    }
                });
            }

            void emitVMCreatingStateEvent() {
                new OvfImageUploadTracker.OvfTrackCanonicalEvent(
                        STAGE_VM_CREATING, bundle.getVmInstanceUuid(), Collections.emptyList()).fire();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-image-on-success";

            @Override
            public boolean skip(Map data) {
                return !bundle.isDeleteImageAfterSuccess();
            }

            @Override
            public void run(FlowTrigger trigger, Map map) {
                List<CreateVmFromOvfImageParam> imageParams = bundle.getParams().stream()
                        .filter(param -> param.getUuid() != null)
                        .filter(param -> param.getType() != ImageUuid)
                        .collect(Collectors.toList());
                deleteImages(imageParams, new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.hasError()) {
                            logger.warn(String.format("failed to delete image but still continue: %s",
                                    multiErr(errorCodeList).getReadableDetails()));
                        }
                        // ignore error code list and continue rollback
                        trigger.next();
                    }
                });
            }
        });

        chain.done(new FlowDoneHandler(bundle.getMessage()) {
            @Override
            public void handle(Map data) {
                completion.success(bundle.getVmInventory());
            }
        }).error(new FlowErrorHandler(bundle.getMessage()) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void deleteImages(List<CreateVmFromOvfImageParam> imageParams, WhileDoneCompletion completion) {
        new While<>(imageParams).each(this::deleteImage).run(completion);
    }

    private void deleteImage(CreateVmFromOvfImageParam param, WhileCompletion completion) {
        String imageUuid = param.getUuid();
        ImageVO image = Q.New(ImageVO.class).eq(ImageVO_.uuid, imageUuid).find();
        if (image == null) {
            completion.done();
            return;
        }

        new While<>(image.getBackupStorageRefs()).each((ref, comp) -> {
            ExpungeImageMsg msg = new ExpungeImageMsg();
            msg.setImageUuid(imageUuid);
            msg.setBackupStorageUuid(ref.getBackupStorageUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, imageUuid);
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.addError(reply.getError());
                        logger.warn(String.format(
                                "failed to delete image[uuid:%s] on backup storage[uuid:%s]: %s",
                                imageUuid, ref.getBackupStorageUuid(), reply.getError()
                        ));
                    }
                    completion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.hasError()) {
                    completion.addError(multiErr(errorCodeList));
                }
                completion.done();
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }
}
