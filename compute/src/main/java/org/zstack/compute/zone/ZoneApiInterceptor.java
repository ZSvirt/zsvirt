package org.zstack.compute.zone;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.zone.*;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:36 AM
 * To change this template use File | Settings | File Templates.
 */
@InterceptorForService("zone")
public class ZoneApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof ZoneMessage) {
            ZoneMessage zmsg = (ZoneMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, ZoneConstant.SERVICE_ID, zmsg.getZoneUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);

        if (msg instanceof APIDeleteZoneMsg) {
            validate((APIDeleteZoneMsg) msg);
        }

        return msg;
    }

    private void validate(APIDeleteZoneMsg msg) {
        if (!dbf.isExist(msg.getUuid(), ZoneVO.class)) {
            APIDeleteZoneEvent evt = new APIDeleteZoneEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }
}
