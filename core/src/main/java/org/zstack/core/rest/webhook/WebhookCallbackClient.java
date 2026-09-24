package org.zstack.core.rest.webhook;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.thread.ThreadFacadeImpl;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * Tracks pending webhook callbacks and completes them on callback, timeout, or request failure.
 *
 * <p>This class is created by the plugin-specific callback registry and configured
 * through Spring's AspectJ injection.</p>
 *
 * <p>Thread safety: {@code ConcurrentHashMap.remove()} serves as the CAS point.
 * Among callback arrival, timeout, and send-failure, only one can successfully remove
 * and thus complete a pending call.</p>
 *
 * @param <T> the callback body type
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class WebhookCallbackClient<T> {
    private static final CLogger logger = Utils.getLogger(WebhookCallbackClient.class);

    private final WebhookProtocol<T> protocol;
    @Autowired
    private ThreadFacade thdf;
    private final ConcurrentHashMap<String, PendingEntry<T>> pendingCalls = new ConcurrentHashMap<>();

    private static class PendingEntry<T> {
        final ReturnValueCompletion<T> completion;
        volatile ThreadFacadeImpl.TimeoutTaskReceipt timeoutReceipt;

        PendingEntry(ReturnValueCompletion<T> completion) {
            this.completion = completion;
        }

        void cancelTimeout() {
            ThreadFacadeImpl.TimeoutTaskReceipt receipt = timeoutReceipt;
            if (receipt != null) {
                receipt.cancel();
            }
        }
    }

    public WebhookCallbackClient(WebhookProtocol<T> protocol) {
        this.protocol = protocol;
    }

    /**
     * Register a pending call and return its task identifier.
     *
     * <p>The caller sends the request and invokes {@link #fail} if submission fails.</p>
     *
     * @param completion the completion to invoke when the callback arrives (or on timeout)
     * @param unit       timeout time unit
     * @param timeout    timeout value
     * @return the generated taskId
     */
    public String submit(ReturnValueCompletion<T> completion, TimeUnit unit, long timeout) {
        return submit(Platform.getUuid(), completion, unit, timeout);
    }

    public String submit(String taskId, ReturnValueCompletion<T> completion,
                         TimeUnit unit, long timeout) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("taskId cannot be empty");
        }

        PendingEntry<T> entry = new PendingEntry<>(completion);
        PendingEntry<T> previous = pendingCalls.putIfAbsent(taskId, entry);
        if (previous != null) {
            throw new IllegalStateException(String.format(
                    "webhook taskId[%s] is already pending for path[%s]",
                    taskId, protocol.getCallbackPath()));
        }

        try {
            entry.timeoutReceipt = thdf.submitTimeoutTask(() -> fail(taskId, entry,
                    operr("[Webhook Timeout] callback timed out for taskId[%s], path[%s]",
                            taskId, protocol.getCallbackPath())), unit, timeout);
        } catch (RuntimeException e) {
            pendingCalls.remove(taskId, entry);
            throw e;
        }
        if (pendingCalls.get(taskId) != entry) {
            entry.cancelTimeout();
        }
        return taskId;
    }

    private void fail(String taskId, PendingEntry<T> expected, ErrorCode error) {
        if (pendingCalls.remove(taskId, expected)) {
            expected.cancelTimeout();
            expected.completion.fail(error);
        }
    }

    /**
     * Actively fail a pending call (e.g. when the HTTP send fails).
     *
     * <p>{@code ConcurrentHashMap.remove()} is atomic — only one of
     * (callback / timeout / send-failure) can win, preventing double invocation.</p>
     */
    public void fail(String taskId, ErrorCode error) {
        PendingEntry<T> entry = pendingCalls.remove(taskId);
        if (entry != null) {
            entry.cancelTimeout();
            entry.completion.fail(error);
        }
    }

    /**
     * Deliver a callback received by the plugin's HTTP endpoint.
     */
    public void deliverCallback(T cmd) {
        onCallback(cmd);
    }

    private void onCallback(T cmd) {
        String taskId = protocol.extractTaskId(cmd);
        if (taskId == null) {
            logger.warn(String.format("received webhook callback without taskId on path[%s], ignoring",
                    protocol.getCallbackPath()));
            return;
        }

        PendingEntry<T> entry = pendingCalls.remove(taskId);
        if (entry == null) {
            logger.warn(String.format("received webhook callback for unknown taskId[%s] on path[%s], ignoring",
                    taskId, protocol.getCallbackPath()));
            return;
        }

        entry.cancelTimeout();

        if (protocol.isSuccess(cmd)) {
            entry.completion.success(cmd);
        } else {
            String error = protocol.extractError(cmd);
            entry.completion.fail(operr(
                    "webhook callback failed for taskId[%s], path[%s], error: %s",
                    taskId, protocol.getCallbackPath(), error != null ? error : "unknown"));
        }
    }
}
