package org.zstack.header.candidate;

import org.zstack.header.rest.SDK;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SDK
public class CandidateResult<T> implements Serializable {
    private T candidate;
    private CandidateDecision finalDecision = CandidateDecision.ACCEPTED;
    private List<CandidateDecisionEntry> decisions = new ArrayList<>();

    public CandidateResult(T candidate) {
        this.candidate = candidate;
    }

    public T getCandidate() {
        return candidate;
    }

    public void setCandidate(T candidate) {
        this.candidate = candidate;
    }

    public CandidateDecision getFinalDecision() {
        return finalDecision;
    }

    public void setFinalDecision(CandidateDecision finalDecision) {
        this.finalDecision = finalDecision;
    }

    public List<CandidateDecisionEntry> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<CandidateDecisionEntry> decisions) {
        this.decisions = decisions;
    }

    public void addFinalDecision(CandidateDecisionEntry entry) {
        if (this.decisions == null) {
            this.decisions = new ArrayList<>();
        }
        if (entry == null) {
            return;
        }
        this.decisions.add(entry);
        this.finalDecision = entry.getDecision();
    }

    public static <T> List<CandidateResult<T>> getNotRejectedCandidates(List<CandidateResult<T>> candidates) {
        if (candidates == null) {
            return new ArrayList<>();
        }
        return candidates.stream()
                .filter(c -> c.getFinalDecision() != CandidateDecision.REJECTED)
                .collect(Collectors.toList());
    }
}
