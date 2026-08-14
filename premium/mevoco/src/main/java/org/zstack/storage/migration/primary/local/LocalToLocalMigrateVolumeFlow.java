package org.zstack.storage.migration.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.longjob.LongJobUtils;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.storage.primary.local.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.File;
import java.util.Map;

// TODO(clone) : when performing cold migration of entire vm instances on local storage, the chain clone dependencies must be carefully considered.
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalToLocalMigrateVolumeFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(LocalToLocalMigrateVolumeFlow.class);

    @Autowired
    protected CloudBus bus;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        String volumeUuid = (String) data.get(StorageMigrationConstant.VOLUME_UUID);
        String srcPsUuid = (String) data.get(StorageMigrationConstant.SRC_PS_UUID);
        String dstPsUuid = (String) data.get(StorageMigrationConstant.DST_PS_UUID);

        LocalStorageUtils.InstallPath path = new LocalStorageUtils.InstallPath();
        path.fullPath = (String) data.get(StorageMigrationConstant.ALLOCATED_INSTALL_URL);
        String dstHostUuid = path.disassemble().hostUuid;

        String srcVolumeInstallPath = Q.New(VolumeVO.class).select(VolumeVO_.installPath).eq(VolumeVO_.uuid, volumeUuid).findValue();
        String srcVolumeFolderPath = getVolumeFolderPath(srcVolumeInstallPath);

        String srcHostUuid = Q.New(LocalStorageResourceRefVO.class)
                .eq(LocalStorageResourceRefVO_.resourceUuid, volumeUuid)
                .eq(LocalStorageResourceRefVO_.primaryStorageUuid, srcPsUuid)
                .select(VmInstanceVO_.hostUuid).findValue();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("migrate-volume-%s-from-ps-%s-host-%s-to-host-%s", volumeUuid, srcPsUuid, srcHostUuid, dstHostUuid));
        chain.enableProgressReport();
        chain.preCheck(d -> LongJobUtils.buildErrIfCanceled());
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-if-install-path-in-trash";

            @Override
            public void run(FlowTrigger cTrigger, Map data) {
                CheckInstallPathInTrashMsg cmsg = new CheckInstallPathInTrashMsg();
                cmsg.setPrimaryStorageUuid(srcPsUuid);
                cmsg.setInstallPath(srcVolumeFolderPath);
                bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, srcPsUuid);
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply r) {
                        if (!r.isSuccess()) {
                            cTrigger.fail(r.getError());
                            return;
                        }

                        CheckInstallPathInTrashReply re = r.castReply();
                        if (re.getTrashId() == null) {
                            cTrigger.next();
                            return;
                        }

                        cTrigger.fail(Platform.operr("found trashId(%s) in primaryStorage [%s] for the migrate installPath[%s]. " +
                                        "please clean it first by 'APICleanUpTrashOnPrimaryStorageMsg' if you insist to migrate the volume[%s]",
                                re.getTrashId(), srcPsUuid, srcVolumeFolderPath, re.getResourceUuid()));
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "migrate-volume-data-from-local-to-local";

            @Override
            public void run(FlowTrigger cTrigger, Map data) {
                MigrateVolumeOnLocalStorageMsg mmsg = new MigrateVolumeOnLocalStorageMsg();
                mmsg.setVolumeUuid(volumeUuid);
                mmsg.setPrimaryStorageUuid(srcPsUuid);
                mmsg.setDestHostUuid(dstHostUuid);
                mmsg.setKeepOriginalVolumeInTrash(true);
                mmsg.setAllocatedPrimaryStorageCapacity(true);
                bus.makeTargetServiceIdByResourceUuid(mmsg, PrimaryStorageConstant.SERVICE_ID, srcPsUuid);
                bus.send(mmsg, new CloudBusCallBack(cTrigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.error(String.format("Failed to migrate Volume %s from PS[%s] host[%s] to host[%s].",
                                    volumeUuid, srcPsUuid, srcHostUuid, dstHostUuid));
                            cTrigger.fail(reply.getError());
                            return;
                        }

                        logger.info(String.format("Migrated Volume %s from PS[%s] host[%s] to host[%s].",
                                volumeUuid, srcPsUuid, srcHostUuid, dstHostUuid));
                        cTrigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(trigger) {
            @Override
            public void handle(Map data) {
                trigger.next();
            }
        }).error(new FlowErrorHandler(trigger) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                trigger.fail(errCode);
            }
        }).start();
    }

    static String getVolumeFolderPath(String srcInstallPath) {
        File vol = new File(srcInstallPath);
        if (vol.getParentFile().getName().equals("snapshots")) {
            return vol.getParentFile().getParent();
        } else {
            return vol.getParent();
        }
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        trigger.rollback();
    }
}
