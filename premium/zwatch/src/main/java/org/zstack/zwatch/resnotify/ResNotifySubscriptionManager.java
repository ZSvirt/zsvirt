package org.zstack.zwatch.resnotify;

import org.apache.commons.lang.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.SQLIntegrityConstraintViolationException;

public class ResNotifySubscriptionManager extends AbstractService {
    private static final CLogger logger = Utils.getLogger(ResNotifySubscriptionManager.class);

    @Autowired
    private CloudBus bus;

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ResNotifyConstants.SERVICE_ID);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APISubscribeResNotifyMsg) {
            handle((APISubscribeResNotifyMsg) msg);
        } else if (msg instanceof APIUpdateResNotifySubscriptionMsg) {
            handle((APIUpdateResNotifySubscriptionMsg) msg);
        } else if (msg instanceof APIDeleteResNotifySubscriptionMsg) {
            handle((APIDeleteResNotifySubscriptionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APISubscribeResNotifyMsg msg) {
        APISubscribeResNotifyEvent evt = new APISubscribeResNotifyEvent(msg.getId());

        ResNotifySubscriptionVO vo = new ResNotifySubscriptionVO();
        vo.setUuid(msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid());
        vo.setName(StringUtils.isNotEmpty(msg.getName()) ? msg.getName()
                : "resnotify-" + vo.getUuid().substring(0, 8));
        vo.setDescription(msg.getDescription());
        vo.setType(ResNotifyType.valueOf(msg.getType()));
        vo.setState(ResNotifySubscriptionState.Enabled);

        if (msg.getResourceTypes() != null && !msg.getResourceTypes().isEmpty()) {
            vo.setResourceTypes(String.join(",", msg.getResourceTypes()));
        }
        if (msg.getEventTypes() != null && !msg.getEventTypes().isEmpty()) {
            vo.setEventTypes(String.join(",", msg.getEventTypes()));
        }

        vo.setAccountUuid(msg.getSession().getAccountUuid());

        ResNotifyWebhookRefInventory webhookRefInv = null;
        try {
            vo = dbf.persistAndRefresh(vo);
            if (vo.getType() == ResNotifyType.WEBHOOK) {
                ResNotifyWebhookRefVO webhookRef = new ResNotifyWebhookRefVO();
                webhookRef.setUuid(vo.getUuid());
                webhookRef.setWebhookUrl(msg.getWebhookUrl());
                webhookRef.setSecret(msg.getSecret());
                webhookRef.setCustomHeaders(msg.getCustomHeaders());
                dbf.persist(webhookRef);
                webhookRefInv = ResNotifyWebhookRefInventory.valueOf(webhookRef);
            }
        } catch (RuntimeException e) {
            if (!isDuplicateConstraint(e)) {
                throw e;
            }

            cleanupHalfCreatedSubscription(vo.getUuid());
            evt.setError(toDuplicateError(e, msg.getName(), msg.getWebhookUrl()));
            bus.publish(evt);
            return;
        }

        ResNotifySubscriptionInventory inv = ResNotifySubscriptionInventory.valueOf(vo);
        inv.setWebhookRef(webhookRefInv);
        evt.setInventory(inv);

        logger.info(String.format("Created resource notify subscription[uuid:%s, name:%s, type:%s]",
                vo.getUuid(), vo.getName(), vo.getType()));

        bus.publish(evt);
    }

    private void handle(APIUpdateResNotifySubscriptionMsg msg) {
        APIUpdateResNotifySubscriptionEvent evt = new APIUpdateResNotifySubscriptionEvent(msg.getId());

        ResNotifySubscriptionVO vo = dbf.findByUuid(msg.getUuid(), ResNotifySubscriptionVO.class);
        if (vo == null) {
            evt.setError(Platform.operr("Subscription[uuid:%s] not found", msg.getUuid()));
            bus.publish(evt);
            return;
        }
        if (StringUtils.isNotEmpty(msg.getName())) {
            vo.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }
        if (msg.getResourceTypes() != null) {
            vo.setResourceTypes(msg.getResourceTypes().isEmpty() ? null : String.join(",", msg.getResourceTypes()));
        }
        if (msg.getEventTypes() != null) {
            vo.setEventTypes(msg.getEventTypes().isEmpty() ? null : String.join(",", msg.getEventTypes()));
        }
        if (StringUtils.isNotEmpty(msg.getState())) {
            vo.setState(ResNotifySubscriptionState.valueOf(msg.getState()));
        }

        try {
            vo = dbf.updateAndRefresh(vo);
        } catch (RuntimeException e) {
            if (!isDuplicateConstraint(e)) {
                throw e;
            }
            evt.setError(toDuplicateError(e, msg.getName(), msg.getWebhookUrl()));
            bus.publish(evt);
            return;
        }
        ResNotifyWebhookRefInventory webhookRefInv = null;
        if (vo.getType() == ResNotifyType.WEBHOOK) {
            ResNotifyWebhookRefVO webhookRef = dbf.findByUuid(vo.getUuid(), ResNotifyWebhookRefVO.class);
            if (webhookRef != null) {
                if (StringUtils.isNotEmpty(msg.getWebhookUrl())) {
                    webhookRef.setWebhookUrl(msg.getWebhookUrl());
                }
                if (msg.getSecret() != null) {
                    webhookRef.setSecret(msg.getSecret());
                }
                if (msg.getCustomHeaders() != null) {
                    webhookRef.setCustomHeaders(msg.getCustomHeaders());
                }
                try {
                    webhookRef = dbf.updateAndRefresh(webhookRef);
                } catch (RuntimeException e) {
                    if (!isDuplicateConstraint(e)) {
                        throw e;
                    }
                    evt.setError(toDuplicateError(e, msg.getName(), msg.getWebhookUrl()));
                    bus.publish(evt);
                    return;
                }
                webhookRefInv = ResNotifyWebhookRefInventory.valueOf(webhookRef);
            }
        }

        ResNotifySubscriptionInventory inv = ResNotifySubscriptionInventory.valueOf(vo);
        inv.setWebhookRef(webhookRefInv);
        evt.setInventory(inv);

        logger.info(String.format("Updated resource notify subscription[uuid:%s]", vo.getUuid()));

        bus.publish(evt);
    }

    private boolean isDuplicateConstraint(Throwable t) {
        Throwable current = t;
        while (current != null) {
            if (current instanceof DataIntegrityViolationException
                    || current instanceof ConstraintViolationException
                    || current instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("duplicate")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ErrorCode toDuplicateError(Throwable t, String name, String webhookUrl) {
        String message = t.getMessage() == null ? "" : t.getMessage();
        String lowerMessage = message.toLowerCase();
        if (StringUtils.isNotEmpty(name)
                && (lowerMessage.contains("uk_resnotifysubscriptionvo_name")
                || lowerMessage.contains("resnotifysubscriptionvo")
                || lowerMessage.contains("name"))) {
            return new ErrorCode(
                    ResNotifyConstants.ERR_DUPLICATE_NAME,
                    String.format("subscription with name[%s] already exists."
                            + " Use UpdateResNotifySubscription to mutate the existing record instead",
                            name));
        }

        if (StringUtils.isNotEmpty(webhookUrl)
                && (lowerMessage.contains("uk_resnotifywebhookrefvo_webhookurl")
                || lowerMessage.contains("resnotifywebhookrefvo")
                || lowerMessage.contains("webhookurl"))) {
            return new ErrorCode(
                    ResNotifyConstants.ERR_DUPLICATE_WEBHOOK_URL,
                    String.format("subscription with webhookUrl[%s] already exists."
                            + " Use UpdateResNotifySubscription to mutate the existing record instead of"
                            + " creating duplicates (see ZCF-4095)",
                            webhookUrl));
        }

        return Platform.operr("resource notify subscription conflicts with an existing record."
                + " Use UpdateResNotifySubscription to mutate the existing record instead");
    }

    private void cleanupHalfCreatedSubscription(String uuid) {
        try {
            SQL.New(ResNotifyWebhookRefVO.class).eq(ResNotifyWebhookRefVO_.uuid, uuid).hardDelete();
            SQL.New(ResNotifySubscriptionVO.class).eq(ResNotifySubscriptionVO_.uuid, uuid).hardDelete();
        } catch (Throwable t) {
            logger.warn(String.format("failed to cleanup half-created ResNotify subscription[uuid:%s]: %s",
                    uuid, t.getMessage()), t);
        }
    }

    private void handle(APIDeleteResNotifySubscriptionMsg msg) {
        APIDeleteResNotifySubscriptionEvent evt = new APIDeleteResNotifySubscriptionEvent(msg.getId());

        ResNotifySubscriptionVO vo = dbf.findByUuid(msg.getUuid(), ResNotifySubscriptionVO.class);
        if (vo != null) {
            if (vo.getType() == ResNotifyType.WEBHOOK) {
                // Hard delete to avoid stale soft-deleted webhook refs blocking re-subscribe with the same uuid.
                SQL.New(ResNotifyWebhookRefVO.class).eq(ResNotifyWebhookRefVO_.uuid, vo.getUuid()).hardDelete();
            }
            dbf.remove(vo);
            logger.info(String.format("Deleted resource notify subscription[uuid:%s]", msg.getUuid()));
        } else {
            int deleted = SQL.New(ResNotifyWebhookRefVO.class).eq(ResNotifyWebhookRefVO_.uuid, msg.getUuid()).hardDelete();
            if (deleted > 0) {
                logger.warn(String.format("Deleted orphan resource notify webhook ref[uuid:%s]", msg.getUuid()));
            }
        }

        bus.publish(evt);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
