package org.zstack.ovf.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.ovf.datatype.ImagePackageVO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Qi Le on 2022/4/26
 */
@RestRequest(
        path = "/ovf/ova-packages",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIExportVmOvaPackageEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APIExportVmOvaPackageMsg extends APICreateMessage implements APIMultiAuditor {
    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmUuid;

    @APIParam(resourceType = BackupStorageVO.class)
    private String backupStorageUuid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public static APIExportVmOvaPackageMsg __example__() {
        APIExportVmOvaPackageMsg msg = new APIExportVmOvaPackageMsg();
        msg.setBackupStorageUuid(uuid(BackupStorageVO.class));
        msg.setVmUuid(uuid(VmInstanceVO.class));
        msg.setName("ova");
        msg.setDescription("description");
        return msg;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        List<APIAuditor.Result> results = new ArrayList<>();
        results.add(new APIAuditor.Result(
                rsp.isSuccess() ? ((APIExportVmOvaPackageEvent) rsp).getInventory().getUuid() : "",
                ImagePackageVO.class
        ));
        results.add(new APIAuditor.Result(((APIExportVmOvaPackageMsg) msg).getVmUuid(), VmInstanceVO.class));
        return results;
    }
}
