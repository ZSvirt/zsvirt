package org.zstack.header.core.external.service;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 1:47 AM
 */
@RestResponse
public class APIDeleteExternalServiceConfigurationEvent extends APIEvent {
    private List<ApplyExternalConfigurationResult> applyResults;

    public APIDeleteExternalServiceConfigurationEvent() {}

    public APIDeleteExternalServiceConfigurationEvent(String apiId) { super(apiId); }

    public List<ApplyExternalConfigurationResult> getApplyResults() {
        return applyResults;
    }

    public void setApplyResults(List<ApplyExternalConfigurationResult> applyResults) {
        this.applyResults = applyResults;
    }

    public static APIDeleteExternalServiceConfigurationEvent __example__() {
        APIDeleteExternalServiceConfigurationEvent event = new APIDeleteExternalServiceConfigurationEvent();
        event.setSuccess(true);
        return event;
    }
}
