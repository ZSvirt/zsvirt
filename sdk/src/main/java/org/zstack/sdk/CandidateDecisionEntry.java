package org.zstack.sdk;

import org.zstack.sdk.CandidateDecision;

public class CandidateDecisionEntry  {

    public CandidateDecision decision;
    public void setDecision(CandidateDecision decision) {
        this.decision = decision;
    }
    public CandidateDecision getDecision() {
        return this.decision;
    }

    public java.lang.String decisionMaker;
    public void setDecisionMaker(java.lang.String decisionMaker) {
        this.decisionMaker = decisionMaker;
    }
    public java.lang.String getDecisionMaker() {
        return this.decisionMaker;
    }

    public java.lang.String reason;
    public void setReason(java.lang.String reason) {
        this.reason = reason;
    }
    public java.lang.String getReason() {
        return this.reason;
    }

}
