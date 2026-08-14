package org.zstack.iscsi;

import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.storage.backup.BackupStorageSystemTags;

import java.util.Collections;

public class IscsiUtils {
    public static String getHostInitiatorName(String hostUuid) {
        return HostSystemTags.ISCSI_INITIATOR_NAME.getTokenByResourceUuid(hostUuid, HostVO.class, HostSystemTags.ISCSI_INITIATOR_NAME_TOKEN);
    }

    public static String getHostMnIpFromInitiatorName(String initiatorName) {
        String tag = HostSystemTags.ISCSI_INITIATOR_NAME.instantiateTag(
                Collections.singletonMap(HostSystemTags.ISCSI_INITIATOR_NAME_TOKEN, initiatorName));

        SystemTagVO tagVO = Q.New(SystemTagVO.class).eq(SystemTagVO_.tag, tag).find();
        if (tagVO == null) {
            return null;
        }

        if (tagVO.getResourceType().equals(HostVO.class.getSimpleName())) {
            return Q.New(HostVO.class).eq(HostVO_.uuid, tagVO.getResourceUuid()).select(HostVO_.managementIp).findValue();
        } else if (tagVO.getResourceType().equals(BackupStorageVO.class.getSimpleName())) {
            return getBsMnIp(tagVO.getResourceUuid());
        }

        return null;
    }

    private static String getBsMnIp(String bsUuid) {
        CloudBus bus = Platform.getComponentLoader().getComponent(CloudBus.class);
        GetBackupStorageManagerHostnameMsg msg = new GetBackupStorageManagerHostnameMsg();
        msg.setUuid(bsUuid);
        bus.makeLocalServiceId(msg, BackupStorageConstant.SERVICE_ID);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(reply.getError());
        }
        return ((GetBackupStorageManagerHostnameReply) reply).getHostname();
    }

    public static String getBSInitiatorName(String bsUuid) {
        return BackupStorageSystemTags.ISCSI_INITIATOR_NAME.getTokenByResourceUuid(bsUuid, BackupStorageVO.class, BackupStorageSystemTags.ISCSI_INITIATOR_NAME_TOKEN);
    }
}
