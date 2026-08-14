package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotGroupMsg;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotMsg;
import org.zstack.header.vm.NewVmInstanceMessage2;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2021/3/16.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateRootTemplateFromVolumeSnapshotFlow implements Flow {
    protected static final CLogger logger = Utils.getLogger(CreateRootTemplateFromVolumeSnapshotFlow.class);

    @Autowired
    private CloudBus bus;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);
        String snapshotUuid = getSnapshotUuid(spec.getApiMsg());
        SessionInventory session = ((APIMessage) spec.getApiMsg()).getSession();

        Completion completion = new Completion(trigger) {
            @Override
            public void success() {
                syncVmSystemTagsToImage(spec.getRootVolumeImage().getUuid(),
                        snapshotUuid, trigger);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        };

        if (spec.getRootVolumeImage().isTemporary()) {
            createTemporaryRootTemplate(spec, snapshotUuid, session, completion);
        } else {
            createRootTemplate(spec, snapshotUuid, session, completion);
        }
    }

    private void createRootTemplate(CreateVmFromVolumeResourceSpec spec, String snapshotUuid, SessionInventory session, Completion completion) {
        CreateRootVolumeTemplateFromVolumeSnapshotMsg cmsg = new CreateRootVolumeTemplateFromVolumeSnapshotMsg();
        cmsg.setName("create-from-snap-" + snapshotUuid);
        cmsg.setSnapshotUuid(snapshotUuid);
        cmsg.setSession(session);
        bus.makeLocalServiceId(cmsg, ImageConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    completion.fail(r.getError());
                    return;
                }

                CreateRootVolumeTemplateFromVolumeSnapshotReply reply = r.castReply();
                spec.getRootVolumeImage().setUuid(reply.getInventory().getUuid());
                completion.success();
            }
        });
    }

    private void syncVmSystemTagsToImage(String imageUuid, String snapShotUuid, FlowTrigger trigger) {
        String volUuid = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.volumeUuid)
                .eq(VolumeSnapshotVO_.uuid, snapShotUuid)
                .findValue();
        String vmUuid = Q.New(VolumeVO.class).select(VolumeVO_.vmInstanceUuid).eq(VolumeVO_.uuid, volUuid).findValue();
        if (vmUuid == null) {
            trigger.next();
            return;
        }

        List<SystemTagVO> vos = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, vmUuid).list();
        SyncSystemTagFromTagMsg smsg = new SyncSystemTagFromTagMsg();
        smsg.setImageUuid(imageUuid);
        smsg.setVmSystemTags(vos.stream().map(SystemTagVO::getTag).collect(Collectors.toList()));
        bus.makeLocalServiceId(smsg, ImageConstant.SERVICE_ID);
        bus.send(smsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("sync vm systemTags to image failed, because: %s", reply.getError()));
                }

                trigger.next();
            }
        });
    }

    private void createTemporaryRootTemplate(CreateVmFromVolumeResourceSpec spec, String snapshotUuid, SessionInventory session, Completion completion) {
        CreateTemporaryRootVolumeTemplateFromVolumeSnapshotMsg cmsg = new CreateTemporaryRootVolumeTemplateFromVolumeSnapshotMsg();
        cmsg.setSnapshotUuid(snapshotUuid);
        cmsg.setSession(session);
        bus.makeLocalServiceId(cmsg, ImageConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    completion.fail(r.getError());
                    return;
                }

                CreateTemporaryRootVolumeTemplateFromVolumeSnapshotReply reply = r.castReply();
                spec.getRootVolumeImage().setUuid(reply.getInventory().getUuid());
                spec.setHostUuid(reply.getLocateHostUuid());
                spec.setPrimaryStorageUuidForRootVolume(reply.getLocatePsUuid());
                completion.success();
            }
        });
    }

    private String getSnapshotUuid(NewVmInstanceMessage2 msg) {
        if (msg instanceof APICreateVmInstanceFromVolumeSnapshotMsg) {
            return ((APICreateVmInstanceFromVolumeSnapshotMsg) msg).getVolumeSnapshotUuid();
        } else if (msg instanceof APICreateVmInstanceFromVolumeSnapshotGroupMsg) {
            String groupUuid = ((APICreateVmInstanceFromVolumeSnapshotGroupMsg) msg).getVolumeSnapshotGroupUuid();
            return Q.New(VolumeSnapshotGroupRefVO.class).select(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid)
                    .eq(VolumeSnapshotGroupRefVO_.volumeType, VolumeType.Root.toString())
                    .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, groupUuid)
                    .findValue();
        }

        return null;
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);
        if (spec.getRootVolumeImage().getUuid() == null) {
            trigger.rollback();
            return;
        }

        ImageDeletionMsg msg = new ImageDeletionMsg();
        msg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
        msg.setImageUuid(spec.getRootVolumeImage().getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, msg.getImageUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                trigger.rollback();
            }
        });
    }
}
