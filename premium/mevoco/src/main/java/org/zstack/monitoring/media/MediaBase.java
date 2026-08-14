package org.zstack.monitoring.media;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

/**
 * Created by xing5 on 2017/6/11.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MediaBase implements Media {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private MediaManager mmgr;

    private MediaVO self;

    public MediaBase(MediaVO self) {
        this.self = self;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeMediaStateMsg) {
            handle((APIChangeMediaStateMsg) msg);
        } else if (msg instanceof APIDeleteMediaMsg) {
            handle((APIDeleteMediaMsg) msg);
        } else if (msg instanceof APIUpdateEmailMediaMsg) {
            handle((APIUpdateEmailMediaMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateEmailMediaMsg msg) {
        EmailMediaVO vo = (EmailMediaVO) self;
        if (msg.getName() != null) {
            vo.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }
        if (msg.getPassword() != null) {
            vo.setPassword(msg.getPassword());
        }
        if (msg.getUsername() != null) {
            vo.setUsername(msg.getUsername());
        }
        if (msg.getSmtpServer() != null) {
            vo.setSmtpServer(msg.getSmtpServer());
        }
        if (msg.getSmtpPort() != null) {
            vo.setSmtpPort(msg.getSmtpPort());
        }

        vo = dbf.updateAndRefresh(vo);
        APIUpdateEmailMediaEvent evt = new APIUpdateEmailMediaEvent(msg.getId());
        evt.setInventory(EmailMediaInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIDeleteMediaMsg msg) {
        dbf.remove(self);

        APIDeleteMediaEvent evt = new APIDeleteMediaEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIChangeMediaStateMsg msg) {
        MediaStateEvent s = MediaStateEvent.valueOf(msg.getStateEvent());
        if (s == MediaStateEvent.enable) {
            self.setState(MediaState.Enabled);
        } else {
            self.setState(MediaState.Disabled);
        }

        dbf.update(self);

        APIChangeMediaStateEvent evt = new APIChangeMediaStateEvent(msg.getId());

        MediaFactory f = mmgr.getMediaFactory(self.getType());
        evt.setInventory(f.getMediaInventory(self.getUuid()));
        bus.publish(evt);
    }
}
