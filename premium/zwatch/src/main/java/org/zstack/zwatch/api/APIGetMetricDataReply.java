package org.zstack.zwatch.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.datatype.Datapoint;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "data")
public class APIGetMetricDataReply extends APIReply {
    private List<Datapoint> data;

    public static APIGetMetricDataReply __example__() {
        APIGetMetricDataReply reply = new APIGetMetricDataReply();
        reply.setData(asList(Datapoint.__example__()));
        return reply;
    }

    public List<Datapoint> getData() {
        return data;
    }

    public void setData(List<Datapoint> data) {
        this.data = data;
    }
}
