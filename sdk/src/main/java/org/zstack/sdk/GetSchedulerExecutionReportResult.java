package org.zstack.sdk;



public class GetSchedulerExecutionReportResult {
    public java.util.List successRecords;
    public void setSuccessRecords(java.util.List successRecords) {
        this.successRecords = successRecords;
    }
    public java.util.List getSuccessRecords() {
        return this.successRecords;
    }

    public java.util.List failureRecords;
    public void setFailureRecords(java.util.List failureRecords) {
        this.failureRecords = failureRecords;
    }
    public java.util.List getFailureRecords() {
        return this.failureRecords;
    }

    public java.util.List partialSuccessRecords;
    public void setPartialSuccessRecords(java.util.List partialSuccessRecords) {
        this.partialSuccessRecords = partialSuccessRecords;
    }
    public java.util.List getPartialSuccessRecords() {
        return this.partialSuccessRecords;
    }

    public java.util.List waitingRecords;
    public void setWaitingRecords(java.util.List waitingRecords) {
        this.waitingRecords = waitingRecords;
    }
    public java.util.List getWaitingRecords() {
        return this.waitingRecords;
    }

}
