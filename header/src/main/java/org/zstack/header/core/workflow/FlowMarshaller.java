package org.zstack.header.core.workflow;

import java.util.Map;

/**
 * Created by frank on 7/3/2015.
 */
public interface FlowMarshaller {
    Flow marshalTheNextFlow(String previousFlowClassName, String nextFlowClassName, FlowChain chain, Map data);
}
