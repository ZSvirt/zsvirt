package org.zstack.sns;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

import java.util.List;

/**
 * Created by Qi Le on 2019-07-16
 */
public abstract class APIValidateSNSSmsEndpointMsg extends APIMessage implements SNSApplicationEndpointMessage {
    @APIParam(resourceType = SNSSmsEndpointVO.class)
    private String uuid;
    @APIParam
    private List<String> phoneNumbers;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public String getApplicationEndpointType() {
        return null;
    }

    @Override
    public String getApplicationEndpointUuid() {
        return uuid;
    }
}
