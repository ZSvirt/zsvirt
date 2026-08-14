package org.zstack.storage.primary.local;

import org.zstack.core.db.Q;
import org.zstack.core.gc.EventBasedGarbageCollector;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.primary.PrimaryStorageCanonicalEvent;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.kvm.KvmCommandFailureChecker;
import org.zstack.kvm.KvmCommandSender;
import org.zstack.kvm.KvmResponseWrapper;
import org.zstack.storage.volume.VolumeErrors;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by xing5 on 2017/3/5.
 */
public class LocalStorageDeleteBitsGC extends EventBasedGarbageCollector {
    @GC
    public String primaryStorageUuid;
    @GC
    public String hostUuid;
    @GC
    public String installPath;
    @GC
    public boolean isDir;
    private static CLogger logger = Utils.getLogger(LocalStorageDeleteBitsGC.class);

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(primaryStorageUuid, PrimaryStorageVO.class)) {
            completion.cancel();
            return;
        }

        if (!dbf.isExist(hostUuid, HostVO.class)) {
            completion.cancel();
            return;
        }

        LocalStorageKvmBackend.DeleteBitsCmd cmd = new LocalStorageKvmBackend.DeleteBitsCmd();
        cmd.setPath(installPath);
        cmd.setHostUuid(hostUuid);
        cmd.storagePath = Q.New(PrimaryStorageVO.class).
                eq(PrimaryStorageVO_.uuid, primaryStorageUuid).
                select(PrimaryStorageVO_.url).
                findValue();

        String path = isDir ? LocalStorageKvmBackend.DELETE_DIR_PATH : LocalStorageKvmBackend.DELETE_BITS_PATH;

        new KvmCommandSender(hostUuid).send(cmd, path,
                new KvmCommandFailureChecker() {
                    @Override
                    public ErrorCode getError(KvmResponseWrapper wrapper) {
                        LocalStorageKvmBackend.DeleteBitsRsp rsp = wrapper.getResponse(LocalStorageKvmBackend.DeleteBitsRsp.class);
                        return rsp.buildErrorCode();
                    }
                },

                new ReturnValueCompletion<KvmResponseWrapper>(completion) {
                    @Override
                    public void success(KvmResponseWrapper ret) {
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        if (errorCode.isError(VolumeErrors.VOLUME_IN_USE)) {
                            completion.cancel();
                            return;
                        }
                        completion.fail(errorCode);
                    }
                }
        );
    }

    @Override
    protected void setup() {
        onEvent(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, ((tokens, data) -> {
            HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
            return d.getHostUuid().equals(hostUuid) && d.getNewStatus().equals(HostStatus.Connected.toString());
        }));

        onEvent(HostCanonicalEvents.HOST_DELETED_PATH, ((tokens, data) -> {
            HostCanonicalEvents.HostDeletedData d = (HostCanonicalEvents.HostDeletedData) data;
            return d.getHostUuid().equals(hostUuid);
        }));

        onEvent(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_DELETED_PATH, ((tokens, data) -> {
            PrimaryStorageCanonicalEvent.PrimaryStorageDeletedData d = (PrimaryStorageCanonicalEvent.PrimaryStorageDeletedData) data;
            return d.getPrimaryStorageUuid().equals(primaryStorageUuid);
        }));
    }
}
