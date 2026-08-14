package org.zstack.storage.primary.imagestore.local;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeAO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVmMsg;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.local.LocalStorageSystemTags;
import org.zstack.storage.volume.VolumeSystemTags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2023/11/21 22:14
 */
public class LocalStorageImageStoreApiInterceptor implements GlobalApiMessageInterceptor {

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICloneVmInstanceMsg) {
            validate((APICloneVmInstanceMsg) msg);
        } else if (msg instanceof APICreateTemplatedVmInstanceFromVmInstanceMsg) {
            validate((APICreateTemplatedVmInstanceFromVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromTemplatedVmInstanceMsg) {
            validate((APICreateVmInstanceFromTemplatedVmInstanceMsg) msg);
        } else if (msg instanceof APIPrimaryStorageMigrateVmMsg) {
            validate((APIPrimaryStorageMigrateVmMsg) msg);
        }

        return msg;
    }

    void validate(APICloneVmInstanceMsg msg) {
        boolean fastCreate = msg.hasSystemTag(VolumeSystemTags.FAST_CREATE::isMatch);
        // It's not fast clone, just return
        if (!fastCreate) {
            return;
        }
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        List<String> psUuidList = vmInstanceVO.getAllDataVolumes()
                .stream().map(VolumeAO::getPrimaryStorageUuid).distinct().collect(Collectors.toList());

        if (psUuidList == null || psUuidList.isEmpty()) {
            return;
        }

        Boolean hasLocalPsDataVolume = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .in(PrimaryStorageVO_.uuid, psUuidList)
                .isExists();
        // fast clone, and no local data volume is attached , just return
        if (!hasLocalPsDataVolume) {
            return;
        }

        /*  On below condition, we need clone vm on source vm host
         *  1. Clone type is fast clone.
         *  2. Source vm is attached local ps volume.
         *  3. Doest not specific host in clone api msg.
         */
        String hostUuid = vmInstanceVO.getHostUuid() == null ? vmInstanceVO.getLastHostUuid() : vmInstanceVO.getHostUuid();
        if(StringUtils.isEmpty(msg.getHostUuid())) {
            msg.setHostUuid(hostUuid);
            return;
        }

        /*  On below condition, we need to throw api validation error
         *  1. Clone type is fast clone.
         *  2. Source vm is attached local ps volume.
         *  3. specific host in clone api msg but doest not match with the local ps volume host
         */
        if (!hostUuid.equals(msg.getHostUuid())) {
            throw new ApiMessageInterceptionException(argerr(
                    "The source vm has local data volume on host[uuid: %s]," +
                            "but in fast clone api msg try to clone vm to host[%s], which is impossible for fast clone feature.", hostUuid, msg.getHostUuid()));
        }

    }

    void validate(APICreateTemplatedVmInstanceFromVmInstanceMsg msg) {
        String hostUuid = getHostUuidFromVmVolume(msg.getVmInstanceUuid());
        if (hostUuid == null) {
            return;
        }
        msg.setHostUuid(hostUuid);
    }

    void validate(APICreateVmInstanceFromTemplatedVmInstanceMsg msg) {
        String hostUuid = getHostUuidFromVmVolume(msg.getTemplatedVmInstanceUuid());
        if (hostUuid == null) {
            return;
        }
        msg.setHostUuid(hostUuid);
    }

    void validate(APIPrimaryStorageMigrateVmMsg msg) {
        String dstPsType = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, msg.getDstPrimaryStorageUuid()).select(PrimaryStorageVO_.type).findValue();
        if (LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(dstPsType) && msg.getDstHostUuid() != null) {
            if (msg.getSystemTags() == null) {
                msg.setSystemTags(new ArrayList<>());
            }
            msg.getSystemTags().add(String.format("localStorage::hostUuid::%s", msg.getDstHostUuid()));
        }
    }

    private String getHostUuidFromVmVolume(String vmInstanceUuid) {
        String sql = "select ref.hostUuid from LocalStorageResourceRefVO ref " +
                "join VolumeVO volume on ref.resourceUuid = volume.uuid " +
                "join PrimaryStorageVO ps on ps.uuid = volume.primaryStorageUuid " +
                "where volume.vmInstanceUuid = :vmInstanceUuid " +
                "and ref.resourceType = :resourceType " +
                "and ps.type = :primaryType";

        return SQL.New(sql, String.class)
                .param("vmInstanceUuid", vmInstanceUuid)
                .param("resourceType", VolumeVO.class.getSimpleName())
                .param("primaryType", LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .limit(1)
                .find();
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(APICloneVmInstanceMsg.class,
                APICreateTemplatedVmInstanceFromVmInstanceMsg.class,
                APICreateVmInstanceFromTemplatedVmInstanceMsg.class,
                APIPrimaryStorageMigrateVmMsg.class);
    }
}
