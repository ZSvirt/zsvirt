package org.zstack.storage.primary.sharedblock;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class SharedBlockGlobalProperty {
    @GlobalProperty(name="SharedBlock.agentPort", defaultValue = "7276")
    public static int AGENT_PORT;
}
