package org.zstack.zwatch.thirdparty.xsky;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.rest.RESTFacade;
import org.zstack.zwatch.thirdparty.Constants;
import org.zstack.zwatch.thirdparty.GetAlertsRsp;
import org.zstack.zwatch.thirdparty.ThirdpartyPlatformBase;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyOriginalAlertVO;
import org.zstack.zwatch.thirdparty.ThirdpartyPlatformSystemTags;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO_;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by yaoning.li on 2020/7/15.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class XSKYPlatformBase extends ThirdpartyPlatformBase implements Component {
    @Autowired
    private RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;

    public XSKYPlatformBase() {
    }

    public XSKYPlatformBase(ThirdpartyPlatformVO vo) {
        super(vo);
    }

    @Override
    public void syncThirdpartyAlerts(Date start, Date end, Completion completion) {
        SyncAlertsModeFactory factory = new SyncAlertsModeFactory(self);

        String baseUrl = self.getUrl();
        if (!baseUrl.contains("?")) {
            baseUrl = baseUrl + "?";
        }

        AlertFetcher fetcher = factory.createInstance(baseUrl, start, end);
        fetcher.syncAlerts(baseUrl, start, end);

        completion.success();
    }

    @Override
    protected List<ThirdpartyOriginalAlertVO> convert(GetAlertsRsp rsp) {
        GetXSKYAlertsBaseRsp xskyAlertsRsp = (GetXSKYAlertsBaseRsp) rsp;

        List<ThirdpartyOriginalAlertVO> alertVOS = new ArrayList<>();
        if (xskyAlertsRsp.getAlerts() == null) {
            return alertVOS;
        }

        for (Map alertMap : xskyAlertsRsp.getAlerts()) {
            ThirdpartyOriginalAlertVO alertVO = render(alertMap);

            Long id = (Long) alertMap.get("id");
            if (id != null) {
                String uuidKey = String.format("%s-%s", self.getUuid(), id.toString());
                alertVO.setUuid(Platform.getUuidFromBytes(uuidKey.getBytes()));
            }
            alertVOS.add(alertVO);
        }

        return alertVOS;
    }

    @Override
    public boolean start() {
        List<String> uuids = Q.New(ThirdpartyPlatformVO.class)
                .select(ThirdpartyPlatformVO_.uuid)
                .eq(ThirdpartyPlatformVO_.type, Constants.XSKY)
                .listValues();

        uuids.forEach(uuid->
                ThirdpartyPlatformSystemTags.SYNC_ALERTS_SUPPORT_PAGING.delete(uuid)
                );

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public Class<? extends GetAlertsRsp> getXSKYAlertsRspClass() {
        if (self.getUrl().contains("alerts")){
            return GetXSKYAlertsRsp.class;
        } else {
            return GetXSKYAlertInfosRsp.class;
        }
    }

}
