package org.zstack.ovf;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmInstanceHelper;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.*;
import org.zstack.ovf.api.APICreateVmInstanceFromOvfMsg;
import org.zstack.ovf.api.APIDeleteImagePackageMsg;
import org.zstack.ovf.api.APIExportVmOvaPackageMsg;
import org.zstack.ovf.api.APIUpdateImagePackageMsg;
import org.zstack.ovf.datatype.ImagePackageVO;
import org.zstack.ovf.datatype.ImagePackageVO_;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO_;
import org.zstack.utils.gson.JSONObjectUtil;

import static org.zstack.core.Platform.*;

/**
 * Created by Wenhao.Zhang on 22/03/09
 */
@InterceptorForService("ovf")
public class OvfInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVmInstanceFromOvfMsg) {
            validate((APICreateVmInstanceFromOvfMsg) msg);
        } else if (msg instanceof APIExportVmOvaPackageMsg) {
            validate((APIExportVmOvaPackageMsg) msg);
        } else if (msg instanceof APIDeleteImagePackageMsg) {
            validate((APIDeleteImagePackageMsg) msg);
        } else if (msg instanceof APIUpdateImagePackageMsg) {
            validate((APIUpdateImagePackageMsg) msg);
        }
        return msg;
    }

    private void validate(APIUpdateImagePackageMsg msg) {
        bus.makeTargetServiceIdByResourceUuid(msg, OvfManager.SERVICE_ID, msg.getUuid());
    }

    private void validate(APIDeleteImagePackageMsg msg) {
        bus.makeTargetServiceIdByResourceUuid(msg, OvfManager.SERVICE_ID, msg.getUuid());
    }

    private void validate(APIExportVmOvaPackageMsg msg) {
        String ovaUuid = Q.New(ImagePackageVO.class)
                .select(ImagePackageVO_.uuid)
                .eq(ImagePackageVO_.vmUuid, msg.getVmUuid())
                .findValue();
        if (ovaUuid != null) {
            throw new ApiMessageInterceptionException(
                    argerr("Vm[uuid: %s] is already exported as the ova package[uuid: %s], please delete the package and try again.",
                            msg.getVmUuid(), ovaUuid)
            );
        }
        if (!Q.New(ImageStoreBackupStorageVO.class)
                .eq(ImageStoreBackupStorageVO_.uuid, msg.getBackupStorageUuid())
                .isExists()) {
            throw new ApiMessageInterceptionException(
                    argerr("Export vm requires an ImageStore backup storage, but given backupStorageUuid: %s is not an ImageStore backup storage.",
                            msg.getBackupStorageUuid()));
        }
        VmInstanceVO vm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, msg.getVmUuid()).find();
        if (vm == null) {
            throw new ApiMessageInterceptionException(
                    argerr("Not found the vm to be exported with the uuid: %s", msg.getVmUuid())
            );
        }
        if (vm.getState() != VmInstanceState.Stopped) {
            throw new ApiMessageInterceptionException(
                    argerr("Only vm in state: %s can be exported.", VmInstanceState.Stopped.toString()));
        }

        if (StringUtils.isBlank(msg.getName())) {
            msg.setName(String.format("exported-vm-%s", vm.getName()));
        }
        if (StringUtils.isBlank(msg.getDescription())) {
            msg.setDescription(String.format("source vm uuid: %s", vm.getUuid()));
        }
        if (msg.getResourceUuid() == null) {
            msg.setResourceUuid(getUuid());
        }
        bus.makeTargetServiceIdByResourceUuid(msg, OvfManager.SERVICE_ID, msg.getResourceUuid());
    }

    private void validate(APICreateVmInstanceFromOvfMsg msg) throws ApiMessageInterceptionException {
        try {
            msg.setImageInfos(OvfHelper.parseCreateVmFromOvfParams(msg.getJsonImageInfos()));
        } catch (OperationFailureException e) {
            throw new ApiMessageInterceptionException(e.getErrorCode());
        }

        try {
            msg.setCreateVmMsg(JSONObjectUtil.toObject(msg.getJsonCreateVmParam(), APICreateVmInstanceMsg.class));
        } catch (JsonSyntaxException e) {
            throw new ApiMessageInterceptionException(argerr(
                    "failed to parse jsonCreateVmParam in APICreateVmInstanceFromOvfMsg"));
        }

        new VmInstanceHelper().validate((NewVmInstanceMessage) msg.getCreateVmMsg());

        if (msg.getResourceUuid() == null) {
            msg.setResourceUuid(getUuid());
        }
    }
}
