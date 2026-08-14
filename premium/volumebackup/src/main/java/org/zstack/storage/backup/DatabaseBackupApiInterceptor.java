package org.zstack.storage.backup;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.database.backup.*;
import org.zstack.header.zone.Zone;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.message.OperationChecker;
import org.zstack.zql.ZQL;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.i18n;
import static org.zstack.core.Platform.operr;

@InterceptorForService("backup.database")
public class DatabaseBackupApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;

    protected static OperationChecker allowedOperations = new OperationChecker(true);

    static {
        allowedOperations.addState(BackupStorageState.Enabled,
                APICreateDatabaseBackupMsg.class.getName(),
                APIExportDatabaseBackupFromBackupStorageMsg.class.getName(),
                APIDeleteExportedDatabaseBackupFromBackupStorageMsg.class.getName()
        );
    }

    private void validateOperationOnBackupStorage(OperationChecker checker, Message msg, String bsUuid) {
        BackupStorageState bsState = Q.New(BackupStorageVO.class).select(BackupStorageVO_.state).eq(BackupStorageVO_.uuid, bsUuid).findValue();
        validateOperationByState(checker, msg, bsState);
    }

    private void validateOperationByState(OperationChecker checker, Message msg, BackupStorageState currentState) {
        if (!checker.isOperationAllowed(msg.getMessageName(), currentState.toString())) {
            throw new ApiMessageInterceptionException(operr("current backup storage state[%s] doesn't allow to proceed message[%s], allowed states are %s", currentState,
                    msg.getMessageName(), checker.getStatesForOperation(msg.getMessageName())));
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);

        if (msg instanceof APIExportDatabaseBackupFromBackupStorageMsg) {
            validate((APIExportDatabaseBackupFromBackupStorageMsg) msg);
        } else if (msg instanceof APIDeleteExportedDatabaseBackupFromBackupStorageMsg) {
            validate((APIDeleteExportedDatabaseBackupFromBackupStorageMsg) msg);
        } else if (msg instanceof APIRecoverDatabaseFromBackupMsg) {
            validate((APIRecoverDatabaseFromBackupMsg) msg);
        } else if (msg instanceof APIGetDatabaseBackupFromImageStoreMsg) {
            validate((APIGetDatabaseBackupFromImageStoreMsg) msg);
        } else if (msg instanceof APICreateDatabaseBackupMsg) {
            validate((APICreateDatabaseBackupMsg) msg);
        }

        return msg;
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof BackupStorageMessage) {
            BackupStorageMessage bmsg = (BackupStorageMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, DatabaseBackupConstant.SERVICE_ID, bmsg.getBackupStorageUuid());
        }
    }

    private void validate(APICreateDatabaseBackupMsg msg) {
        validateOperationOnBackupStorage(allowedOperations, msg, msg.getBackupStorageUuid());
    }

    private void validate(APIDeleteExportedDatabaseBackupFromBackupStorageMsg msg) {
        validateOperationOnBackupStorage(allowedOperations, msg, msg.getBackupStorageUuid());

        boolean exported = Q.New(DatabaseBackupStorageRefVO.class).notNull(DatabaseBackupStorageRefVO_.exportUrl)
                .eq(DatabaseBackupStorageRefVO_.databaseBackupUuid, msg.getDatabaseBackupUuid())
                .eq(DatabaseBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                .isExists();
        if (!exported) {
            throw new ApiMessageInterceptionException(operr(
                    "database backup[uuid%s] has not been exported from backupStorage[uuid:%s]",
                    msg.getDatabaseBackupUuid(), msg.getBackupStorageUuid()));
        }
    }

    private void validate(APIExportDatabaseBackupFromBackupStorageMsg msg){
        validateOperationOnBackupStorage(allowedOperations, msg, msg.getBackupStorageUuid());

        boolean exported = Q.New(DatabaseBackupStorageRefVO.class).notNull(DatabaseBackupStorageRefVO_.exportUrl)
                .eq(DatabaseBackupStorageRefVO_.databaseBackupUuid, msg.getDatabaseBackupUuid())
                .eq(DatabaseBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                .isExists();
        if (exported) {
            throw new ApiMessageInterceptionException(operr(
                    "database backup[uuid%s] has been exported from backupStorage[uuid:%s]",
                    msg.getDatabaseBackupUuid(), msg.getBackupStorageUuid()));
        }
    }

    private void validate(APIGetDatabaseBackupFromImageStoreMsg msg) {
        checkUrl(msg.getUrl());
    }

    @Transactional(readOnly = true)
    private void validate(APIRecoverDatabaseFromBackupMsg msg) {
        if (!DatabaseBackupGlobalConfig.ALLOW_COVER_DATABASE.value(Boolean.class) && Q.New(ZoneVO.class).isExists()) {
            throw new ApiMessageInterceptionException(operr("do not allow cover database from backup"));
        }

        if (msg.getUuid() == null && (msg.getBackupInstallPath() == null || msg.getBackupStorageUrl() == null)) {
            throw new ApiMessageInterceptionException(argerr("installPath and bsUrl are both need"));
        }

        if (msg.getUuid() != null) {
            boolean isExist = Q.New(DatabaseBackupVO.class).eq(DatabaseBackupVO_.uuid, msg.getUuid())
                    .eq(DatabaseBackupVO_.state, DatabaseBackupState.Enabled)
                    .eq(DatabaseBackupVO_.status,DatabaseBackupStatus.Ready)
                    .isExists();
            if (!isExist) {
                throw new ApiMessageInterceptionException(argerr("databaseBackup[uuid:%s] is not Enabled and Ready"));
            }
        } else {
            checkUrl(msg.getBackupStorageUrl());
        }
        //todo: check mn ha
    }

    private void checkUrl(String url){
        String err = i18n("illegal url[%s], correct example is ssh://username:password@hostname[:sshPort]/path", url);
        try {
            URI uri = new URI(url);
            if (Strings.isEmpty(uri.getPath()) || Strings.isEmpty(uri.getHost()) || Strings.isEmpty(uri.getUserInfo())) {
                throw new ApiMessageInterceptionException(operr(err));
            }
        } catch (URISyntaxException e) {
            throw new ApiMessageInterceptionException(operr(err));
        }
    }
}
