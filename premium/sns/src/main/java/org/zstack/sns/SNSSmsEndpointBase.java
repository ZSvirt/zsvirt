package org.zstack.sns;

import org.zstack.core.Platform;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

/**
 * Created by Qi Le on 2019-07-10
 */
public class SNSSmsEndpointBase extends SNSApplicationEndpointBase implements SNSSmsEndpoint {

    public SNSSmsEndpointBase() {
    }

    public SNSSmsEndpointBase(SNSSmsEndpointVO self) {
        this.self = self;
    }

    protected void deleteHook() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(SNSSmsReceiverVO.class).eq(SNSSmsReceiverVO_.endpointUuid, self.getUuid()).hardDelete();
            }
        }.execute();
    }

    protected SNSSmsEndpointVO getSelf() {
        return (SNSSmsEndpointVO) self;
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddSNSSmsReceiverMsg) {
            handle((APIAddSNSSmsReceiverMsg) msg);
        } else if (msg instanceof APIRemoveSNSSmsReceiverMsg) {
            handle((APIRemoveSNSSmsReceiverMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIRemoveSNSSmsReceiverMsg msg) {
        SQL.New(SNSSmsReceiverVO.class).eq(SNSSmsReceiverVO_.endpointUuid, msg.getEndpointUuid())
                .eq(SNSSmsReceiverVO_.phoneNumber, msg.getPhoneNumber()).hardDelete();
        APIRemoveSNSSmsReceiverEvent event = new APIRemoveSNSSmsReceiverEvent(msg.getId());
        bus.publish(event);
    }

    private void handle(APIAddSNSSmsReceiverMsg msg) {
        SNSSmsReceiverVO vo = new SNSSmsReceiverVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setPhoneNumber(msg.getPhoneNumber());
        vo.setEndpointUuid(msg.getEndpointUuid());
        vo.setType(SmsReceiverType.valueOf(msg.getType()));
        vo.setDescription(msg.getDescription());
        dbf.persist(vo);

        APIAddSNSSmsReceiverEvent event = new APIAddSNSSmsReceiverEvent(msg.getId());
        event.setInventory(SNSSmsReceiverInventory.valueOf(vo));
        bus.publish(event);
    }
}
