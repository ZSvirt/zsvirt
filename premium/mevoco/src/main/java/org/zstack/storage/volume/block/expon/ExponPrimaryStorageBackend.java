package org.zstack.storage.volume.block.expon;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.expon.ExponConstants;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.Message;
import org.zstack.header.storage.addon.primary.BaseVolumeInfo;
import org.zstack.header.storage.addon.primary.CreateVolumeSnapshotSpec;
import org.zstack.header.storage.addon.primary.CreateVolumeSpec;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotStats;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStats;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.block.*;
import org.zstack.storage.addon.primary.BlockExternalPrimaryStorageBackend;
import org.zstack.storage.addon.primary.ExternalPrimaryStorage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class ExponPrimaryStorageBackend extends ExternalPrimaryStorage implements BlockExternalPrimaryStorageBackend {
    @Override
    public String getType() {
        return ExponConstants.EXPON_MANUFACTURER;
    }

    public ExponPrimaryStorageBackend(ExternalPrimaryStorage other) {
        super(other);
    }

    public void handle(GetAccessPathMsg msg) {
        GetAccessPathReply reply = new GetAccessPathReply();
        ExponBlockVolumeVO exponBlockVolumeVO = Q.New(ExponBlockVolumeVO.class).eq(ExponBlockVolumeVO_.uuid, msg.getVolumeUuid()).find();
        if (exponBlockVolumeVO == null) {
            reply.setError(operr("ExponBlockVolume[uuid:%s] not found", msg.getVolumeUuid()));
            bus.reply(msg, reply);
            return;
        }

        BaseVolumeInfo vol = BaseVolumeInfo.valueOf(VolumeInventory.valueOf(exponBlockVolumeVO));
        String path = node.getActivePath(vol, null, vol.isShareable());
        if (!path.startsWith("iscsi")) {
            reply.setError(operr("path error"));
            bus.reply(msg, reply);
            return;
        }
        String[] serverHostNames;
        try {
            URI uri = new URI(path);
            serverHostNames = uri.getHost().split(",");
            Arrays.sort(serverHostNames);
        } catch (URISyntaxException e) {
            throw new OperationFailureException(operr(e.getMessage()));
        }
        AccessPathInfo accessPathInfo = new AccessPathInfo();
        accessPathInfo.setGatewayIps(new ArrayList<>(Arrays.asList(serverHostNames)));
        reply.setInfos(new ArrayList<>(Arrays.asList(accessPathInfo)));
        bus.reply(msg, reply);
    }

    @Override
    public void handle(InstantiateVolumeOnPrimaryStorageMsg msg) {
        VolumeInventory volume = msg.getVolume();
        CreateVolumeSpec spec = new CreateVolumeSpec();
        spec.setUuid(volume.getUuid());
        spec.setSize(volume.getSize());
        spec.setAllocatedUrl(msg.getAllocatedInstallUrl());
        spec.setName(volume.getName());
        createEmptyVolume(msg, spec);
    }

    private void createEmptyVolume(InstantiateVolumeOnPrimaryStorageMsg msg, CreateVolumeSpec spec) {
        VolumeInventory volume = msg.getVolume();
        ExponBlockVolumeVO exponVO = dbf.findByUuid(volume.getUuid(), ExponBlockVolumeVO.class);
        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
        controller.createVolume(spec, new ReturnValueCompletion<VolumeStats>(msg) {
            @Override
            public void success(VolumeStats stats) {
                volume.setActualSize(stats.getActualSize());
                volume.setSize(stats.getSize());
                volume.setFormat(stats.getFormat());
                volume.setInstallPath(stats.getInstallPath());
                exponVO.setExponStatus(stats.getRunStatus());
                dbf.updateAndRefresh(exponVO);
                reply.setVolume(volume);
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
        VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(sp.getVolumeUuid(), VolumeVO.class));
        CreateVolumeSnapshotSpec sspec = new CreateVolumeSnapshotSpec();
        sspec.setVolumeInstallPath(vol.getInstallPath());
        sspec.setName(msg.getName());
        TakeSnapshotReply reply = new TakeSnapshotReply();
        controller.createSnapshot(sspec, new ReturnValueCompletion<VolumeSnapshotStats>(msg) {
            @Override
            public void success(VolumeSnapshotStats stats) {
                sp.setPrimaryStorageInstallPath(stats.getInstallPath());
                sp.setPrimaryStorageUuid(self.getUuid());
                sp.setType(VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString());
                sp.setSize(stats.getActualSize());
                reply.setInventory(sp);
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
    public void handle(DeleteSnapshotOnPrimaryStorageMsg msg) {
        DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        controller.expungeSnapshot(msg.getSnapshot().getPrimaryStorageInstallPath(), new Completion(msg) {
            @Override
            public void success() {
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
    public void handle(DeleteVolumeOnPrimaryStorageMsg msg) {
        DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
        trashVolume(msg.getVolume().getInstallPath(), msg.getVolume().getProtocol(), true, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }
}
