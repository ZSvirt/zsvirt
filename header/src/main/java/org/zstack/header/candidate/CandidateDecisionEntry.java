package org.zstack.header.candidate;

import java.io.Serializable;

public class CandidateDecisionEntry implements Serializable {
    private CandidateDecision decision;
    private String decisionMaker;
    private String reason;

    public CandidateDecision getDecision() {
        return decision;
    }

    public void setDecision(CandidateDecision decision) {
        this.decision = decision;
    }

    public String getDecisionMaker() {
        return decisionMaker;
    }

    public void setDecisionMaker(String decisionMaker) {
        this.decisionMaker = decisionMaker;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
