package org.zstack.network.service.userdata;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/4/15.
 */
@GlobalPropertyDefinition
public class UserdataGlobalProperty {
    @GlobalProperty(name="Userdata.hostPort", defaultValue = "1180")
    public static int HOST_PORT;
}
