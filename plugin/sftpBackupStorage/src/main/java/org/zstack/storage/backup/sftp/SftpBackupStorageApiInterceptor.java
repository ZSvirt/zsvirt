package org.zstack.storage.backup.sftp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg;
import org.zstack.header.message.APIMessage;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.utils.network.NetworkUtils;

import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

/**
 */
@InterceptorForService("storage.backup.sftp")
public class SftpBackupStorageApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddSftpBackupStorageMsg) {
            validate((APIAddSftpBackupStorageMsg)msg);
        } else if (msg instanceof APIQuerySftpBackupStorageMsg) {
            validate((APIQuerySftpBackupStorageMsg)msg);
        } else if (msg instanceof APIUpdateSftpBackupStorageMsg) {
            validate((APIUpdateSftpBackupStorageMsg) msg);
        } else if (msg instanceof APICreateRootVolumeTemplateFromRootVolumeMsg) {
            validate((APICreateRootVolumeTemplateFromRootVolumeMsg) msg);
        }

        return msg;
    }

    private void validate(APICreateRootVolumeTemplateFromRootVolumeMsg msg) {
        if (msg.getBackupStorageUuids() == null || msg.getBackupStorageUuids().isEmpty()) {
            return;
        }

        // if vm in running or pause
        if (Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.rootVolumeUuid, msg.getRootVolumeUuid())
                .in(VmInstanceVO_.state, Arrays.asList(VmInstanceState.Running, VmInstanceState.Paused)).isExists()) {
            for (String bsUuid : msg.getBackupStorageUuids()) {
                String bsType = Q.New(BackupStorageVO.class)
                        .eq(BackupStorageVO_.uuid, bsUuid)
                        .select(BackupStorageVO_.type).findValue();

                if (bsType.equals(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE)) {
                    throw new ApiMessageInterceptionException(argerr("Please stop the vm before create volume template to sftp backup storage %s", bsUuid));
                }
            }
        }
    }

    private void validate(APIUpdateSftpBackupStorageMsg msg) {
        if (msg.getHostname() != null && !NetworkUtils.isIpv4Address(msg.getHostname()) && !NetworkUtils.isHostname(msg.getHostname())) {
            throw new ApiMessageInterceptionException(argerr("hostname[%s] is neither an IPv4 address nor a valid hostname", msg.getHostname()));
        }
    }

    private void validate(APIQuerySftpBackupStorageMsg msg) {
        boolean found = false;
        for (QueryCondition qcond : msg.getConditions()) {
            if ("type".equals(qcond.getName())) {
                qcond.setOp(QueryOp.EQ.toString());
                qcond.setValue(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE);
                found = true;
                break;
            }
        }

        if (!found) {
            msg.addQueryCondition("type", QueryOp.EQ, SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE);
        }
    }

    private void validate(APIAddSftpBackupStorageMsg msg) {
        if (!NetworkUtils.isIpv4Address(msg.getHostname()) && !NetworkUtils.isHostname(msg.getHostname())) {
            throw new ApiMessageInterceptionException(argerr("hostname[%s] is neither an IPv4 address nor a valid hostname", msg.getHostname()));
        }

        SimpleQuery<SftpBackupStorageVO> q = dbf.createQuery(SftpBackupStorageVO.class);
        q.add(SftpBackupStorageVO_.hostname, Op.EQ, msg.getHostname());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("duplicate backup storage. There has been a sftp backup storage[hostname:%s] existing", msg.getHostname()));
        }
        String dir = msg.getUrl();
        if (dir.startsWith("/proc")||dir.startsWith("/dev") || dir.startsWith("/sys")) {
            throw new ApiMessageInterceptionException(argerr(" the url contains an invalid folder[/dev or /proc or /sys]"));
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return list(
            APICreateRootVolumeTemplateFromRootVolumeMsg.class,
            APIAddSftpBackupStorageMsg.class,
            APIUpdateSftpBackupStorageMsg.class,
            APIQuerySftpBackupStorageMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }
}
