package org.zstack.sshkeypair;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.sshkeypair.*;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;

import java.util.List;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

@InterceptorForService("sshKeyPair")
public class SshKeyPairAPIInterceptor implements ApiMessageInterceptor {

    @Autowired
    private CloudBus bus;

    private static final List<String> ALLOW_RESOURCE_TYPES = asList(VmInstanceVO.class.getSimpleName());

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateSshKeyPairMsg) {
            validate((APICreateSshKeyPairMsg) msg);
        } else if (msg instanceof APIDeleteSshKeyPairMsg) {
            validate((APIDeleteSshKeyPairMsg) msg);
        } else if (msg instanceof APIAttachSshKeyPairToVmInstanceMsg) {
            validate((APIAttachSshKeyPairToVmInstanceMsg) msg);
        } else if (msg instanceof APIDetachSshKeyPairFromVmInstanceMsg) {
            validate((APIDetachSshKeyPairFromVmInstanceMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof SshKeyPairMessage) {
            SshKeyPairMessage smsg = (SshKeyPairMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, SshKeyPairManager.SERVICE_ID, smsg.getSshKeyPairUuid());
        }
    }

    private void validate(APICreateSshKeyPairMsg msg) {
        List<String> sshKeyPairUuids = Q.New(SshKeyPairVO.class)
                .select(SshKeyPairVO_.uuid)
                .eq(SshKeyPairVO_.publicKey, msg.getPublicKey())
                .listValues();
        if (sshKeyPairUuids.isEmpty()) {
            return;
        }
        boolean isExist = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.accountUuid, msg.getSession().getAccountUuid())
                .eq(AccountResourceRefVO_.resourceType, SshKeyPairVO.class.getSimpleName())
                .in(AccountResourceRefVO_.resourceUuid, sshKeyPairUuids)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .isExists();
        if (isExist) {
            throw new ApiMessageInterceptionException(argerr("The sshKeyPair already upload"));
        }
    }

    private void validate(APIDeleteSshKeyPairMsg msg) {
        boolean exists = Q.New(SshKeyPairRefVO.class)
                .eq(SshKeyPairRefVO_.sshKeyPairUuid, msg.getUuid())
                .isExists();
        if (exists) {
            throw new ApiMessageInterceptionException((argerr("The sshKeyPair[uuid:%s] was in using.", msg.getSshKeyPairUuid())));
        }
    }

    private void validate(APIAttachSshKeyPairToVmInstanceMsg msg) {
        String resourceType = Q.New(ResourceVO.class)
                .eq(ResourceVO_.uuid, msg.getVmInstanceUuid())
                .select(ResourceVO_.resourceType)
                .findValue();
        checkType(resourceType);

        boolean isExist = Q.New(SshKeyPairRefVO.class)
                .eq(SshKeyPairRefVO_.sshKeyPairUuid, msg.getSshKeyPairUuid())
                .eq(SshKeyPairRefVO_.resourceUuid, msg.getVmInstanceUuid())
                .eq(SshKeyPairRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .isExists();
        if (isExist) {
            throw new ApiMessageInterceptionException((argerr("The sshKeyPair[uuid:%s] was already attached on vm[uuid:].",
                    msg.getSshKeyPairUuid(), msg.getVmInstanceUuid())));
        }
        validateVmInstanceRunning(Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find());
    }

    private void validate(APIDetachSshKeyPairFromVmInstanceMsg msg) {
        String resourceType = Q.New(ResourceVO.class)
                .eq(ResourceVO_.uuid, msg.getVmInstanceUuid())
                .select(ResourceVO_.resourceType)
                .findValue();
        checkType(resourceType);

        boolean isExist = Q.New(SshKeyPairRefVO.class)
                .eq(SshKeyPairRefVO_.sshKeyPairUuid, msg.getSshKeyPairUuid())
                .eq(SshKeyPairRefVO_.resourceUuid, msg.getVmInstanceUuid())
                .eq(SshKeyPairRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .isExists();
        if (!isExist) {
            throw new ApiMessageInterceptionException((argerr("The sshKeyPair[uuid:%s] was not attached on vm[uuid:%s].",
                    msg.getSshKeyPairUuid(), msg.getVmInstanceUuid())));
        }
        validateVmInstanceRunning(Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find());
    }

    private void validateVmInstanceRunning(VmInstanceVO vmInstanceVO) {
        if (!vmInstanceVO.getState().equals(VmInstanceState.Running)) {
            throw new ApiMessageInterceptionException((argerr(
                    "The vmInstance[uuid:%s] not in running state.", vmInstanceVO.getUuid())));
        }
    }

    private void checkType(String type) {
        if (!ALLOW_RESOURCE_TYPES.contains(type)) {
            throw new ApiMessageInterceptionException(argerr("resource types %s are not supported to attach sshKeyPair, allowed types are %s", type, ALLOW_RESOURCE_TYPES));
        }
    }
}
