package org.zstack.zql;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.jmx.JmxFacade;
import org.zstack.header.Component;
import org.zstack.query.QueryGlobalConfig;

import javax.management.MXBean;

@MXBean
public class ZQLJMX implements ZQLMXBean, Component {
    @Autowired
    private JmxFacade jmxf;

    @Override
    public ZQLStatistic getZQLStatistic() {
        return ZQL.getZQLStatistic();
    }

    @Override
    public boolean start() {
        QueryGlobalConfig.ZQL_STATISTICS_ON.installUpdateExtension((oldConfig, newConfig) -> {
            // turn on zql statistics
            if (newConfig.value(Boolean.class)) {
                return;
            }

            ZQL.cleanStatisticData();
        });

        jmxf.registerBean("ZQL", this);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
