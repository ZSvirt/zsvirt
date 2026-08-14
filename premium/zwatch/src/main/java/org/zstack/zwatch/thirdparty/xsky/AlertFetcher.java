package org.zstack.zwatch.thirdparty.xsky;

import java.util.Date;

public interface AlertFetcher {
    void syncAlerts(String url, Date start, Date end);
}

