package org.zstack.storage.migration.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.storage.migration.StorageMigrationMessage;

/**
 *  *  *  *  * * Created by LiangHanYu on 2020/8/26 16:52
 *   *   *   *   */
@RestRequest(
        path = "/primary-storage/hosts/{vmInstanceUuid}/migration-candidates",
        method = HttpMethod.GET,
        responseClass = APIGetHostCandidatesForVmMigrationReply.class
)
public class APIGetHostCandidatesForVmMigrationMsg extends APISyncCallMessage implements StorageMigrationMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(resourceType = PrimaryStorageVO.class)
    private String dstPrimaryStorageUuid;

    @APIParam(required = false, numberRange = {1, Integer.MAX_VALUE})
    private Integer limit;

    public static APIGetHostCandidatesForVmMigrationMsg __example__() {
        APIGetHostCandidatesForVmMigrationMsg msg = new APIGetHostCandidatesForVmMigrationMsg();
        msg.setVmInstanceUuid(uuid());
        msg.setDstPrimaryStorageUuid(uuid());
        return msg;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public String getDstPrimaryStorageUuid() {
        return dstPrimaryStorageUuid;
    }

    public void setDstPrimaryStorageUuid(String dstPrimaryStorageUuid) {
        this.dstPrimaryStorageUuid = dstPrimaryStorageUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
