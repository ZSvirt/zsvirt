package org.zstack.sdk;

import org.zstack.sdk.CandidateDecision;

public class CandidateResult  {

    public java.lang.Object candidate;
    public void setCandidate(java.lang.Object candidate) {
        this.candidate = candidate;
    }
    public java.lang.Object getCandidate() {
        return this.candidate;
    }

    public CandidateDecision finalDecision;
    public void setFinalDecision(CandidateDecision finalDecision) {
        this.finalDecision = finalDecision;
    }
    public CandidateDecision getFinalDecision() {
        return this.finalDecision;
    }

    public java.util.List decisions;
    public void setDecisions(java.util.List decisions) {
        this.decisions = decisions;
    }
    public java.util.List getDecisions() {
        return this.decisions;
    }

}
