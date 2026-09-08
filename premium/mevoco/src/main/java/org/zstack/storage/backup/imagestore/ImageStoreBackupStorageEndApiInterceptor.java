package org.zstack.storage.backup.imagestore;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.storage.backup.sftp.APIAddSftpBackupStorageMsg;
import org.zstack.storage.backup.sftp.SftpBackupStorageVO;
import org.zstack.storage.backup.sftp.SftpBackupStorageVO_;

import static org.zstack.core.Platform.operr;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class ImageStoreBackupStorageEndApiInterceptor implements GlobalApiMessageInterceptor {
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
        boolean sftpExists = Q.New(SftpBackupStorageVO.class)
                .eq(SftpBackupStorageVO_.hostname, hostname)
                .isExists();
        if (sftpExists) {
            throw new ApiMessageInterceptionException(operr("More than one BackupStorage on the same host identified by hostname. " +
                            "There has been a SftpBackupStorage [hostname:%s] existing. " +
                            "The BackupStorage type to be added is %s. ", hostname, newBS));
        }

        boolean iStoreExists = Q.New(ImageStoreBackupStorageVO.class)
                .eq(ImageStoreBackupStorageVO_.hostname, hostname)
                .isExists();
        if (iStoreExists) {
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
