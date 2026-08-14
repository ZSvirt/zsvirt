package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.*;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeMsg;
import org.zstack.tag.TagManager;

import java.util.List;
import java.util.Map;

/**
 * Created by MaJin on 2021/3/15.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateTemplateFromVolumeFlow implements Flow {
    @Autowired
    private CloudBus bus;
    @Autowired
    private TagManager tagMgr;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);
        APICreateVmInstanceFromVolumeMsg msg = (APICreateVmInstanceFromVolumeMsg) spec.getApiMsg();

        Completion completion = new Completion(trigger) {
            @Override
            public void success() {
                createImageTags(spec.getRootVolumeImage().getUuid(), msg);
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        };

        if (spec.getRootVolumeImage().isTemporary()) {
            createTemporaryImage(spec, msg, completion);
        } else {
            createImage(spec, msg, completion);
        }
    }

    private void createImage(CreateVmFromVolumeResourceSpec spec, APICreateVmInstanceFromVolumeMsg msg, Completion completion) {
        CreateDataVolumeTemplateFromVolumeMsg cmsg = new CreateDataVolumeTemplateFromVolumeMsg();
        cmsg.setVolumeUuid(msg.getVolumeUuid());
        cmsg.setName("root-image-for-vm-" + msg.getName());
        cmsg.setSession(msg.getSession());
        bus.makeLocalServiceId(cmsg, ImageConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                CreateDataVolumeTemplateFromVolumeReply r = reply.castReply();
                spec.getRootVolumeImage().setUuid(r.getInventory().getUuid());

                UpdateImageMsg umsg = new UpdateImageMsg();
                umsg.setUuid(r.getInventory().getUuid());
                umsg.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString());
                umsg.setPlatform(msg.getPlatform() == null ? ImagePlatform.Linux.toString() : msg.getPlatform());
                bus.makeTargetServiceIdByResourceUuid(umsg, ImageConstant.SERVICE_ID, umsg.getImageUuid());
                bus.send(umsg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        completion.success();
                    }
                });
            }
        });
    }

    private void createTemporaryImage(CreateVmFromVolumeResourceSpec spec, APICreateVmInstanceFromVolumeMsg msg, Completion completion) {
        CreateTemporaryRootVolumeTemplateFromVolumeMsg cmsg = new CreateTemporaryRootVolumeTemplateFromVolumeMsg();
        cmsg.setVolumeUuid(msg.getVolumeUuid());
        cmsg.setSession(msg.getSession());
        cmsg.setPlatform(msg.getPlatform());
        String specifiedArch = null;
        if (msg.getHostUuid() != null) {
            specifiedArch = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid())
                    .select(HostVO_.architecture)
                    .findValue();
        } else if (msg.getClusterUuid() != null) {
            specifiedArch = Q.New(ClusterVO.class).eq(ClusterVO_.uuid, msg.getClusterUuid())
                    .select(ClusterVO_.architecture)
                    .findValue();
        }
        cmsg.setArchitecture(specifiedArch);
        bus.makeLocalServiceId(cmsg, ImageConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                CreateTemporaryRootVolumeTemplateFromVolumeReply r = reply.castReply();
                spec.getRootVolumeImage().setUuid(r.getInventory().getUuid());
                spec.setHostUuid(r.getLocateHostUuid());
                spec.setPrimaryStorageUuidForRootVolume(r.getLocatePsUuid());
                completion.success();
            }
        });
    }

    private void createImageTags(String uuid, APICreateMessage msg) {
        if (msg.getSystemTags() != null) {
            List<String> imageTags = tagMgr.filterSystemTags(msg.getSystemTags(), ImageVO.class.getSimpleName());
            msg.getSystemTags().removeAll(imageTags);
            tagMgr.createTags(imageTags, null, uuid, ImageVO.class.getSimpleName());
        }
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);
        String imageUuid = spec.getRootVolumeImage().getUuid();
        if (imageUuid == null) {
            trigger.rollback();
            return;
        }

        ImageDeletionMsg dmsg = new ImageDeletionMsg();
        dmsg.setImageUuid(imageUuid);
        dmsg.setForceDelete(true);
        dmsg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
        bus.makeTargetServiceIdByResourceUuid(dmsg, ImageConstant.SERVICE_ID, dmsg.getImageUuid());
        bus.send(dmsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                trigger.rollback();
            }
        });
    }
}
