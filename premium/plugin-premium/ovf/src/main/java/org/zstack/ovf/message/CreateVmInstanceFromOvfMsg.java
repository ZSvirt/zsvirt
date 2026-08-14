package org.zstack.ovf.message;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.ovf.api.APICreateVmInstanceFromOvfMsg;
import org.zstack.ovf.datatype.CreateVmFromOvfImageParam;

import java.util.Collections;
import java.util.List;

/**
 * Created by Wenhao.Zhang on 22/03/09
 */
public class CreateVmInstanceFromOvfMsg extends NeedReplyMessage {
    private String xmlBase64;
    private List<CreateVmFromOvfImageParam> imageInfos = Collections.emptyList();
    private String backupStorageUuid;
    private APICreateVmInstanceMsg createVmMsg;

    private boolean deleteImageAfterSuccess = false;
    private boolean deleteImageOnFail = true;

    private String resourceUuid;

    public String getXmlBase64() {
        return xmlBase64;
    }

    public void setXmlBase64(String xmlBase64) {
        this.xmlBase64 = xmlBase64;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public APICreateVmInstanceMsg getCreateVmMsg() {
        return createVmMsg;
    }

    public void setCreateVmMsg(APICreateVmInstanceMsg createVmMsg) {
        this.createVmMsg = createVmMsg;
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

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public static CreateVmInstanceFromOvfMsg fromApiMessage(APICreateVmInstanceFromOvfMsg apiMessage) {
        CreateVmInstanceFromOvfMsg message = new CreateVmInstanceFromOvfMsg();
        message.setResourceUuid(apiMessage.getResourceUuid());
        message.setCreateVmMsg(apiMessage.getCreateVmMsg());
        message.setBackupStorageUuid(apiMessage.getBackupStorageUuid());
        message.setXmlBase64(apiMessage.getXmlBase64());
        message.setImageInfos(apiMessage.getImageInfos());
        message.setDeleteImageAfterSuccess(apiMessage.isDeleteImageAfterSuccess());
        message.setDeleteImageOnFail(apiMessage.isDeleteImageOnFail());
        // create VM instance need account UUID which from session
        message.getCreateVmMsg().setSession(apiMessage.getSession());
        return message;
    }
}
