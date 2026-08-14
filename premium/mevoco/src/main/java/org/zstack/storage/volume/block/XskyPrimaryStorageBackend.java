package org.zstack.storage.volume.block;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.core.trash.StorageTrash;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.header.volume.block.*;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephGlobalConfig;
import org.zstack.storage.ceph.CephSystemTags;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author shenjin
 * @date 2023/6/15 13:11
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class XskyPrimaryStorageBackend extends CephPrimaryStorageBase implements BlockPrimaryStorageBackend {
    private static final CLogger logger = Utils.getLogger(XskyPrimaryStorageBackend.class);
    @Autowired
    private StorageTrash trash;

    public static final String RESIZE_BLOCK_VOLUME = "/xsky/ceph/primarystorage/volume/resize";
    public static final String UPDATE_BLOCK_VOLUME = "/xsky/ceph/primarystorage/volume/update";
    public static final String GET_BLOCK_VOLUME_ACCESS_PATH = "/xsky/ceph/primarystorage/volume/access/path";
    public static final String UPDATE_BLOCK_VOLUME_SNAPSHOT = "/xsky/ceph/primarystorage/volume/snapshot/update";

    public XskyPrimaryStorageBackend(PrimaryStorageVO vo) {
        super(vo);
    }

    @Override
    public String getType() {
        return CephConstants.CEPH_MANUFACTURER_XSKY;
    }

    @Override
    protected <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback) {
        httpCall(path, cmd, retClass, callback, null, 0);
    }

    protected <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback, TimeUnit unit, long timeout) {
        setTokenAndTime(cmd);
        new HttpCaller<>(path, cmd, retClass, callback, unit, timeout).call();
    }

    private void setTokenAndTime(AgentCommand cmd) {
        if (CephSystemTags.THIRDPARTY_PLATFORM.hasTag(self.getUuid())) {
            cmd.setToken(CephSystemTags.THIRDPARTY_PLATFORM.getTokenByResourceUuid(self.getUuid(),
                    CephSystemTags.THIRDPARTY_PLATFORM_TOKEN));
            cmd.setTpTimeout(CephGlobalConfig.THIRD_PARTY_SDK_TIMEOUT.value(String.class));
        }
    }

    @Override
    public void handle(InstantiateVolumeOnPrimaryStorageMsg msg) {
        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();

        XskyBlockVolumeVO xskyBlockVolumeVO = Q.New(XskyBlockVolumeVO.class)
                .eq(XskyBlockVolumeVO_.uuid, msg.getVolume().getUuid())
                .find();

        VolumeInventory volume = msg.getVolume();
        final String finalPoolName = getTargetPoolNameFromAllocatedUrl(msg.getAllocatedInstallUrl());

        final XskyPrimaryStorageBackend.CreateBlockVolumeCmd cmd = new XskyPrimaryStorageBackend.CreateBlockVolumeCmd();
        cmd.setInstallPath(makeVolumeInstallPathByTargetPool(volume.getName(), finalPoolName));
        cmd.setName(volume.getName());
        cmd.setDescription(volume.getDescription());
        cmd.setSize(volume.getSize());
        cmd.setBurstTotalBw(xskyBlockVolumeVO.getBurstTotalBw());
        cmd.burstTotalIops = xskyBlockVolumeVO.getBurstTotalIops();
        cmd.maxTotalBw = xskyBlockVolumeVO.getMaxTotalBw();
        cmd.maxTotalIops = xskyBlockVolumeVO.getMaxTotalIops();
        httpCall(String.format("/%s%s", CephConstants.CEPH_MANUFACTURER_XSKY, CREATE_VOLUME_PATH), cmd, XskyPrimaryStorageBackend.CreateBlockVolumeRsp.class, new ReturnValueCompletion<CreateBlockVolumeRsp>(msg) {
            @Override
            public void success(XskyPrimaryStorageBackend.CreateBlockVolumeRsp rsp) {
                xskyBlockVolumeVO.setActualSize(rsp.actualSize);
                xskyBlockVolumeVO.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                xskyBlockVolumeVO.setInstallPath(buildEmptyVolumeInstallPath(finalPoolName, cmd.installPath, rsp.getInstallPath()));
                xskyBlockVolumeVO.setXskyBlockVolumeId(rsp.xskyBlockVolumeId);
                xskyBlockVolumeVO.setXskyStatus(rsp.xskyStatus);
                dbf.updateAndRefresh(xskyBlockVolumeVO);

                reply.setVolume(XskyBlockVolumeInventory.valueOf(xskyBlockVolumeVO));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode error) {
                reply.setError(error);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public void handle(DeleteVolumeOnPrimaryStorageMsg msg) {
        PrimaryStorageVO primaryStorageVO = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, msg.getVolume().getPrimaryStorageUuid()).find();
        if (primaryStorageVO == null) {
            String err = String.format("Cannot find PrimaryStorage[uuid:%s], it may have been deleted", msg.getPrimaryStorageUuid());
            bus.replyErrorByMessageType(msg, err);
            return;
        }

        XskyPrimaryStorageBackend.DeleteBlockVolumeCmd cmd = new XskyPrimaryStorageBackend.DeleteBlockVolumeCmd();
        BlockVolumeVO vo = Q.New(BlockVolumeVO.class).eq(BlockVolumeVO_.uuid, msg.getVolume().getUuid()).find();
        //xsky block volume id
        Integer blockVolumeId = Q.New(XskyBlockVolumeVO.class).select(XskyBlockVolumeVO_.xskyBlockVolumeId)
                .eq(XskyBlockVolumeVO_.uuid, msg.getVolume().getUuid()).findValue();
        cmd.setXskyBlockVolumeId(blockVolumeId);
        cmd.setInstallPath(msg.getVolume().getInstallPath());

        DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();

        String path = String.format("/%s%s", CephConstants.CEPH_MANUFACTURER_XSKY, DELETE_PATH);
        httpCall(path, cmd, CephPrimaryStorageBase.AgentResponse.class, new ReturnValueCompletion<CephPrimaryStorageBase.AgentResponse>(msg) {
            @Override
            public void success(CephPrimaryStorageBase.AgentResponse ret) {
                dbf.remove(vo);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public void handle(TakeSnapshotMsg msg) {
        inQueue().name(String.format("take-block-volume-snapshot-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> takeSnapshot(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    protected void takeSnapshot(final TakeSnapshotMsg msg, final NoErrorCompletion completion) {
        final TakeSnapshotReply reply = new TakeSnapshotReply();

        final VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        String volumePath = Q.New(XskyBlockVolumeVO.class)
                .select(XskyBlockVolumeVO_.installPath)
                .eq(XskyBlockVolumeVO_.uuid, sp.getVolumeUuid())
                .findValue();

        final String spPath = String.format("%s@%s", volumePath, sp.getUuid());
        CreateSnapshotCmd cmd = new CreateSnapshotCmd();
        cmd.setVolumeUuid(sp.getVolumeUuid());
        cmd.setSnapshotPath(spPath);
        cmd.setName(msg.getName());
        cmd.setDescription(msg.getName());
        httpCall(CREATE_SNAPSHOT_PATH, cmd, CreateSnapshotRsp.class, new ReturnValueCompletion<CreateSnapshotRsp>(msg) {
            @Override
            public void success(CreateSnapshotRsp rsp) {
                // current ceph has no way to get actual size
                long asize = rsp.getActualSize() == null ? 0 : rsp.getActualSize();
                sp.setSize(asize);
                sp.setPrimaryStorageUuid(self.getUuid());
                sp.setPrimaryStorageInstallPath(rsp.getInstallPath());
                sp.setType(VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString());
                sp.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                reply.setInventory(sp);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    @Override
    public void handle(DeleteSnapshotOnPrimaryStorageMsg msg) {
        inQueue().name(String.format("delete-block-volume-snapshot-on-primarystorage-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> deleteSnapshotOnPrimaryStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    protected void deleteSnapshotOnPrimaryStorage(DeleteSnapshotOnPrimaryStorageMsg msg, NoErrorCompletion completion) {
        final DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        DeleteSnapshotCmd cmd = new DeleteSnapshotCmd();
        cmd.setSnapshotPath(msg.getSnapshot().getPrimaryStorageInstallPath());
        httpCall(DELETE_SNAPSHOT_PATH, cmd, DeleteSnapshotRsp.class, new ReturnValueCompletion<DeleteSnapshotRsp>(msg) {
            @Override
            public void success(DeleteSnapshotRsp returnValue) {
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        }, TimeUnit.MILLISECONDS, msg.getTimeout());
    }

    @Override
    public void handle(final RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        final RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("revert-xsky-block-volume-[uuid:%s]-from-snapshot-[uuid:%s]-on-ceph-primary-storage",
                msg.getVolume().getUuid(), msg.getSnapshot().getUuid()));
        chain.enableProgressReport();
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                // get volume path from snapshot path, just split @
                String volumePath = msg.getSnapshot().getPrimaryStorageInstallPath().split("@")[0];

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        RollbackSnapshotCmd cmd = new RollbackSnapshotCmd();
                        cmd.setSnapshotPath(msg.getSnapshot().getPrimaryStorageInstallPath());
                        httpCall(ROLLBACK_SNAPSHOT_PATH, cmd, RollbackSnapshotRsp.class, new ReturnValueCompletion<RollbackSnapshotRsp>(msg) {
                            @Override
                            public void success(RollbackSnapshotRsp returnValue) {
                                reply.setSize(returnValue.getSize());
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        }, TimeUnit.MILLISECONDS, msg.getTimeout());
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        Long trashId = trash.getTrashId(self.getUuid(), volumePath);
                        if (trashId != null) {
                            trash.removeFromDb(trashId);
                        }

                        reply.setNewVolumeInstallPath(volumePath);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    public void handle(ResizeVolumeOnPrimaryStorageMsg msg) {
        final ResizeVolumeOnPrimaryStorageReply reply = new ResizeVolumeOnPrimaryStorageReply();
        final VolumeInventory volume = msg.getVolume();
        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.getUuid()).find();
        Integer id = Q.New(XskyBlockVolumeVO.class).select(XskyBlockVolumeVO_.xskyBlockVolumeId)
                .eq(XskyBlockVolumeVO_.uuid, volume.getUuid()).findValue();
        XskyPrimaryStorageBackend.ResizeBlockVolumeCmd cmd = new XskyPrimaryStorageBackend.ResizeBlockVolumeCmd();
        cmd.setXskyBlockVolumeId(id);
        cmd.setSize(msg.getSize());

        PrimaryStorageVO primaryStorageVO = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid()).find();
        if (primaryStorageVO == null) {
            String err = String.format("Cannot find PrimaryStorage[uuid:%s], it may have been deleted", msg.getPrimaryStorageUuid());
            bus.replyErrorByMessageType(msg, err);
            return;
        }

        httpCall(RESIZE_BLOCK_VOLUME, cmd, XskyPrimaryStorageBackend.ResizeBlockVolumeRsp.class, new ReturnValueCompletion<XskyPrimaryStorageBackend.ResizeBlockVolumeRsp>(msg) {
            @Override
            public void success(XskyPrimaryStorageBackend.ResizeBlockVolumeRsp returnValue) {
                logger.debug(String.format("successfully resize the volume[uuid:%s] to %d", volume.getUuid(), returnValue.getSize()));
                volumeVO.setSize(returnValue.getSize());
                dbf.updateAndRefresh(volumeVO);
                reply.setVolume(VolumeInventory.valueOf(volumeVO));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("fail to resize volume[uuid:%s] to %d", volume.getUuid(), msg.getSize()));
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public void handle(GetAccessPathMsg msg) {
        GetAccessPathReply reply = new GetAccessPathReply();
        GetAccessPathCmd cmd = new GetAccessPathCmd();
        if (!StringUtils.isEmpty(msg.getVolumeUuid())) {
            String iscsiPath = Q.New(BlockVolumeVO.class).select(BlockVolumeVO_.iscsiPath).eq(BlockVolumeVO_.uuid, msg.getVolumeUuid()).findValue();
            cmd.setIscsiPath(iscsiPath);
        }
        httpCall(GET_BLOCK_VOLUME_ACCESS_PATH, cmd, XskyPrimaryStorageBackend.GetAccessPathRsp.class, new ReturnValueCompletion<XskyPrimaryStorageBackend.GetAccessPathRsp>(msg) {
            @Override
            public void success(XskyPrimaryStorageBackend.GetAccessPathRsp ret) {
                reply.setInfos(ret.getInfos());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode error) {
                reply.setError(error);
                bus.reply(msg, reply);
            }
        });
    }

    public static class CreateBlockVolumeCmd extends AgentCommand {
        String name;
        String description;
        Long size;
        String installPath; // 包含poolUuid
        Long burstTotalBw;
        Long burstTotalIops;
        Long maxTotalBw;
        Long maxTotalIops;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public void setBurstTotalBw(Long burstTotalBw) {
            this.burstTotalBw = burstTotalBw;
        }

        public void setBurstTotalIops(Long burstTotalIops) {
            this.burstTotalIops = burstTotalIops;
        }

        public void setMaxTotalBw(Long maxTotalBw) {
            this.maxTotalBw = maxTotalBw;
        }

        public void setMaxTotalIops(Long maxTotalIops) {
            this.maxTotalIops = maxTotalIops;
        }
    }
    
    public static class CreateBlockVolumeRsp extends AgentResponse {
        String xskyStatus;
        Integer xskyBlockVolumeId;
        Long actualSize;
        String installPath;

        public String getXskyStatus() {
            return xskyStatus;
        }

        public void setXskyStatus(String xskyStatus) {
            this.xskyStatus = xskyStatus;
        }

        public Integer getXskyBlockVolumeId() {
            return xskyBlockVolumeId;
        }

        public void setXskyBlockVolumeId(Integer xskyBlockVolumeId) {
            this.xskyBlockVolumeId = xskyBlockVolumeId;
        }

        public Long getActualSize() {
            return actualSize;
        }

        public void setActualSize(Long actualSize) {
            this.actualSize = actualSize;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }
    
    public static class DeleteBlockVolumeCmd extends AgentCommand {
        Integer xskyBlockVolumeId;
        String installPath;

        public Integer getXskyBlockVolumeId() {
            return xskyBlockVolumeId;
        }

        public void setXskyBlockVolumeId(Integer xskyBlockVolumeId) {
            this.xskyBlockVolumeId = xskyBlockVolumeId;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class GetAccessPathCmd extends AgentCommand {
        private String iscsiPath;

        public String getIscsiPath() {
            return iscsiPath;
        }

        public void setIscsiPath(String iscsiPath) {
            this.iscsiPath = iscsiPath;
        }
    }

    public static class GetAccessPathRsp extends AgentResponse {
        List<AccessPathInfo> infos;

        public List<AccessPathInfo> getInfos() {
            return infos;
        }

        public void setInfos(List<AccessPathInfo> infos) {
            this.infos = infos;
        }
    }
    
    public static class UpdateBlockVolumeCmd extends AgentCommand {
        Integer xskyBlockVolumeId;
        String name;
        String description;
        Long burstTotalBw;
        Long burstTotalIops;
        Long maxTotalBw;
        Long maxTotalIops;

        public Integer getXskyBlockVolumeId() {
            return xskyBlockVolumeId;
        }

        public void setXskyBlockVolumeId(Integer xskyBlockVolumeId) {
            this.xskyBlockVolumeId = xskyBlockVolumeId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Long getBurstTotalBw() {
            return burstTotalBw;
        }

        public void setBurstTotalBw(Long burstTotalBw) {
            this.burstTotalBw = burstTotalBw;
        }

        public Long getBurstTotalIops() {
            return burstTotalIops;
        }

        public void setBurstTotalIops(Long burstTotalIops) {
            this.burstTotalIops = burstTotalIops;
        }

        public Long getMaxTotalBw() {
            return maxTotalBw;
        }

        public void setMaxTotalBw(Long maxTotalBw) {
            this.maxTotalBw = maxTotalBw;
        }

        public Long getMaxTotalIops() {
            return maxTotalIops;
        }

        public void setMaxTotalIops(Long maxTotalIops) {
            this.maxTotalIops = maxTotalIops;
        }
    }

    public static class ResizeBlockVolumeCmd extends AgentCommand {
        Integer xskyBlockVolumeId;
        long size;

        public Integer getXskyBlockVolumeId() {
            return xskyBlockVolumeId;
        }

        public void setXskyBlockVolumeId(Integer xskyBlockVolumeId) {
            this.xskyBlockVolumeId = xskyBlockVolumeId;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }
    
    public static class ResizeBlockVolumeRsp extends AgentResponse {
        long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }
}
