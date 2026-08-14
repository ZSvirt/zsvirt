package org.zstack.zwatch.thirdparty.xsky;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.rest.RESTFacade;
import org.zstack.tag.SystemTagCreator;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.thirdparty.GetAlertsRsp;
import org.zstack.zwatch.thirdparty.ThirdpartyPlatformSystemTags;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SyncAlertsModeFactory extends XSKYPlatformBase{
    @Autowired
    private RESTFacade restf;

    public SyncAlertsModeFactory(ThirdpartyPlatformVO vo) {
        super(vo);
    }

    public AlertFetcher createInstance(String baseUrl, Date start, Date end) {
        boolean isSupport;
        boolean hasTag = ThirdpartyPlatformSystemTags.SYNC_ALERTS_SUPPORT_PAGING.hasTag(self.getUuid());

        if (!hasTag) {
            isSupport = isPagingSupported(baseUrl, start, end);
            setSyncTagIsSupportPaging(isSupport);
        }else{
            isSupport = Boolean.parseBoolean(ThirdpartyPlatformSystemTags.SYNC_ALERTS_SUPPORT_PAGING.getTokenByResourceUuid(self.getUuid(), ThirdpartyPlatformSystemTags.SYNC_ALERTS_SUPPORT_PAGING_TOKEN));
        }

        return isSupport ? new SyncAlertsUsePagination(self) : new SyncAlertsUseDefault(self);

    }

    private boolean isPagingSupported(String baseUrl, Date start, Date end) {
        long startMillis = start.getTime();
        long endMillis = end.getTime();
        long limitNum = ZWatchGlobalConfig.THIRDPARTY_ALERT_QUERY_PAGINATION_LIMIT.value(Long.class);

        Date s = new Date(startMillis);
        Date e = new Date(endMillis);

        List<String> urlParamsList = new ArrayList<>();
        urlParamsList.add(String.format("duration_begin=%s", s.toInstant().toString()));
        urlParamsList.add(String.format("duration_end=%s", e.toInstant().toString()));
        urlParamsList.add(String.format("limit=%s", limitNum));

        String url = String.format("%s&%s", baseUrl, String.join("&", urlParamsList));
        GetXSKYAlertsBaseRsp rsp = (GetXSKYAlertsBaseRsp)restf.syncJsonGet(url, null, null, this.getXSKYAlertsRspClass(), TimeUnit.SECONDS, 10);

        Paging paging = rsp.getPaging();
        long limit = paging.getLimit();

        return limit == -1;
    }

    private void setSyncTagIsSupportPaging(boolean isSupport) {
        SystemTagCreator creator = ThirdpartyPlatformSystemTags.SYNC_ALERTS_SUPPORT_PAGING.newSystemTagCreator(self.getUuid());
        creator.setTagByTokens(map(e(ThirdpartyPlatformSystemTags.SYNC_ALERTS_SUPPORT_PAGING_TOKEN, isSupport)));
        creator.inherent = false;
        creator.recreate = true;
        creator.create();
    }
}

