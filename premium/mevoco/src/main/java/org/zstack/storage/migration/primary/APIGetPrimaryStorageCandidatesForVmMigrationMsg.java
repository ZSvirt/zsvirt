package org.zstack.storage.migration.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.storage.migration.StorageMigrationMessage;

@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/storage-migration-candidates",
        method = HttpMethod.GET,
        responseClass = APIGetPrimaryStorageCandidatesForVmMigrationReply.class
)
public class APIGetPrimaryStorageCandidatesForVmMigrationMsg extends APISyncCallMessage implements StorageMigrationMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(required = false)
    private boolean withDataVolumes = true;

    public boolean isMigrateStorageOnly() {
        return migrateStorageOnly;
    }

    public void setMigrateStorageOnly(boolean migrateStorageOnly) {
        this.migrateStorageOnly = migrateStorageOnly;
    }

    @APIParam(required = false)
    private boolean migrateStorageOnly = false;

    public static APIGetPrimaryStorageCandidatesForVmMigrationMsg __example__() {
        APIGetPrimaryStorageCandidatesForVmMigrationMsg msg = new APIGetPrimaryStorageCandidatesForVmMigrationMsg();
        msg.setVmInstanceUuid(uuid());
        msg.setWithDataVolumes(true);
        return msg;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public boolean isWithDataVolumes() {
        return withDataVolumes;
    }

    public void setWithDataVolumes(boolean withDataVolumes) {
        this.withDataVolumes = withDataVolumes;
    }
}
