package org.zstack.abstraction.header.sns;

import org.zstack.abstraction.entity.sns.PluginEndpointData;
import org.zstack.abstraction.header.PluginDriver;

/**
 * {@link EndpointDriver} extends {@link PluginDriver} and contains sender
 * of sns endpoint.
 */
public interface EndpointDriver extends PluginDriver {
    boolean send(PluginEndpointData message);
}
