package org.zstack.storage.backup.imagestore;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostStatus;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStoragePrimaryStorageExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.image.ImageSystemTags;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephMonExtensionPoint;
import org.zstack.storage.ceph.backup.CephBackupStorageGlobalProperty;
import org.zstack.storage.ceph.backup.CephBackupStorageMonInventory;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2017/10/31.
 */
public class ImageStoreBackupStorageExtension implements BackupStoragePrimaryStorageExtensionPoint, CephMonExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ImageStoreBackupStorageExtension.class);

    @Autowired
    protected CloudBus bus;

    static private String agentPackageName = ImageStoreBackupStorageGlobalProperty.AGENT_PACKAGE_NAME;
    static private String agentClientPackageName = ImageStoreBackupStorageGlobalProperty.AGENT_CLIENT_PACKAGE_NAME;

    @Override
    public String getPrimaryStoragePriorityMap(BackupStorageInventory bs, ImageInventory image) {
        if (!bs.getType().equals(ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE)) {
            return null;
        }
        if (image != null) {
            String sourceType = ImageSystemTags.IMAGE_SOURCE_TYPE.getTokenByResourceUuid(image.getUuid(), ImageSystemTags.IMAGE_SOURCE_TYPE_TOKEN);
            if (CephConstants.CEPH_PRIMARY_STORAGE_TYPE.equals(sourceType)) {
                return CephBackupStorageGlobalProperty.CEPH_BACKUPSTORAGE_PRIORITY;
            }
        }
        return ImageStoreBackupStorageGlobalProperty.IMAGESTOREBACKUPSTORAGE_BACKUPSTORAGE_PRIORITY;
    }

    @Override
    /**
     * we install imagestore client agent for ceph mon
     */
    public void addMoreAgentInPrimaryStorage(CephPrimaryStorageMonInventory inventory, Completion completion) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            completion.success();
            return;
        }

        if (Q.New(ImageStoreBackupStorageVO.class)
                .eq(ImageStoreBackupStorageVO_.hostname, inventory.getHostname())
                .isExists()) {
            logger.warn("skip imagestore client deployment on ceph-ps-mon, it is also zstore.");
            completion.success();
            return;
        }

        logger.debug("add imagestore client agent for ceph mon");
        SshFileMd5Checker checker = new SshFileMd5Checker();
        String hostname = inventory.getHostname();
        Integer port = inventory.getSshPort();
        String username = inventory.getSshUsername();
        String password = inventory.getSshPassword();

        checker.setTargetIp(hostname);
        checker.setUsername(username);
        checker.setPassword(password);
        checker.setSshPort(port);
        checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/imagestorebackupstorage/%s", agentPackageName), true).getAbsolutePath(),
                String.format("/var/lib/zstack/imagestorebackupstorage/package/%s", agentClientPackageName));

        SshFileMd5Checker caChecker = new SshFileMd5Checker();
        caChecker.setTargetIp(hostname);
        caChecker.setUsername(username);
        caChecker.setPassword(password);
        caChecker.setSshPort(port);
        caChecker.addSrcDestPair( (PathUtil.join(PathUtil.getZStackHomeFolder(), "imagestore", "bin") + "/certs/ca.pem" ),
                ImageStoreBackupStorageGlobalProperty.REGISTRY_CERTS);

        AnsibleRunner runner = new AnsibleRunner();
        runner.installChecker(checker);
        runner.installChecker(caChecker);
        runner.setPassword(password);
        runner.setUsername(username);
        runner.setTargetIp(hostname);
        runner.setTargetUuid(inventory.getMonUuid());
        runner.setSshPort(port);
        runner.setPlayBookName(ImageStoreBackupStorageConstant.ANSIBLE_PLAYBOOK_NAME);

        ImageStoreAgentDeployArguments arguments = new ImageStoreAgentDeployArguments();
        arguments.setClient("true");
        runner.setDeployArguments(arguments);
        runner.run(new ReturnValueCompletion<Boolean>(completion) {
            @Override
            public void success(Boolean deployed) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                errorCode.setDetails(String.format("add imagestore client agent for ceph mon failed, caused by: %s", errorCode.getDetails()));
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public List<String> getBackupStorageSupportedPS(String psUuid) {
        List<String> result = new ArrayList<>();
        List<String> bsUuids = SQL.New("select b.uuid from PrimaryStorageVO p, ImageStoreBackupStorageVO b, BackupStorageZoneRefVO ref where " +
                "p.uuid = :puuid and b.uuid = ref.backupStorageUuid and ref.zoneUuid = p.zoneUuid").
                param("puuid", psUuid).list();
        for(String bsUuid: bsUuids) {
            if (!ImageStoreBackupStorageSelector.isRemote(bsUuid)) {
                result.add(bsUuid);
            }
        }
        return result;
    }

    /**
     * get cache path on primary storage and delete it
     * @param hostUuid: if not local storage, it is null
     */
    @Override
    public void cleanupPrimaryCacheForBS(PrimaryStorageInventory ps, String hostUuid, Completion completion) {
        if (ps.getAttachedClusterUuids().isEmpty()) {
            logger.warn(String.format("PS[uuid:%s] has not attached clusters", ps.getUuid()));
            completion.success();
            return;
        }

        CleanImageStoreLocalCacheMsg msg = new CleanImageStoreLocalCacheMsg();
        String host = hostUuid != null ? hostUuid : getConnectedHost(ps);
        msg.setHostUuid(host);
        msg.setMountPath(ps.getMountPath());
        bus.makeLocalServiceId(msg, ImageStoreBackupStorageConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                logger.info(String.format("cleaned ImageStore cache for PS[uuid:%s] on host[uuid:%s]",
                        ps.getUuid(), host));
                completion.success();
            }
        });
    }

    private String getConnectedHost(final PrimaryStorageInventory ps) {
        List<String> hostUuids = SQL.New("select h.uuid from HostVO h " +
                "where h.status = :connectionState and h.clusterUuid in (:clusterUuids)")
                .param("connectionState", HostStatus.Connected)
                .param("clusterUuids", ps.getAttachedClusterUuids())
                .limit(5)
                .list();
        if (!hostUuids.isEmpty()) {
            Collections.shuffle(hostUuids);
            return hostUuids.get(0);
        }

        throw new OperationFailureException(
                operr("cannot find a connected host in cluster to which PS [uuid: %s] attached", ps.getUuid()));
    }

    @Override
    public void addMoreAgentInBackupStorage(CephBackupStorageMonInventory inventory, Completion completion) {
        completion.success();
    }
}
