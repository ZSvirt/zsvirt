package org.zstack.compute.vm;

import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

public class VmMevocoComplementApiInterceptor implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(VmMevocoComplementApiInterceptor.class);

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(APICreateVmInstanceFromTemplatedVmInstanceMsg.class);
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVmInstanceFromTemplatedVmInstanceMsg) {
            validate((APICreateVmInstanceFromTemplatedVmInstanceMsg) msg);
        }

        return msg;
    }

    private void validate(APICreateVmInstanceFromTemplatedVmInstanceMsg msg) {
        VmInstanceVO templatedVmInstance = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getTemplatedVmInstanceUuid()).find();
        if (templatedVmInstance == null) {
            throw new ApiMessageInterceptionException(argerr("the templated vmInstance[uuid:%s] is not exist", msg.getTemplatedVmInstanceUuid()));
        }
        msg.setTemplatedVmInstance(templatedVmInstance);

        msg.setPlatform(templatedVmInstance.getPlatform());
    }
}
