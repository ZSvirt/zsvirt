package org.zstack.core.webhook;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.db.UpdateQuery;
import org.zstack.header.AbstractService;
import org.zstack.header.core.webhooks.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

/**
 * Created by xing5 on 2017/5/7.
 */
public class WebhookManagerImpl extends AbstractService implements WebhookManager {

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
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
        if (msg instanceof APICreateWebhookMsg) {
            handle((APICreateWebhookMsg) msg);
        } else if (msg instanceof APIDeleteWebhookMsg) {
            handle((APIDeleteWebhookMsg) msg);
        } else if (msg instanceof APIUpdateWebhookMsg) {
            handle((APIUpdateWebhookMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateWebhookMsg msg) {
        WebhookVO vo = new SQLBatchWithReturn<WebhookVO>() {
            @Override
            protected WebhookVO scripts() {
                UpdateQuery q = sql(WebhookVO.class);
                q.eq(WebhookVO_.uuid, msg.getUuid());

                boolean update = false;
                if (msg.getName() != null) {
                    q.set(WebhookVO_.name, msg.getName());
                    update = true;
                }
                if (msg.getDescription() != null) {
                    q.set(WebhookVO_.description, msg.getDescription());
                    update = true;
                }
                if (msg.getUrl() != null) {
                    q.set(WebhookVO_.url, msg.getUrl());
                    update = true;
                }
                if (msg.getType() != null) {
                    q.set(WebhookVO_.type, msg.getType());
                    update = true;
                }
                if (msg.getOpaque() != null) {
                    q.set(WebhookVO_.opaque, msg.getOpaque());
                    update = true;
                }

                if (update) {
                    q.update();
                }

                return findByUuid(msg.getUuid(), WebhookVO.class);
            }
        }.execute();

        APIUpdateWebhookEvent evt = new APIUpdateWebhookEvent(msg.getId());
        evt.setInventory(WebhookInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIDeleteWebhookMsg msg) {
        APIDeleteWebhookEvent evt = new APIDeleteWebhookEvent(msg.getId());
        SQL.New(WebhookVO.class).eq(WebhookVO_.uuid, msg.getUuid()).hardDelete();
        bus.publish(evt);
    }

    private void handle(APICreateWebhookMsg msg) {
        WebhookVO vo = new WebhookVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setUrl(msg.getUrl());
        vo.setType(msg.getType());
        vo.setOpaque(msg.getOpaque());
        vo = dbf.persistAndRefresh(vo);

        APICreateWebhookEvent evt = new APICreateWebhookEvent(msg.getId());
        evt.setInventory(WebhookInventory.valueOf(vo));
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(WebhookConstants.SERVICE_ID);
    }
}
