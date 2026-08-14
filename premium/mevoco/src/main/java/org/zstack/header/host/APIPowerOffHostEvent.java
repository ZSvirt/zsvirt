package org.zstack.header.host;

import org.zstack.core.Platform;
import org.zstack.header.cluster.PowerOffHardwareResult;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MaJin on 2019/6/28.
 */
@RestResponse(fieldsTo = "all")
public class APIPowerOffHostEvent extends APIEvent {
    @NoJsonSchema
    private List<PowerOffHardwareResult> results = new ArrayList<>();

    public APIPowerOffHostEvent(String apiId) {
        super(apiId);
    }

    public APIPowerOffHostEvent() {
        super();
    }

    public static APIPowerOffHostEvent __example__() {
        return new APIPowerOffHostEvent(Platform.getUuid());
    }

    public List<PowerOffHardwareResult> getResults() {
        return results;
    }

    public void setResults(List<PowerOffHardwareResult> results) {
        this.results = results;
    }
}
