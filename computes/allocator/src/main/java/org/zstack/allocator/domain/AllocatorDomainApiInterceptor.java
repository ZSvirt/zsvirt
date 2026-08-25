package org.zstack.allocator.domain;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.allocator.domain.api.APICreateAllocatorDomainMsg;
import org.zstack.allocator.domain.api.APIGetAllocatorDomainCandidatesMsg;
import org.zstack.allocator.domain.api.APIUpdateAllocatorDomainMsg;
import org.zstack.allocator.domain.api.AllocatorDomainMessage;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.allocator.domain.entity.AllocatorDomainVO_;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;

import static org.zstack.core.Platform.argerr;

@InterceptorForService(AllocatorDomainConstant.SERVICE_ID)
public class AllocatorDomainApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;

    private final AllocatorDomainValidator validator = new AllocatorDomainValidator();

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateAllocatorDomainMsg) {
            validate((APICreateAllocatorDomainMsg) msg);
        } else if (msg instanceof APIUpdateAllocatorDomainMsg) {
            validate((APIUpdateAllocatorDomainMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof APICreateAllocatorDomainMsg) {
            APICreateAllocatorDomainMsg cmsg = (APICreateAllocatorDomainMsg) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, AllocatorDomainConstant.SERVICE_ID, cmsg.getResourceUuid());
        } else if (msg instanceof AllocatorDomainMessage) {
            AllocatorDomainMessage dmsg = (AllocatorDomainMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, AllocatorDomainConstant.SERVICE_ID, dmsg.getAllocatorDomainUuid());
        } else if (msg instanceof APIGetAllocatorDomainCandidatesMsg) {
            bus.makeLocalServiceId(msg, AllocatorDomainConstant.SERVICE_ID);
        }
    }

    private void validate(APICreateAllocatorDomainMsg msg) {
        if (StringUtils.isEmpty(msg.getResourceUuid())) {
            msg.setResourceUuid(Platform.getUuid());
        } else {
            boolean exists = Q.New(AllocatorDomainVO.class)
                    .eq(AllocatorDomainVO_.uuid, msg.getResourceUuid())
                    .isExists();
            if (exists) {
                throw new ApiMessageInterceptionException(
                        argerr("AllocatorDomainVO[%s] is already exists", msg.getResourceUuid()));
            }
        }
        validator.validateCreate(msg);
    }

    private void validate(APIUpdateAllocatorDomainMsg msg) {
        validator.validateUpdate(msg);
    }
}
