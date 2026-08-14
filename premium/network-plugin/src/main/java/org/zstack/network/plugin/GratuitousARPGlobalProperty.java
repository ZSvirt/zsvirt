package org.zstack.network.plugin;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by shixin.ruan on 2021/08/09.
 */
@GlobalPropertyDefinition
public class GratuitousARPGlobalProperty {
    @GlobalProperty(name="SendGratuitousARP", defaultValue = "false")
    public static boolean SEND_GRATUITOUS_ARP;

}
