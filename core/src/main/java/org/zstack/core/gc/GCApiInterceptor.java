package org.zstack.core.gc;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

/**
 * Created by xing5 on 2017/3/5.
 */
@InterceptorForService("gc")
public class GCApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeleteGCJobMsg) {
            validate((APIDeleteGCJobMsg) msg);
        }

        if (msg instanceof GarbageCollectorMessage) {
            setServiceId((GarbageCollectorMessage) msg);
        }

        return msg;
    }

    private void validate(APIDeleteGCJobMsg msg) {
        if (!dbf.isExist(msg.getUuid(), GarbageCollectorVO.class)) {
            APIDeleteGCJobEvent evt = new APIDeleteGCJobEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void setServiceId(GarbageCollectorMessage msg) {
        String mgmtUuid = Q.New(GarbageCollectorVO.class).select(GarbageCollectorVO_.managementNodeUuid)
                .eq(GarbageCollectorVO_.uuid, msg.getGCJobUuid()).findValue();

        if (mgmtUuid != null) {
            bus.makeTargetServiceIdByResourceUuid((Message) msg, GCConstants.SERVICE_ID, mgmtUuid);
        } else {
            bus.makeLocalServiceId((Message) msg, GCConstants.SERVICE_ID);
        }
    }
}
