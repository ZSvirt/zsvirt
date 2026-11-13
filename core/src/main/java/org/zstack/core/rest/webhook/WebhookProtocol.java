package org.zstack.core.rest.webhook;

/**
 * Defines the protocol adaptation for an external system's webhook callback mechanism.
 *
 * <p>Different external controllers use different callback body formats and
 * success/failure semantics. This interface abstracts those
 * differences so that {@link WebhookCallbackClient} can handle the common async lifecycle
 * (pending call registration, timeout, CAS-guarded callback dispatch) generically.</p>
 *
 * @param <T> the callback body type that the external system POSTs back
 */
public interface WebhookProtocol<T> {

    /**
     * The path of the endpoint that receives callbacks (e.g. "/zns/callback").
     */
    String getCallbackPath();

    /**
     * Extract the task identifier from the callback body.
     * This must match the taskId returned by {@link WebhookCallbackClient#submit}.
     */
    String extractTaskId(T callback);

    /**
     * Determine whether the callback indicates a successful operation.
     */
    boolean isSuccess(T callback);

    /**
     * Extract a human-readable error description from the callback body.
     * Called only when {@link #isSuccess} returns false.
     */
    String extractError(T callback);
}
