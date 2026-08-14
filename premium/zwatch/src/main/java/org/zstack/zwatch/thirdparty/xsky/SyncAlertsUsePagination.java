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
public class SyncAlertsUsePagination extends XSKYPlatformBase implements AlertFetcher {
    @Autowired
    private RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;

    public SyncAlertsUsePagination(ThirdpartyPlatformVO vo) {
        super(vo);
    }

    @Override
    public void syncAlerts(String baseUrl, Date start, Date end) {
        long limitNum = ZWatchGlobalConfig.THIRDPARTY_ALERT_QUERY_PAGINATION_LIMIT.value(Long.class);
        long startMillis = start.getTime();
        long endMillis = end.getTime();
        long offset = 0;

        Date s = new Date(startMillis);
        Date e = new Date(endMillis);

        while (true) {
            List<String> urlParams = new ArrayList<>();
            urlParams.add(String.format("duration_begin=%s", s.toInstant().toString()));
            urlParams.add(String.format("duration_end=%s", e.toInstant().toString()));
            urlParams.add(String.format("duration_limit=%s", limitNum));
            urlParams.add(String.format("duration_offset=%s", offset));
            String url = String.format("%s&%s", baseUrl, String.join("&", urlParams));

            GetXSKYAlertsBaseRsp rsp = (GetXSKYAlertsBaseRsp)restf.syncJsonGet(url, null, null,this.getXSKYAlertsRspClass(), TimeUnit.SECONDS, 10);
            persistAndFireAlerts(rsp);
            if (rsp.getPaging().getCount() < rsp.getPaging().getLimit()) {
                break;
            }
            offset = offset + limitNum;
        }
    }
}

