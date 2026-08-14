package org.zstack.apimediator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.storage.backup.imagestore.APIAddImageStoreBackupStorageMsg;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO_;
import org.zstack.storage.backup.sftp.APIAddSftpBackupStorageMsg;
import org.zstack.storage.backup.sftp.SftpBackupStorageVO;
import org.zstack.storage.backup.sftp.SftpBackupStorageVO_;

import static org.zstack.core.Platform.operr;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class ApiValidator implements GlobalApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APIAddImageStoreBackupStorageMsg.class);
        ret.add(APIAddSftpBackupStorageMsg.class);
        return ret;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddImageStoreBackupStorageMsg) {
            validate((APIAddImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APIAddSftpBackupStorageMsg) {
            validate((APIAddSftpBackupStorageMsg) msg);
        }

        return msg;
    }

    private void validateMoreThanOneBackupStorageOnSameHost(String hostname, String newBS) {
        SimpleQuery<SftpBackupStorageVO> q = dbf.createQuery(SftpBackupStorageVO.class);
        q.add(SftpBackupStorageVO_.hostname, Op.EQ, hostname);
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("More than one BackupStorage on the same host identified by hostname. " +
                            "There has been a SftpBackupStorage [hostname:%s] existing. " +
                            "The BackupStorage type to be added is %s. ", hostname, newBS));
        }

        SimpleQuery<ImageStoreBackupStorageVO> q1 = dbf.createQuery(ImageStoreBackupStorageVO.class);
        q1.add(ImageStoreBackupStorageVO_.hostname, Op.EQ, hostname);
        if (q1.isExists()) {
            throw new ApiMessageInterceptionException(operr("More than one BackupStorage on the same host identified by hostname. " +
                            "There has been an ImageStoreBackupStorage [hostname:%s] existing. " +
                            "The BackupStorage type to be added is %s. ", hostname, newBS));
        }
    }

    private void validate(APIAddImageStoreBackupStorageMsg msg) {
        validateMoreThanOneBackupStorageOnSameHost(msg.getHostname(), msg.getType());
    }

    private void validate(APIAddSftpBackupStorageMsg msg) {
        validateMoreThanOneBackupStorageOnSameHost(msg.getHostname(), msg.getType());
    }
}
