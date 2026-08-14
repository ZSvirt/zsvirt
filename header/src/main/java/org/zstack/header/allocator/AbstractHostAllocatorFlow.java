package org.zstack.header.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.I18nMessage;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.utils.CollectionUtils.transform;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractHostAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(AbstractHostAllocatorFlow.class);
    protected List<HostCandidate> candidates;
    protected HostAllocatorSpec spec;
    private HostAllocatorTrigger trigger;

    public abstract void allocate();

    public List<HostCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<HostCandidate> candidates) {
        this.candidates = candidates;
    }

    public HostAllocatorSpec getSpec() {
        return spec;
    }

    public void setSpec(HostAllocatorSpec spec) {
        this.spec = spec;
    }

    public void setTrigger(HostAllocatorTrigger trigger) {
        this.trigger = trigger;
    }

    protected void next() {
        trigger.next();
    }

    /**
     * Will stop pagination
     */
    protected void allocatorTriggerFail(ErrorCode errorCode) {
        trigger.fail(errorCode);
    }

    /**
     * Call fail() will stop pagination and stop allocating now.
     * If you want to start next pagination, use {@link #rejectAll(I18nMessage)} and {@link #next()}.
     */
    protected void fail(ErrorCode reason) {
        throw new OperationFailureException(reason);
    }

    protected void recommend(HostCandidate candidate) {
        candidate.markAsRecommended(getClass().getSimpleName());
        logger.debug(String.format("%s recommend host[%s]", getClass().getSimpleName(), candidate.getUuid()));
    }

    protected void notRecommend(HostCandidate candidate) {
        candidate.markAsNotRecommended(getClass().getSimpleName());
        logger.debug(String.format("%s does not recommend host[%s]", getClass().getSimpleName(), candidate.getUuid()));
    }

    protected void reject(HostCandidate candidate, String reason) {
        candidate.markAsRejected(getClass().getSimpleName(), reason);
        logger.debug(String.format("%s reject host[%s]: %s", candidate.rejectBy, candidate.getUuid(), candidate.reject));
    }

    protected void reject(HostCandidate candidate, I18nMessage reason) {
        candidate.markAsRejected(getClass().getSimpleName(), reason);
        logger.debug(String.format("%s reject host[%s]: %s", candidate.rejectBy, candidate.getUuid(), candidate.reject));
    }

    protected void rejectAll(String reason) {
        candidates.forEach(c -> reject(c, reason));
    }

    protected void rejectAll(I18nMessage reason) {
        candidates.forEach(c -> reject(c, reason));
    }

    protected List<String> allHostUuidList() {
        return transform(candidates, HostCandidate::getUuid);
    }
}
