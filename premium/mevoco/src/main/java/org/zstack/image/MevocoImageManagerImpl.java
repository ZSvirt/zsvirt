package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.CommitVolumeAsImageMsg;
import org.zstack.header.storage.primary.CommitVolumeAsImageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.snapshot.CommitVolumeSnapshotAsImageMsg;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.storage.snapshot.VolumeTemplateOverlayMsg;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.Arrays;
import java.util.List;

public class MevocoImageManagerImpl implements ImageExtensionManager {
    private static final CLogger logger = Utils.getLogger(MevocoImageManagerImpl.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private TagManager tagMgr;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateRootVolumeTemplateFromRootVolumeMsg) {
            handle((APICreateRootVolumeTemplateFromRootVolumeMsg) msg);
        } else if (msg instanceof APICreateDataVolumeTemplateFromVolumeMsg) {
            handle((APICreateDataVolumeTemplateFromVolumeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateTemplateFromSnapshotMessage) {
            ImageMessageFiller.fillFromSnapshot((AddImageMessage) msg, ((CreateTemplateFromSnapshotMessage) msg).getSnapshotUuid());
        }

        if (msg instanceof CreateRootVolumeTemplateFromRootVolumeMsg) {
            handle((CreateRootVolumeTemplateFromRootVolumeMsg) msg);
        } else if (msg instanceof CreateDataVolumeTemplateFromVolumeMsg) {
            handle((CreateDataVolumeTemplateFromVolumeMsg) msg);
        } else if (msg instanceof CreateRootVolumeTemplateFromVolumeSnapshotMsg) {
            handle((CreateRootVolumeTemplateFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof CreateDataVolumeTemplateFromVolumeSnapshotMsg) {
            handle((CreateDataVolumeTemplateFromVolumeSnapshotMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APICreateDataVolumeTemplateFromVolumeMsg msg) {
        if (!handleByUs(msg.getVolumeUuid())) {
            throw new StopRoutingException();
        }

        CommitVolumeAsImageMsg cmsg = new CommitVolumeAsImageMsg();
        cloneMsg(msg, cmsg);

        cmsg.setVolumeUuid(msg.getVolumeUuid());
        String psUuid = Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                .select(VolumeVO_.primaryStorageUuid)
                .findValue();
        cmsg.setPrimaryStorageUuid(psUuid);

        createTemplateFromVolume(cmsg, new ReturnValueCompletion<ImageInventory>(msg) {
            APICreateDataVolumeTemplateFromVolumeEvent evt = new APICreateDataVolumeTemplateFromVolumeEvent(msg.getId());

            @Override
            public void success(ImageInventory inv) {
                tagMgr.createTagsFromAPICreateMessage(msg, inv.getUuid(), ImageVO.class.getSimpleName());
                evt.setInventory(inv);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(final APICreateRootVolumeTemplateFromRootVolumeMsg msg) {
        if (!handleByUs(msg.getRootVolumeUuid())) {
            throw new StopRoutingException();
        }

        CommitVolumeAsImageMsg cmsg = new CommitVolumeAsImageMsg();
        cloneMsg(msg, cmsg);

        cmsg.setVolumeUuid(msg.getRootVolumeUuid());
        cmsg.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString());
        VolumeVO vo = dbf.findByUuid(msg.getRootVolumeUuid(), VolumeVO.class);
        cmsg.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        cmsg.setArchitecture(dbf.findByUuid(vo.getVmInstanceUuid(), VmInstanceVO.class).getArchitecture());
        cmsg.setVirtio(VmSystemTags.VIRTIO.hasTag(vo.getVmInstanceUuid()));

        createTemplateFromVolume(cmsg, new ReturnValueCompletion<ImageInventory>(msg) {
            APICreateRootVolumeTemplateFromRootVolumeEvent evt = new APICreateRootVolumeTemplateFromRootVolumeEvent(msg.getId());

            @Override
            public void success(ImageInventory inv) {
                tagMgr.createTagsFromAPICreateMessage(msg, inv.getUuid(), ImageVO.class.getSimpleName());
                evt.setInventory(inv);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void createTemplateFromVolume(CommitVolumeAsImageMsg msg, ReturnValueCompletion<ImageInventory> completion) {
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());

        VolumeTemplateOverlayMsg overlayMsg = new VolumeTemplateOverlayMsg();
        overlayMsg.setVolumeUuid(msg.getVolumeUuid());
        overlayMsg.setMessage(msg);
        bus.makeTargetServiceIdByResourceUuid(overlayMsg, VolumeConstant.SERVICE_ID, overlayMsg.getVolumeUuid());
        bus.send(overlayMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    CommitVolumeAsImageReply r = reply.castReply();
                    completion.success(r.getInventory());
                }
            }
        });
    }

    private void handle(CreateRootVolumeTemplateFromRootVolumeMsg msg) {
        if (!handleByUs(msg.getRootVolumeUuid())) {
            throw new StopRoutingException();
        }

        CommitVolumeAsImageMsg cmsg = new CommitVolumeAsImageMsg();
        cloneMsg(msg, cmsg);

        cmsg.setVolumeUuid(msg.getRootVolumeUuid());
        VolumeVO vo = dbf.findByUuid(msg.getRootVolumeUuid(), VolumeVO.class);
        cmsg.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        cmsg.setArchitecture(dbf.findByUuid(vo.getVmInstanceUuid(), VmInstanceVO.class).getArchitecture());
        cmsg.setVirtio(VmSystemTags.VIRTIO.hasTag(vo.getVmInstanceUuid()));
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, cmsg.getPrimaryStorageUuid());

        createTemplateFromVolume(cmsg, new ReturnValueCompletion<ImageInventory>(msg) {
            final CreateRootVolumeTemplateFromRootVolumeReply creply = new CreateRootVolumeTemplateFromRootVolumeReply();

            @Override
            public void success(ImageInventory inv) {
                creply.setInventory(inv);
                if (!CollectionUtils.isEmpty(msg.getSystemTags())) {
                    tagMgr.createNonInherentSystemTags(msg.getSystemTags(), creply.getInventory().getUuid(), ImageVO.class.getSimpleName());
                }

                bus.reply(msg, creply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                creply.setError(errorCode);
                bus.reply(msg, creply);
            }
        });
    }

    private void handle(CreateDataVolumeTemplateFromVolumeMsg msg) {
        if (!handleByUs(msg.getVolumeUuid())) {
            throw new StopRoutingException();
        }

        CommitVolumeAsImageMsg cmsg = new CommitVolumeAsImageMsg();
        cloneMsg(msg, cmsg);
        cmsg.setVolumeUuid(msg.getVolumeUuid());
        String psUuid = Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                .select(VolumeVO_.primaryStorageUuid)
                .findValue();
        cmsg.setPrimaryStorageUuid(psUuid);
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, cmsg.getPrimaryStorageUuid());
        createTemplateFromVolume(cmsg, new ReturnValueCompletion<ImageInventory>(msg) {
            final CreateDataVolumeTemplateFromVolumeReply creply = new CreateDataVolumeTemplateFromVolumeReply();

            @Override
            public void success(ImageInventory inv) {
                if (!CollectionUtils.isEmpty(msg.getSystemTags())) {
                    tagMgr.createNonInherentSystemTags(msg.getSystemTags(), creply.getInventory().getUuid(), ImageVO.class.getSimpleName());
                }

                creply.setInventory(inv);
                bus.reply(msg, creply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                creply.setError(errorCode);
                bus.reply(msg, creply);
            }
        });
    }

    private void handle(CreateDataVolumeTemplateFromVolumeSnapshotMsg msg) {
        Tuple t = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, msg.getSnapshotUuid())
                .select(VolumeSnapshotVO_.volumeUuid, VolumeVO_.primaryStorageUuid)
                .findTuple();

        if (!handleByUs(t.get(0, String.class))) {
            throw new StopRoutingException();
        }

        CommitVolumeSnapshotAsImageMsg cmsg = new CommitVolumeSnapshotAsImageMsg();
        cloneMsg(msg, cmsg);
        cmsg.setVolumeSnapshotUuid(msg.getSnapshotUuid());
        cmsg.setVolumeUuid(t.get(0, String.class));
        cmsg.setPrimaryStorageUuid(t.get(1, String.class));

        createTemplateFromVolume(cmsg, new ReturnValueCompletion<ImageInventory>(msg) {
            final CreateDataVolumeTemplateFromVolumeSnapshotReply creply = new CreateDataVolumeTemplateFromVolumeSnapshotReply();

            @Override
            public void success(ImageInventory inv) {
                if (!CollectionUtils.isEmpty(msg.getSystemTags())) {
                    tagMgr.createNonInherentSystemTags(msg.getSystemTags(), inv.getUuid(), ImageVO.class.getSimpleName());
                }

                creply.setInventory(inv);
                bus.reply(msg, creply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                creply.setError(errorCode);
                bus.reply(msg, creply);
            }
        });
    }

    private void handle(CreateRootVolumeTemplateFromVolumeSnapshotMsg msg) {
        Tuple t = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, msg.getSnapshotUuid())
                .select(VolumeSnapshotVO_.volumeUuid, VolumeVO_.primaryStorageUuid)
                .findTuple();

        if (!handleByUs(t.get(0, String.class))) {
            throw new StopRoutingException();
        }

        CommitVolumeSnapshotAsImageMsg cmsg = new CommitVolumeSnapshotAsImageMsg();
        cloneMsg(msg, cmsg);
        cmsg.setVolumeSnapshotUuid(msg.getSnapshotUuid());
        cmsg.setVolumeUuid(t.get(0, String.class));
        cmsg.setPrimaryStorageUuid(t.get(1, String.class));

        createTemplateFromVolume(cmsg, new ReturnValueCompletion<ImageInventory>(msg) {
            final CreateRootVolumeTemplateFromVolumeSnapshotReply creply = new CreateRootVolumeTemplateFromVolumeSnapshotReply();

            @Override
            public void success(ImageInventory inv) {
                if (!CollectionUtils.isEmpty(msg.getSystemTags())) {
                    tagMgr.createNonInherentSystemTags(msg.getSystemTags(), inv.getUuid(), ImageVO.class.getSimpleName());
                }

                creply.setInventory(inv);
                bus.reply(msg, creply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                creply.setError(errorCode);
                bus.reply(msg, creply);
            }
        });
    }

    private void cloneMsg(AddImageMessage smsg, CommitVolumeAsImageMsg dmsg) {
        dmsg.setName(smsg.getName());
        dmsg.setDescription(smsg.getDescription());
        dmsg.setGuestOsType(smsg.getGuestOsType());
        dmsg.setPlatform(smsg.getPlatform());
        dmsg.setArchitecture(smsg.getArchitecture());
        dmsg.setResourceUuid(smsg.getResourceUuid());
        dmsg.setBackupStorageUuids(smsg.getBackupStorageUuids());
        dmsg.setMediaType(smsg.getMediaType());
        dmsg.setSession(smsg.getSession());
        dmsg.setSystemTags(smsg.getSystemTags());
        dmsg.setVirtio(smsg.isVirtio());
    }

    private boolean handleByUs(String volumeUuid) {
        String psType = SQL.New("select ps.type from PrimaryStorageVO ps, VolumeVO vol where vol.primaryStorageUuid = ps.uuid and vol.uuid = :volUuid")
                .param("volUuid", volumeUuid).find();
        if (PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE.equals(psType)) {
            return false;
        }

        return Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).notNull(VolumeVO_.vmInstanceUuid).isExists();
    }

    @Override
    public List<Class> getMessageClasses() {
        return Arrays.asList(CreateRootVolumeTemplateFromRootVolumeMsg.class,
                CreateDataVolumeTemplateFromVolumeMsg.class,
                CreateRootVolumeTemplateFromVolumeSnapshotMsg.class,
                CreateDataVolumeTemplateFromVolumeSnapshotMsg.class,
                APICreateRootVolumeTemplateFromRootVolumeMsg.class,
                APICreateDataVolumeTemplateFromVolumeMsg.class);
    }
}
