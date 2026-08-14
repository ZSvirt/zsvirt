package org.zstack.core.log;

import org.zstack.core.cloudbus.CloudBusGlobalConfig;
import org.zstack.core.cloudbus.CloudBusGlobalProperty;

/**
 * Created by ZStack on 2020/9/28.
 */
public class LogUtils {
    public boolean isLogReadAPI() {
        long openReadAPILog = CloudBusGlobalConfig.OPEN_READ_API_LOG.value(Long.class);
        if (openReadAPILog != -1) {
            return openReadAPILog == 1;
        } else {
            return !CloudBusGlobalProperty.READ_API_LOG_OFF;
        }
    }
}
