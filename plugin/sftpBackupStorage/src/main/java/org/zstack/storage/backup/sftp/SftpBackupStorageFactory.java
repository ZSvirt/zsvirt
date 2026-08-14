package org.zstack.storage.backup.sftp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.backup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.argerr;

import java.util.ArrayList;
import java.util.List;

public class SftpBackupStorageFactory implements BackupStorageFactory, GlobalApiMessageInterceptor, Component {
    private static final CLogger logger = Utils.getLogger(SftpBackupStorageFactory.class);
    public static BackupStorageType type = new BackupStorageType(
            SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE,
            BackupStorageConstant.SCHEME_HTTP,
            BackupStorageConstant.SCHEME_HTTPS,
            BackupStorageConstant.SCHEME_NFS,
            BackupStorageConstant.SCHEME_FILE,
            BackupStorageConstant.SCHEME_FTP,
            BackupStorageConstant.SCHEME_SFTP
    );

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private ErrorFacade errf;

    static {
        type.setOrder(999);
    }

    @Override
    public BackupStorageType getBackupStorageType() {
        return type;
    }

    @Override
    public BackupStorageInventory createBackupStorage(BackupStorageVO vo, APIAddBackupStorageMsg msg) {
        APIAddSftpBackupStorageMsg amsg = (APIAddSftpBackupStorageMsg) msg;
        final SftpBackupStorageVO lvo = new SftpBackupStorageVO(vo);
        lvo.setHostname(amsg.getHostname());
        lvo.setUsername(amsg.getUsername());
        lvo.setPassword(amsg.getPassword());
        lvo.setSshPort(amsg.getSshPort());
        dbf.persist(lvo);
        return SftpBackupStorageInventory.valueOf(lvo);
    }

    @Override
    public BackupStorage getBackupStorage(BackupStorageVO vo) {
        SftpBackupStorageVO lvo = dbf.findByUuid(vo.getUuid(), SftpBackupStorageVO.class);
        return new SftpBackupStorage(lvo);
    }

    @Override
    public BackupStorageInventory reload(String uuid) {
        SftpBackupStorageVO vo = dbf.findByUuid(uuid, SftpBackupStorageVO.class);
        return SftpBackupStorageInventory.valueOf(vo);
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            if (msg instanceof APIAddSftpBackupStorageMsg) {
                APIAddSftpBackupStorageMsg amsg = (APIAddSftpBackupStorageMsg) msg;
                String url = amsg.getUrl();
                if (!url.startsWith("/")) {
                    ErrorCode err = argerr("invalid url[%s], the url must be an absolute path starting with '/'", amsg.getUrl());
                    throw new ApiMessageInterceptionException(err);
                }

                String hostname = amsg.getHostname();
                SimpleQuery<SftpBackupStorageVO> query = dbf.createQuery(SftpBackupStorageVO.class);
                query.add(SftpBackupStorageVO_.hostname, Op.EQ, hostname);
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

        asf.deployModule(SftpBackupStorageConstant.ANSIBLE_MODULE_PATH, SftpBackupStorageConstant.ANSIBLE_PLAYBOOK_NAME);
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
}
