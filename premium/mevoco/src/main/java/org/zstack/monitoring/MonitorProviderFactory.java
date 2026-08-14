package org.zstack.monitoring;

/**
 * Created by xing5 on 2017/6/3.
 */
public interface MonitorProviderFactory {
    String getMonitorProviderType();

    MonitorProvider createProvider();
}
