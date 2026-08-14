package org.zstack.zwatch.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetZWatchAlertHistogramReply extends APIReply {
    private List<Histogram> histograms;

    public List<Histogram> getHistograms() {
        return histograms;
    }

    public void setHistograms(List<Histogram> histograms) {
        this.histograms = histograms;
    }
}
