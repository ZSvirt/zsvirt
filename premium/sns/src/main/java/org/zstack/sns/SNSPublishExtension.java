package org.zstack.sns;

import java.util.Map;

/**
 * Created by yaoning.li on 2020/8/11.
 */
public interface SNSPublishExtension {
    void afterPublishToEndpoint(String endpointUuid, Map<String, String> message);
}
