package org.zstack.scheduler;

import com.google.common.primitives.Ints;
import org.zstack.header.message.APIReply;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetSchedulerExecutionReportReply extends APIReply {
    @NoJsonSchema
    private List<Integer> successRecords;
    @NoJsonSchema
    private List<Integer> failureRecords;
    @NoJsonSchema
    private List<Integer> partialSuccessRecords;
    @NoJsonSchema
    private List<Integer> waitingRecords;

    public List<Integer> getSuccessRecords() {
        return successRecords;
    }

    public void setSuccessRecords(List<Integer> successRecords) {
        this.successRecords = successRecords;
    }

    public List<Integer> getFailureRecords() {
        return failureRecords;
    }

    public void setFailureRecords(List<Integer> failureRecords) {
        this.failureRecords = failureRecords;
    }

    public List<Integer> getPartialSuccessRecords() {
        return partialSuccessRecords;
    }

    public void setPartialSuccessRecords(List<Integer> partialSuccessRecords) {
        this.partialSuccessRecords = partialSuccessRecords;
    }

    public List<Integer> getWaitingRecords() {
        return waitingRecords;
    }

    public void setWaitingRecords(List<Integer> waitingRecords) {
        this.waitingRecords = waitingRecords;
    }

    public void loadFromReport(SchedulerExecutionReport report) {
        this.successRecords = Ints.asList(report.getSuccessRecords());
        this.failureRecords = Ints.asList(report.getFailureRecords());
        this.partialSuccessRecords = Ints.asList(report.getPartialSuccessRecords());
        this.waitingRecords = Ints.asList(report.getWaitingRecords());
    }

    public static APIGetSchedulerExecutionReportReply __example__() {
        APIGetSchedulerExecutionReportReply reply = new APIGetSchedulerExecutionReportReply();
        reply.successRecords = Arrays.asList(100, 99, 50, 0);
        reply.failureRecords = Arrays.asList(0, 1, 34, 0);
        reply.partialSuccessRecords = Arrays.asList(0, 0, 16, 0);
        reply.waitingRecords = Arrays.asList(0, 0, 0, 100);
        return reply;
    }
}
