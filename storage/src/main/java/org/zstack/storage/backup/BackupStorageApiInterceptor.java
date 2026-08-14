package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.backup.*;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 4:21 PM
 * To change this template use File | Settings | File Templates.
 */
@InterceptorForService("storage.backup")
public class BackupStorageApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof BackupStorageMessage) {
            BackupStorageMessage bmsg = (BackupStorageMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, bmsg.getBackupStorageUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAttachBackupStorageToZoneMsg) {
            validate((APIAttachBackupStorageToZoneMsg) msg);
        } else if (msg instanceof APIDetachBackupStorageFromZoneMsg) {
            validate((APIDetachBackupStorageFromZoneMsg) msg);
        } else if (msg instanceof APIDeleteBackupStorageMsg) {
            validate((APIDeleteBackupStorageMsg) msg);
        } else if (msg instanceof APIExportImageFromBackupStorageMsg) {
            validate((APIExportImageFromBackupStorageMsg) msg);
        } else if (msg instanceof APIDeleteExportedImageFromBackupStorageMsg) {
            validate((APIDeleteExportedImageFromBackupStorageMsg) msg);
        } else if (msg instanceof APIGetBackupStorageCapacityMsg) {
            validate((APIGetBackupStorageCapacityMsg) msg);
        } else if (msg instanceof APIGetTrashOnBackupStorageMsg) {
            validate((APIGetTrashOnBackupStorageMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void checkNull(final String name, final String val) {
        if (val == null) {
            throw new ApiMessageInterceptionException(argerr("%s should not be null", name));
        }
    }
    private void validate(APIDeleteExportedImageFromBackupStorageMsg msg) {
        checkNull("backup storage uuid", msg.getBackupStorageUuid());
        checkNull("image uuid", msg.getImageUuid());
    }

    private void validate(APIExportImageFromBackupStorageMsg msg) {
        checkNull("backup storage uuid", msg.getBackupStorageUuid());
        checkNull("image uuid", msg.getImageUuid());
    }

    private void validate(APIGetBackupStorageCapacityMsg msg) {
        boolean pass = false;
        if (msg.getBackupStorageUuids() != null && !msg.getBackupStorageUuids().isEmpty()) {
            pass = true;
        }
        if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
            pass = true;
        }

        if (!pass && !msg.isAll()) {
            throw new ApiMessageInterceptionException(argerr("zoneUuids, backupStorageUuids must have at least one be none-empty list, or all is set to true"));
        }

        if (msg.isAll() && (msg.getBackupStorageUuids() == null || msg.getBackupStorageUuids().isEmpty())) {
            List<String> bsUuids = Q.New(BackupStorageVO.class)
                    .select(BackupStorageVO_.uuid)
                    .listValues();
            msg.setBackupStorageUuids(bsUuids);

            if (msg.getBackupStorageUuids().isEmpty()) {
                APIGetBackupStorageCapacityReply reply = new APIGetBackupStorageCapacityReply();
                bus.reply(msg, reply);
                throw new StopRoutingException();
            }
        }
    }

    private void validate(APIDeleteBackupStorageMsg msg) {
        if (!dbf.isExist(msg.getUuid(), BackupStorageVO.class)) {
            APIDeleteBackupStorageEvent evt = new APIDeleteBackupStorageEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APIDetachBackupStorageFromZoneMsg msg) {
        if (!Q.New(BackupStorageZoneRefVO.class)
                .eq(BackupStorageZoneRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                .eq(BackupStorageZoneRefVO_.zoneUuid, msg.getZoneUuid())
                .isExists()) {
            throw new ApiMessageInterceptionException(operr("backup storage[uuid:%s] has not been attached to zone[uuid:%s]", msg.getBackupStorageUuid(), msg.getZoneUuid()));
        }
    }

    private void validate(APIAttachBackupStorageToZoneMsg msg) {
        if (Q.New(BackupStorageZoneRefVO.class)
                .eq(BackupStorageZoneRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                .eq(BackupStorageZoneRefVO_.zoneUuid, msg.getZoneUuid())
                .isExists()) {
            throw new ApiMessageInterceptionException(operr("backup storage[uuid:%s] has been attached to zone[uuid:%s]", msg.getBackupStorageUuid(), msg.getZoneUuid()));
        }
    }

    private void validate(final APIGetTrashOnBackupStorageMsg msg) {
        if ((msg.getResourceType() != null) ^ (msg.getResourceUuid() != null)) {
            throw new ApiMessageInterceptionException((argerr("'resourceUuid' and 'resourceType' must be set both or neither!")));
        }
    }

    /**
     * Former message-level on sftp/imagestore reconnect (setServiceId via BackupStorageMessage).
     * Optional plugins loaded by name.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        List<Class> list = new ArrayList<>(2);
        for (String name : new String[]{
                "org.zstack.storage.backup.sftp.APIReconnectSftpBackupStorageMsg",
                "org.zstack.storage.backup.imagestore.APIReconnectImageStoreBackupStorageMsg"
        }) {
            try {
                list.add(Class.forName(name));
            } catch (ClassNotFoundException ignored) {}
        }
        return list;
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.DEFAULT;
    }

}
