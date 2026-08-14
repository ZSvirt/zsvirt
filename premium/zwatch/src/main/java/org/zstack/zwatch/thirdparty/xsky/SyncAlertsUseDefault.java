package org.zstack.zwatch.thirdparty.xsky;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.rest.RESTFacade;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.thirdparty.GetAlertsRsp;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SyncAlertsUseDefault extends XSKYPlatformBase implements AlertFetcher {
    @Autowired
    private RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;

    public SyncAlertsUseDefault(ThirdpartyPlatformVO vo) {
        super(vo);
    }

    @Override
    public void syncAlerts(String baseUrl, Date start, Date end) {
        long durationMillis = TimeUnit.SECONDS.toMillis(ZWatchGlobalConfig.THIRDPARTY_ALERT_QUERY_DURATION.value(Long.class));
        long startMillis = start.getTime();
        long endMillis = end.getTime();

        for (long i = startMillis; i < endMillis; i = i + durationMillis) {
            Date s = new Date(i);
            Date e = new Date(Math.min(i + durationMillis, endMillis));

            List<String> urlParams = new ArrayList<>();
            urlParams.add(String.format("duration_begin=%s", s.toInstant().toString()));
            urlParams.add(String.format("duration_end=%s", e.toInstant().toString()));
            // xsky not support(ZSTAC-28651)
            // urlParams.add("limit=100"); urlParams.add("offset=0");
            String url = String.format("%s&%s", baseUrl, String.join("&", urlParams));

            GetXSKYAlertsBaseRsp rsp = (GetXSKYAlertsBaseRsp)restf.syncJsonGet(url, null, null, this.getXSKYAlertsRspClass(), TimeUnit.SECONDS, 10);
            persistAndFireAlerts(rsp);
        }
    }
}

