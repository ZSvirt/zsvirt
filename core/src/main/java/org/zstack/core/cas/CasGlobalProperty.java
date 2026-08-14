package org.zstack.core.cas;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * @Author: DaoDao
 * @Date: 2022/8/26
 */
@GlobalPropertyDefinition
public class CasGlobalProperty {
    //The start of the CAS server URL
    @GlobalProperty(name="casServerLoginUrl")
    public static String casServerLoginUrl;
    //Defines the location of the CAS server login URL
    @GlobalProperty(name="casServerUrlPrefix")
    public static String casServerUrlPrefix;
    @GlobalProperty(name="serverName")
    public static String serverName;

}

