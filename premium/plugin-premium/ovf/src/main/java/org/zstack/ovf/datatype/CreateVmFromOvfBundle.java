package org.zstack.ovf.datatype;

import org.apache.logging.log4j.ThreadContext;
import org.zstack.compute.vm.VmInstanceUtils;
import org.zstack.header.Constants;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageArchitecture;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.ovf.OvfHelper;
import org.zstack.ovf.message.CreateVmInstanceFromOvfMsg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static org.zstack.core.Platform.err;
import static org.zstack.ovf.datatype.OvfErrors.*;

/**
 * Created by Wenhao.Zhang on 22/03/09
 */
public class CreateVmFromOvfBundle {
    private NeedReplyMessage message;
    private SessionInventory session;

    /**
     * maybe null
     */
    private String currentApiId;

    private OvfInfo ovfInfo;
    private boolean deleteImageAfterSuccess;
    private boolean deleteImageOnFail;
    private List<CreateVmFromOvfImageParam> params;
    private boolean hasDataVolumes;
    /**
     * Key: ovfId (in OVF template)
     * value: CreateVmFromOvfImageParam (also in field 'params' list)
     */
    private final Map<String, CreateVmFromOvfImageParam> diskParamMap = new HashMap<>();

    private String backupStorageUuid;
    private String vmInstanceUuid;
    private CreateVmInstanceMsg innerMessage;
    private VmInstanceInventory vmInventory;

    /**
     * @see CreateVmFromOvfBundle#builder()
     */
    private CreateVmFromOvfBundle() {
    }

    public NeedReplyMessage getMessage() {
        return message;
    }

    public void setMessage(NeedReplyMessage message) {
        this.message = message;
    }

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

    /**
     * no set methods
     */
    public String getCurrentApiId() {
        return currentApiId;
    }

    public OvfInfo getOvfInfo() {
        return ovfInfo;
    }

    public void setOvfInfo(OvfInfo ovfInfo) {
        this.ovfInfo = ovfInfo;
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

    public List<CreateVmFromOvfImageParam> getParams() {
        return params;
    }

    public void setParams(List<CreateVmFromOvfImageParam> params) {
        this.params = params;
    }

    public boolean isHasDataVolumes() {
        return hasDataVolumes;
    }

    public void setHasDataVolumes(boolean hasDataVolumes) {
        this.hasDataVolumes = hasDataVolumes;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public CreateVmInstanceMsg getInnerMessage() {
        return innerMessage;
    }

    public void setInnerMessage(CreateVmInstanceMsg innerMessage) {
        this.innerMessage = innerMessage;
    }

    public VmInstanceInventory getVmInventory() {
        return vmInventory;
    }

    public void setVmInventory(VmInstanceInventory vmInventory) {
        this.vmInventory = vmInventory;
    }

    public Map<String, CreateVmFromOvfImageParam> getDiskParamMap() {
        return diskParamMap;
    }

    public static Factory builder() {
        return new Factory();
    }

    public static class Factory {
        private CreateVmInstanceFromOvfMsg message;
        private OvfInfo ovfInfo;
        private Consumer<APICreateVmInstanceMsg> apiValidator = ignored -> {};

        private Factory() {}
        public Factory withCreateMessage(CreateVmInstanceFromOvfMsg message) {
            this.message = Objects.requireNonNull(message, "CreateVmInstanceFromOvfMsg can not be null");
            return this;
        }
        public Factory withOvfInfo(OvfInfo ovfInfo) {
            this.ovfInfo = Objects.requireNonNull(ovfInfo, "OvfInfo can not be null");
            return this;
        }
        public Factory withApiCreateVmMessageValidator(Consumer<APICreateVmInstanceMsg> validator) {
            this.apiValidator = Objects.requireNonNull(validator, "APICreateVmInstanceMsgValidator can not be null");
            return this;
        }
        public ErrorableValue<CreateVmFromOvfBundle> build() {
            if (message == null) {
                return ErrorableValue.ofErrorCode(err(INVALID_OVF_XML, "message can not be null"));
            }
            if (ovfInfo == null)
                return ErrorableValue.ofErrorCode(err(INVALID_OVF_XML, "ovfInfo can not be null"));

            CreateVmFromOvfBundle bundle = new CreateVmFromOvfBundle();
            bundle.setMessage(message);
            bundle.setOvfInfo(ovfInfo);
            bundle.setDeleteImageAfterSuccess(message.isDeleteImageAfterSuccess());
            bundle.setDeleteImageOnFail(message.isDeleteImageOnFail());
            bundle.setParams(message.getImageInfos());
            bundle.setBackupStorageUuid(message.getBackupStorageUuid());
            bundle.setVmInstanceUuid(message.getResourceUuid());
            bundle.currentApiId = ThreadContext.get(Constants.THREAD_CONTEXT_API);

            APICreateVmInstanceMsg apiMsg = message.getCreateVmMsg();
            bundle.setSession(apiMsg.getSession());

            if (apiMsg.getCpuNum() == null) {
                apiMsg.setCpuNum(ovfInfo.getCpu().getQuantity());
            }
            if (apiMsg.getMemorySize() == null) {
                apiMsg.setMemorySize(ovfInfo.getMemory().getQuantity());
            }
            // imageUuid of apiMsg is must be null: image should be uploaded soon.
            if (apiMsg.getPlatform() == null) {
                apiMsg.setPlatform(ovfInfo.getPreAnalysisInfo().getInferredPlatform().toString());
            }
            if (apiMsg.getGuestOsType() == null) {
                apiMsg.setGuestOsType(OvfHelper.getGuestOsTypeFromOVF(ovfInfo));
            }
            if (apiMsg.getArchitecture() == null) {
                apiMsg.setArchitecture(
                        ovfInfo.getPreAnalysisInfo().getInferredArchitectureOrDefault(ImageArchitecture.x86_64)
                                .toString());
            }
            if (apiMsg.getRootDiskSize() == null) {
                final Long inferredRootDiskSize = ovfInfo.getPreAnalysisInfo().getInferredRootDiskSize();
                if (inferredRootDiskSize == null) {
                    return ErrorableValue.ofErrorCode(
                            err(INVALID_OVF_XML, "failed to create ovf bundle: Neither the OVF file nor the custom API " +
                            "has set the size of the root disk, so unable to allocate root disk. " +
                            "You should set root disk size in CreateVmInstanceFromOvfAction.jsonCreateVmParam.rootDiskSize"));
                }
                apiMsg.setRootDiskSize(inferredRootDiskSize);
            }

            try {
                apiValidator.accept(apiMsg);
            } catch (ApiMessageInterceptionException e) {
                return ErrorableValue.ofErrorCode(
                        err(INVALID_OVF_XML, "failed to create ovf bundle")
                        .withCause(e.getError()));
            } catch (RuntimeException e) {
                return ErrorableValue.ofErrorCode(
                        err(INVALID_OVF_XML, "failed to validate ovf bundle")
                        .withOpaque("api.message", apiMsg)
                        .withOpaque("exception.class", e.getClass().getSimpleName())
                        .withOpaque("exception", e.getMessage()));
            }

            bundle.setInnerMessage(VmInstanceUtils.fromAPICreateVmInstanceMsg(apiMsg));
            return ErrorableValue.of(bundle);
        }
    }
}
