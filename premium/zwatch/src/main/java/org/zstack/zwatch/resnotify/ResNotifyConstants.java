package org.zstack.zwatch.resnotify;



public interface ResNotifyConstants {
    String SERVICE_ID = "zwatch.resNotify";

    /**
     * Error code prefix used by ResNotify subscription validation.
     * Follows the zwatch convention: {@code ORG_ZSTACK_ZWATCH_<MODULE>_<NNNNN>}.
     */
    String ERR_CODE_PREFIX = "ORG_ZSTACK_ZWATCH_RESNOTIFY";

    /**
     * Raised when an attempt is made to subscribe a webhook URL that is
     * already in use (global uniqueness). Callers should use the
     * UpdateResNotifySubscription API to mutate the existing record
     * instead, otherwise stale subscriptions pile up and exhaust the
     * outbound HTTP connection pool.
     */
    String ERR_DUPLICATE_WEBHOOK_URL = ERR_CODE_PREFIX + "_10001";

    /**
     * Raised when an attempt is made to create or update a subscription
     * using a name that already exists (global uniqueness).
     */
    String ERR_DUPLICATE_NAME = ERR_CODE_PREFIX + "_10002";
}
