package org.zstack.zwatch.resnotify;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.Platform;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validator for resource-notify subscription APIs.
 *
 * <p>The primary reason this interceptor exists is to stop duplicate
 * subscriptions from piling up. ZCF issues APISubscribeResNotifyMsg on
 * every restart / cluster-mode switch / platform rebuild; without
 * deduplication the handler inserts a fresh row each time and the
 * outbound webhook delivery saturates the Apache HTTP connection pool,
 * which in turn makes downstream event delivery lag dramatically
 * (ZCF-4095).
 *
 * <p>ResNotify is a platform-level configuration (global webhook bus),
 * so deduplication is global -- not scoped by account:
 * <ul>
 *   <li>{@code webhookUrl} must be globally unique across all WEBHOOK subscriptions.</li>
 *   <li>{@code name} must be globally unique across all subscriptions.</li>
 * </ul>
 *
 * <p>Non-WEBHOOK types are intentionally skipped from URL-based
 * deduplication so that other delivery channels (e.g. queue / SNS)
 * do not accidentally collide on an empty webhook URL.
 */
@InterceptorForService(ResNotifyConstants.SERVICE_ID)
public class ResNotifySubscriptionApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(ResNotifySubscriptionApiInterceptor.class);
    private static final Set<String> SUPPORTED_EVENT_TYPES = new HashSet<>(Arrays.asList(
            ResNotifyService.EVENT_TYPE_CREATE,
            ResNotifyService.EVENT_TYPE_UPDATE,
            ResNotifyService.EVENT_TYPE_DELETE
    ));

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APISubscribeResNotifyMsg) {
            validate((APISubscribeResNotifyMsg) msg);
        } else if (msg instanceof APIUpdateResNotifySubscriptionMsg) {
            validate((APIUpdateResNotifySubscriptionMsg) msg);
        }
        return msg;
    }

    private void validate(APISubscribeResNotifyMsg msg) {
        String type = StringUtils.isNotEmpty(msg.getType()) ? msg.getType() : ResNotifyType.WEBHOOK.name();

        // Global name uniqueness (excludeUuid=null for new subscriptions).
        ErrorCode nameErr = checkDuplicateName(msg.getName(), null);
        if (nameErr != null) {
            throw new ApiMessageInterceptionException(nameErr);
        }

        validateEventTypesLength(msg.getEventTypes());

        // WEBHOOK-specific: global webhookUrl uniqueness.
        if (!ResNotifyType.WEBHOOK.name().equals(type)) {
            return;
        }
        if (StringUtils.isEmpty(msg.getWebhookUrl())) {
            return;
        }

        validateWebhookUrl(msg.getWebhookUrl());

        ErrorCode urlErr = checkDuplicateWebhookSubscription(msg.getWebhookUrl(), null);
        if (urlErr != null) {
            throw new ApiMessageInterceptionException(urlErr);
        }
    }

    private void validate(APIUpdateResNotifySubscriptionMsg msg) {
        // Global name uniqueness (exclude the subscription being updated).
        ErrorCode nameErr = checkDuplicateName(msg.getName(), msg.getUuid());
        if (nameErr != null) {
            throw new ApiMessageInterceptionException(nameErr);
        }

        validateEventTypesLength(msg.getEventTypes());

        // webhookUrl uniqueness only matters for WEBHOOK-type subscriptions.
        if (StringUtils.isEmpty(msg.getWebhookUrl())) {
            return;
        }

        ResNotifySubscriptionVO vo = dbf.findByUuid(msg.getUuid(), ResNotifySubscriptionVO.class);
        if (vo == null) {
            return;
        }
        if (vo.getType() != ResNotifyType.WEBHOOK) {
            return;
        }

        validateWebhookUrl(msg.getWebhookUrl());

        ErrorCode urlErr = checkDuplicateWebhookSubscription(msg.getWebhookUrl(), vo.getUuid());
        if (urlErr != null) {
            throw new ApiMessageInterceptionException(urlErr);
        }
    }

    private void validateEventTypesLength(List<String> eventTypes) {
        if (eventTypes == null || eventTypes.isEmpty()) {
            return;
        }

        String joined = String.join(",", eventTypes);
        if (joined.length() > 256) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "eventTypes is too long after joining by comma, max length is 256, got %s",
                    joined.length()));
        }

        for (String eventType : eventTypes) {
            if (!SUPPORTED_EVENT_TYPES.contains(eventType)) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "invalid eventType[%s], valid values are %s",
                        eventType, SUPPORTED_EVENT_TYPES));
            }
        }
    }

    private void validateWebhookUrl(String webhookUrl) {
        String error = ResNotifyWebhookUrlValidator.validate(webhookUrl);
        if (error != null) {
            throw new ApiMessageInterceptionException(Platform.argerr("%s", error));
        }
    }

    /**
     * Returns a populated ErrorCode if any subscription already uses
     * {@code name}; returns {@code null} if {@code name} is empty or
     * no collision is found.
     *
     * <p>{@code excludeUuid} (nullable) is used by update flows to skip
     * the row being updated.
     */
    private ErrorCode checkDuplicateName(String name, String excludeUuid) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        Q q = Q.New(ResNotifySubscriptionVO.class)
                .eq(ResNotifySubscriptionVO_.name, name);
        if (excludeUuid != null) {
            q = q.notEq(ResNotifySubscriptionVO_.uuid, excludeUuid);
        }
        if (!q.isExists()) {
            return null;
        }

        logger.warn(String.format("rejecting duplicate ResNotify subscription: name[%s]", name));
        return new ErrorCode(
                ResNotifyConstants.ERR_DUPLICATE_NAME,
                String.format("subscription with name[%s] already exists."
                        + " Use UpdateResNotifySubscription to mutate the existing record instead",
                        name));
    }

    /**
     * Returns a populated ErrorCode if any subscription already targets
     * {@code webhookUrl}; returns {@code null} otherwise.
     *
     * <p>{@code excludeUuid} (nullable) is used by update flows to skip
     * the row being updated.
     */
    private ErrorCode checkDuplicateWebhookSubscription(String webhookUrl, String excludeUuid) {
        Q q = Q.New(ResNotifyWebhookRefVO.class)
                .select(ResNotifyWebhookRefVO_.uuid)
                .eq(ResNotifyWebhookRefVO_.webhookUrl, webhookUrl);
        if (excludeUuid != null) {
            q = q.notEq(ResNotifyWebhookRefVO_.uuid, excludeUuid);
        }
        String collidingUuid = q.findValue();
        if (collidingUuid == null) {
            return null;
        }

        logger.warn(String.format("rejecting duplicate ResNotify webhook subscription:"
                + " webhookUrl[%s] collides with subscription[uuid:%s]",
                webhookUrl, collidingUuid));
        return new ErrorCode(
                ResNotifyConstants.ERR_DUPLICATE_WEBHOOK_URL,
                String.format("subscription with webhookUrl[%s] already exists as subscription[uuid:%s]."
                        + " Use UpdateResNotifySubscription to mutate the existing record instead of"
                        + " creating duplicates (see ZCF-4095)",
                        webhookUrl, collidingUuid));
    }
}
