package org.zstack.storage.backup.imagestore;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.KVMChangeHostPasswordExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.backup.*;
import org.zstack.kvm.KVMHostVO;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.argerr;

public class ImageStoreBackupStorageFactory implements BackupStorageFactory, GlobalApiMessageInterceptor, Component,
        KVMChangeHostPasswordExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ImageStoreBackupStorageFactory.class);
    public static BackupStorageType type = new BackupStorageType(ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE, BackupStorageConstant.SCHEME_HTTP,
            BackupStorageConstant.SCHEME_HTTPS, BackupStorageConstant.SCHEME_NFS, BackupStorageConstant.SCHEME_FILE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private TagManager tagMgr;

    static {
        type.setOrder(949);
    }

    @Override
    public BackupStorageType getBackupStorageType() {
        return type;
    }

    @Override
    @Deferred
    public BackupStorageInventory createBackupStorage(BackupStorageVO vo, APIAddBackupStorageMsg msg) {
        APIAddImageStoreBackupStorageMsg amsg = (APIAddImageStoreBackupStorageMsg) msg;
        final ImageStoreBackupStorageVO lvo = new ImageStoreBackupStorageVO(vo);
        lvo.setHostname(amsg.getHostname());
        lvo.setUsername(amsg.getUsername());
        lvo.setPassword(amsg.getPassword());
        lvo.setSshPort(amsg.getSshPort());
        lvo.setUrl(amsg.getUrl());

        dbf.persistAndRefresh(lvo);
        tagMgr.createTagsFromAPICreateMessage(msg, lvo.getUuid(), ImageStoreBackupStorageVO.class.getSimpleName(), false);
        Defer.guard(() -> {
            dbf.remove(lvo);
        });

        if (msg instanceof APIAddDisasterImageStoreBackupStorageMsg) {
            doSshMount((APIAddDisasterImageStoreBackupStorageMsg)msg);
        }

        return ImageStoreBackupStorageInventory.valueOf(lvo);
    }

    private void doSshMount(APIAddDisasterImageStoreBackupStorageMsg msg) {
        if (msg.getAttachPoint() != null && msg.getEndPoint() != null) {
            final String script = String.format("mkdir -p %s && sudo mount -t nfs4 %s:%s %s",
                    msg.getUrl(), msg.getEndPoint(), msg.getAttachPoint(), msg.getUrl());

            new Ssh().shell(script).setTimeout(45)
                    .setPrivateKey(asf.getPrivateKey())
                    .setUsername(msg.getUsername()).setPassword(msg.getPassword())
                    .setHostname(msg.getHostname()).setPort(msg.getSshPort())
                    .runErrorByExceptionAndClose();
        }
    }

    @Override
    public BackupStorage getBackupStorage(BackupStorageVO vo) {
        ImageStoreBackupStorageVO lvo = dbf.findByUuid(vo.getUuid(), ImageStoreBackupStorageVO.class);
        return new ImageStoreBackupStorage(lvo);
    }

    @Override
    public BackupStorageInventory reload(String uuid) {
        ImageStoreBackupStorageVO vo = dbf.findByUuid(uuid, ImageStoreBackupStorageVO.class);
        return ImageStoreBackupStorageInventory.valueOf(vo);
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            if (msg instanceof APIAddImageStoreBackupStorageMsg) {
                APIAddImageStoreBackupStorageMsg amsg = (APIAddImageStoreBackupStorageMsg) msg;
                String url = amsg.getUrl();
                if (!url.startsWith("/")) {
                    ErrorCode err = argerr("invalid url[%s], the url must be an absolute path starting with '/'", amsg.getUrl());
                    throw new ApiMessageInterceptionException(err);
                }

                String hostname = amsg.getHostname();
                SimpleQuery<ImageStoreBackupStorageVO> query = dbf.createQuery(ImageStoreBackupStorageVO.class);
                query.add(ImageStoreBackupStorageVO_.hostname, Op.EQ, hostname);
                long count = query.count();
                if (count != 0) {
                    ErrorCode err = argerr("existing SimpleHttpBackupStorage with hostname[%s] found", hostname);
                    throw new ApiMessageInterceptionException(err);
                }
            }
        }

        return msg;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> clzs = new ArrayList<Class>(1);
        clzs.add(APIAddBackupStorageMsg.class);
        return clzs;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }


    private void deploySaltState() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        asf.deployModule(ImageStoreBackupStorageConstant.ANSIBLE_MODULE_PATH, ImageStoreBackupStorageConstant.ANSIBLE_PLAYBOOK_NAME);
    }

    @Override
    public boolean start() {
        deploySaltState();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void afterChangeHostPassword(KVMHostVO kvmHostVO) {
        ImageStoreBackupStorageVO backupStorageVO = Q.New(ImageStoreBackupStorageVO.class).eq(ImageStoreBackupStorageVO_.hostname, kvmHostVO.getManagementIp()).find();
        if (backupStorageVO == null) {
            return;
        }

        backupStorageVO.setPassword(kvmHostVO.getPassword());
        dbf.updateAndRefresh(backupStorageVO);
    }
}
