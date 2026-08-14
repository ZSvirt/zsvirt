package org.zstack.storage.migration.backup.ceph;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.CheckInstallPathOnBSMsg;
import org.zstack.header.storage.backup.CheckInstallPathOnBSReply;
import org.zstack.header.storage.backup.DeleteBitsOnBackupStorageMsg;
import org.zstack.storage.ceph.MonStatus;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO;
import org.zstack.storage.ceph.backup.CephBackupStorageVO;
import org.zstack.storage.ceph.backup.CephToCephMigrateImageMsg;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by GuoYi on 9/2/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephToCephMigrateImageFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(CephToCephMigrateImageFlow.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected ApiTimeoutManager timeoutMgr;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        String imageUuid = (String) data.get(StorageMigrationConstant.IMAGE_UUID);
        String srcBsUuid = (String) data.get(StorageMigrationConstant.SRC_BS_UUID);
        String dstBsUuid = (String) data.get(StorageMigrationConstant.DST_BS_UUID);
        CephBackupStorageVO srcBsVO = dbf.findByUuid(srcBsUuid, CephBackupStorageVO.class);
        CephBackupStorageVO dstBsVO = dbf.findByUuid(dstBsUuid, CephBackupStorageVO.class);

        // update image status before migration
        ImageVO image = dbf.findByUuid(imageUuid, ImageVO.class);
        image.setStatus(ImageStatus.Migrating);
        dbf.update(image);
        ImageBackupStorageRefVO ref = Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, srcBsUuid)
                .eq(ImageBackupStorageRefVO_.imageUuid, imageUuid)
                .find();
        ref.setStatus(ImageStatus.Migrating);
        dbf.update(ref);

        // get install path of dstImage
        // different install path after storage migration
        final String dstImageInstallPath = makeImageInstallPath(dstBsVO.getPoolName(), Platform.getUuid());

        data.put(StorageMigrationConstant.DST_IMAGE_INSTALL_PATH, dstImageInstallPath);

        // get size of srcImage
        ImageVO srcImageVO = dbf.findByUuid(imageUuid, ImageVO.class);
        long srcImageSize = srcImageVO.getSize();

        // choose one from the connected mons of source ceph backup storage
        List<CephBackupStorageMonVO> srcConnectedMons = new ArrayList<>();
        for (CephBackupStorageMonVO srcMon : srcBsVO.getMons()) {
            if (srcMon.getStatus() == MonStatus.Connected) {
                srcConnectedMons.add(srcMon);
            }
        }
        if (srcConnectedMons.isEmpty()) {
            throw new OperationFailureException(Platform.operr(
                    "all ceph mons are Disconnected in ceph backup storage[uuid:%s]", srcBsVO.getUuid()
            ));
        }
        Collections.shuffle(srcConnectedMons);
        CephBackupStorageMonVO srcMon = srcConnectedMons.get(0);

        // choose one from the connected mons of destination ceph backup storage
        List<CephBackupStorageMonVO> dstConnectedMons = new ArrayList<>();
        for (CephBackupStorageMonVO dstMon : dstBsVO.getMons()) {
            if (dstMon.getStatus() == MonStatus.Connected) {
                dstConnectedMons.add(dstMon);
            }
        }
        if (dstConnectedMons.isEmpty()) {
            throw new OperationFailureException(Platform.operr(
                    "all ceph mons are Disconnected in ceph backup storage[uuid:%s]", dstBsVO.getUuid()
            ));
        }
        Collections.shuffle(dstConnectedMons);
        CephBackupStorageMonVO dstMon = dstConnectedMons.get(0);


        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.enableProgressReport();
        chain.setName(String.format("migrate-image-%s-from-bs-%s-to-bs-%s", imageUuid, srcBsUuid, dstBsUuid));

        chain.then(new NoRollbackFlow() {
            String __name__ = "check if install path in trash";
            @Override
            public void run(FlowTrigger cTrigger, Map data) {
                CheckInstallPathOnBSMsg cmsg = new CheckInstallPathOnBSMsg();
                cmsg.setBackupStorageUuid(dstBsUuid);
                cmsg.setInstallPath(dstImageInstallPath);
                bus.makeTargetServiceIdByResourceUuid(cmsg, BackupStorageConstant.SERVICE_ID, dstBsUuid);
                bus.send(cmsg, new CloudBusCallBack(cTrigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            CheckInstallPathOnBSReply reply1 = reply.castReply();
                            if (reply1.getTrashId() == null) {
                                cTrigger.next();
                            } else {
                                cTrigger.fail(Platform.operr("found trashId(%s) in BackupStorage [%s] for the migrate installPath[%s]. Please clean it first by 'APICleanUpTrashOnBackupStorageMsg' if you insist to migrate the image[%s]",
                                        reply1.getTrashId(), dstBsUuid, dstImageInstallPath, reply1.getResourceUuid()));
                            }
                        } else {
                            cTrigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "migrate image";
            @Override
            public void run(FlowTrigger cTrigger, Map data) {
                CephToCephMigrateImageMsg msg = new CephToCephMigrateImageMsg();
                msg.setImageUuid(imageUuid);
                msg.setImageSize(srcImageSize);
                msg.setSrcInstallPath(ref.getInstallPath());
                msg.setDstInstallPath(dstImageInstallPath);
                msg.setDstMonHostname(dstMon.getHostname());
                msg.setDstMonSshUsername(dstMon.getSshUsername());
                msg.setDstMonSshPassword(dstMon.getSshPassword());
                msg.setDstMonSshPort(dstMon.getSshPort());
                msg.setBackupStorageUuid(srcBsUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, imageUuid);
                bus.send(msg, new CloudBusCallBack(cTrigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            logger.info(String.format("Migrated Image %s from BS %s to BS %s.", imageUuid, srcBsUuid, dstBsUuid));
                            cTrigger.next();
                        } else {
                            cTrigger.fail(reply.getError());
                        }
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
                trigger.fail(Platform.operr("Failed to migrate Image %s from BS %s to BS %s. cause: %s", imageUuid, srcBsUuid, dstBsUuid, errCode.getDetails()));
            }
        }).start();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        String srcBsUuid = (String) data.get(StorageMigrationConstant.SRC_BS_UUID);
        String dstBsUuid = (String) data.get(StorageMigrationConstant.DST_BS_UUID);
        String dstImageInstallPath = (String) data.get(StorageMigrationConstant.DST_IMAGE_INSTALL_PATH);

        // recover image status
        String imageUuid = (String) data.get(StorageMigrationConstant.IMAGE_UUID);
        ImageVO image = dbf.findByUuid(imageUuid, ImageVO.class);
        image.setStatus(ImageStatus.Ready);
        dbf.update(image);
        ImageBackupStorageRefVO ref = Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, srcBsUuid)
                .eq(ImageBackupStorageRefVO_.imageUuid, imageUuid)
                .find();
        ref.setStatus(ImageStatus.Ready);
        dbf.update(ref);

        // call agent
        DeleteBitsOnBackupStorageMsg dmsg = new DeleteBitsOnBackupStorageMsg();
        dmsg.setBackupStorageUuid(dstBsUuid);
        dmsg.setInstallPath(dstImageInstallPath);
        bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, dstBsUuid);
        bus.send(dmsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.info(String.format("Delete the migrated image %s.", dstImageInstallPath));
                } else {
                    logger.warn(String.format("Failed to delete the migrated image %s.", dstImageInstallPath));
                }
            }
        });
        trigger.rollback();
    }

    private String makeImageInstallPath(String poolName, String imageUuid) {
        return String.format("ceph://%s/%s", poolName, imageUuid);
    }
}
