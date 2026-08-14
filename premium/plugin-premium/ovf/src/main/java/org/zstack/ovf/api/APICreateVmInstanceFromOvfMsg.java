package org.zstack.ovf.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.ovf.datatype.CreateVmFromOvfImageParam;

import java.util.List;

/**
 * Created by Wenhao.Zhang on 22/03/08
 */
@TagResourceType(VmInstanceVO.class)
@RestRequest(
        path = "/ovf/create-vm-instance",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateVmInstanceFromOvfEvent.class
)
public class APICreateVmInstanceFromOvfMsg extends APICreateMessage implements APIAuditor {
    @APIParam
    private String xmlBase64;
    @APIParam
    private String jsonImageInfos;
    @APIParam(resourceType = BackupStorageVO.class)
    private String backupStorageUuid;
    @APIParam
    private String jsonCreateVmParam;

    @APIParam(required = false)
    private boolean deleteImageAfterSuccess = true;
    @APIParam(required = false)
    private boolean deleteImageOnFail = false;

    @APINoSee
    private List<CreateVmFromOvfImageParam> imageInfos;
    @APINoSee
    private APICreateVmInstanceMsg createVmMsg;

    public String getXmlBase64() {
        return xmlBase64;
    }

    public void setXmlBase64(String xmlBase64) {
        this.xmlBase64 = xmlBase64;
    }

    public String getJsonImageInfos() {
        return jsonImageInfos;
    }

    public void setJsonImageInfos(String jsonImageInfos) {
        this.jsonImageInfos = jsonImageInfos;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getJsonCreateVmParam() {
        return jsonCreateVmParam;
    }

    public void setJsonCreateVmParam(String jsonCreateVmParam) {
        this.jsonCreateVmParam = jsonCreateVmParam;
    }

    public boolean isDeleteImageAfterSuccess() {
        return deleteImageAfterSuccess;
    }

    public void setDeleteImageAfterSuccess(boolean deleteImageAfterSuccess) {
        this.deleteImageAfterSuccess = deleteImageAfterSuccess;
    }

    public boolean isDeleteImageOnFail() {
        return deleteImageOnFail;
    }

    public void setDeleteImageOnFail(boolean deleteImageOnFail) {
        this.deleteImageOnFail = deleteImageOnFail;
    }

    public List<CreateVmFromOvfImageParam> getImageInfos() {
        return imageInfos;
    }

    public void setImageInfos(List<CreateVmFromOvfImageParam> imageInfos) {
        this.imageInfos = imageInfos;
    }

    public APICreateVmInstanceMsg getCreateVmMsg() {
        return createVmMsg;
    }

    public void setCreateVmMsg(APICreateVmInstanceMsg createVmMsg) {
        this.createVmMsg = createVmMsg;
    }

    public static APICreateVmInstanceFromOvfMsg __example__() {
        APICreateVmInstanceFromOvfMsg msg = new APICreateVmInstanceFromOvfMsg();

        msg.setXmlBase64("PD94bWwgdmVyc2lvbj0nMS4wJyBlbmNvZGluZz0nVVRGLTgnPz4KPEVudmVsb3BlIHhtbG5zPSJodHRwOi8vc2NoZW1hcy5kbXR");
        msg.setJsonImageInfos("[{\"ovfId\":\"vm-1-system.vmdk\",\"type\":\"Download\",\"installPath\":\"http://xxx/vm-1-system.vmdk\"}]");
        msg.setBackupStorageUuid(uuid());
        msg.setJsonCreateVmParam("{\"name\":\"a\",\"clusterUuid\":\"" + uuid() + "\",\"l3NetworkUuids\":[\"" + uuid() + "\"]}");

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        APICreateVmInstanceFromOvfEvent event = (APICreateVmInstanceFromOvfEvent) rsp;
        return new APIAuditor.Result(rsp.isSuccess() ? event.getInventory().getUuid() : "", VmInstanceVO.class);
    }
}
