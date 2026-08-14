package org.zstack.storage.device.localRaid;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Create by weiwang at 2018/10/18
 */

@RestResponse(fieldsTo = {"all"})
public class APISelfTestLocalRaidEvent extends APIEvent {
    private String result;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public APISelfTestLocalRaidEvent() {
    }

    public APISelfTestLocalRaidEvent(String apiId) {
        super(apiId);
    }

    public static APISelfTestLocalRaidEvent __example__() {
        APISelfTestLocalRaidEvent evt = new APISelfTestLocalRaidEvent();
        return evt;
    }
}
