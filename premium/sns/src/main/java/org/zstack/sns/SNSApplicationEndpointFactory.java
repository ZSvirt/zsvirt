package org.zstack.sns;

import java.util.List;

public interface SNSApplicationEndpointFactory {
    SNSApplicationEndpointVO createApplicationEndpoint(SNSApplicationEndpointVO vo, APICreateSNSApplicationEndpointMsg msg);

    String getApplicationEndpointType();

    SNSApplicationEndpointInventory getSNSApplicationEndpointInventory(SNSApplicationEndpointVO vo);

    SNSApplicationEndpoint getSNSApplicationEndpoint(String uuid);

    SNSApplicationEndpoint getSNSApplicationEndpoint();

    List<SNSApplicationEndpoint> getSNSApplicationEndpoints(List<String> uuids);
}
