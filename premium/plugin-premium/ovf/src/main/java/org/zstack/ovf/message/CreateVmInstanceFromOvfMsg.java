package org.zstack.ovf.message;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.ovf.OvfHelper;
import org.zstack.ovf.api.APICreateVmInstanceFromOvfMsg;
import org.zstack.ovf.datatype.CreateVmFromOvfImageParam;
import org.zstack.utils.gson.JSONObjectUtil;

import com.google.gson.JsonSyntaxException;

import java.util.Collections;
import java.util.List;

import static org.zstack.core.Platform.argerr;

/**
 * Created by Wenhao.Zhang on 22/03/09
 */
public class CreateVmInstanceFromOvfMsg extends NeedReplyMessage {
    private String xmlBase64;
    private List<CreateVmFromOvfImageParam> imageInfos = Collections.emptyList();
    private Boolean ovfDiskEncryption;
    private String backupStorageUuid;
    private APICreateVmInstanceMsg createVmMsg;

    private boolean deleteImageAfterSuccess = false;
    private boolean deleteImageOnFail = true;

    private Boolean vmEncryption;

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

    public Boolean getOvfDiskEncryption() {
        return ovfDiskEncryption;
    }

    public void setOvfDiskEncryption(Boolean ovfDiskEncryption) {
        this.ovfDiskEncryption = ovfDiskEncryption;
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

    public Boolean getVmEncryption() {
        return vmEncryption;
    }

    public void setVmEncryption(Boolean vmEncryption) {
        this.vmEncryption = vmEncryption;
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
        resolveCreateVmMsg(apiMessage);
        resolveOvfDiskEncryption(apiMessage);

        CreateVmInstanceFromOvfMsg message = new CreateVmInstanceFromOvfMsg();
        message.setResourceUuid(apiMessage.getResourceUuid());
        message.setCreateVmMsg(apiMessage.getCreateVmMsg());
        message.setOvfDiskEncryption(apiMessage.getOvfDiskEncryption());
        message.setBackupStorageUuid(apiMessage.getBackupStorageUuid());
        message.setXmlBase64(apiMessage.getXmlBase64());
        message.setImageInfos(apiMessage.getImageInfos());
        message.setDeleteImageAfterSuccess(apiMessage.isDeleteImageAfterSuccess());
        message.setDeleteImageOnFail(apiMessage.isDeleteImageOnFail());
        message.setVmEncryption(apiMessage.getVmEncryption());
        // create VM instance need account UUID which from session
        message.getCreateVmMsg().setSession(apiMessage.getSession());
        return message;
    }

    public static void resolveCreateVmMsg(APICreateVmInstanceFromOvfMsg apiMessage) {
        if (apiMessage.getCreateVmMsg() != null) {
            if (apiMessage.getVmEncryption() != null) {
                apiMessage.getCreateVmMsg().setVmEncryption(apiMessage.getVmEncryption());
            }
            return;
        }
        if (apiMessage.getJsonCreateVmParam() == null) {
            return;
        }
        // Fallback for direct fromApiMessage callers, such as OVF long-job resume paths.
        try {
            apiMessage.setCreateVmMsg(JSONObjectUtil.toObject(apiMessage.getJsonCreateVmParam(), APICreateVmInstanceMsg.class));
        } catch (JsonSyntaxException e) {
            throw new ApiMessageInterceptionException(argerr(
                    "failed to parse jsonCreateVmParam in APICreateVmInstanceFromOvfMsg"));
        }
        if (apiMessage.getVmEncryption() != null) {
            apiMessage.getCreateVmMsg().setVmEncryption(apiMessage.getVmEncryption());
        }
    }

    public static void resolveOvfDiskEncryption(APICreateVmInstanceFromOvfMsg apiMessage) {
        if (apiMessage.getOvfDiskEncryption() == null) {
            try {
                apiMessage.setOvfDiskEncryption(OvfHelper.parseCreateVmFromOvfDiskEncryption(apiMessage.getJsonCreateVmParam()));
            } catch (OperationFailureException e) {
                throw new ApiMessageInterceptionException(e.getErrorCode());
            }
        }
    }
}
