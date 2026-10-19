package org.zstack.header.storage.primary.allocate;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.I18nMessage;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.utils.CollectionUtils.transform;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractPrimaryStorageAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(AbstractPrimaryStorageAllocatorFlow.class);
    protected List<PrimaryStorageCandidate> candidates;
    protected PrimaryStorageAllocationSpec spec;
    private PrimaryStorageAllocatorTrigger trigger;

    public abstract void allocate();

    public List<PrimaryStorageCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<PrimaryStorageCandidate> candidates) {
        this.candidates = candidates;
    }

    public PrimaryStorageAllocationSpec getSpec() {
        return spec;
    }

    public void setSpec(PrimaryStorageAllocationSpec spec) {
        this.spec = spec;
    }

    public void setTrigger(PrimaryStorageAllocatorTrigger trigger) {
        this.trigger = trigger;
    }

    protected void next() {
        trigger.next();
    }

    /**
     * Stop allocating now. Filtering a candidate out is {@link #reject} plus {@link #next()}.
     */
    protected void fail(ErrorCode reason) {
        throw new OperationFailureException(reason);
    }

    protected void recommend(PrimaryStorageCandidate candidate) {
        candidate.markAsRecommended(getClass().getSimpleName());
        logger.debug(String.format("%s recommend primary storage[%s]", getClass().getSimpleName(), candidate.getUuid()));
    }

    protected void notRecommend(PrimaryStorageCandidate candidate) {
        candidate.markAsNotRecommended(getClass().getSimpleName());
        logger.debug(String.format("%s does not recommend primary storage[%s]", getClass().getSimpleName(), candidate.getUuid()));
    }

    protected void reject(PrimaryStorageCandidate candidate, String reason) {
        candidate.markAsRejected(getClass().getSimpleName(), reason);
        logger.debug(String.format("%s reject primary storage[%s]: %s", candidate.rejectBy, candidate.getUuid(), candidate.reject));
    }

    protected void reject(PrimaryStorageCandidate candidate, I18nMessage reason) {
        candidate.markAsRejected(getClass().getSimpleName(), reason);
        logger.debug(String.format("%s reject primary storage[%s]: %s", candidate.rejectBy, candidate.getUuid(), candidate.reject));
    }

    protected void rejectAll(String reason) {
        candidates.forEach(c -> reject(c, reason));
    }

    protected void rejectAll(I18nMessage reason) {
        candidates.forEach(c -> reject(c, reason));
    }

    protected List<String> allPrimaryStorageUuidList() {
        return transform(candidates, PrimaryStorageCandidate::getUuid);
    }
}
