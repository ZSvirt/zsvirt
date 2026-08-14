package org.zstack.crypto.keyprovider.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIRekeyKeyProviderRefsEvent extends APIEvent {
    private int totalCount;
    private int successCount;
    private int skippedCount;
    private int failedCount;
    private List<RekeyProviderResult> providerResults = new ArrayList<>();

    public APIRekeyKeyProviderRefsEvent() {
        super(null);
    }

    public APIRekeyKeyProviderRefsEvent(String apiId) {
        super(apiId);
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<RekeyProviderResult> getProviderResults() {
        return providerResults;
    }

    public void setProviderResults(List<RekeyProviderResult> providerResults) {
        this.providerResults = providerResults == null ? new ArrayList<>() : providerResults;
    }

    public static APIRekeyKeyProviderRefsEvent __example__() {
        APIRekeyKeyProviderRefsEvent event = new APIRekeyKeyProviderRefsEvent();
        event.setTotalCount(3);
        event.setSuccessCount(1);
        event.setSkippedCount(1);
        event.setFailedCount(1);
        event.getProviderResults().add(RekeyProviderResult.__example__());
        return event;
    }
}
