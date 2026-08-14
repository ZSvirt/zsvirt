package org.zstack.zwatch.thirdparty.xsky;


import java.util.List;
import java.util.Map;

public class GetXSKYAlertInfosRsp extends GetXSKYAlertsBaseRsp {
    private List<Map> alert_infos;

    @Override
    public List<Map> getAlerts() {
        return alert_infos;
    }

    public List<Map> getAlertInfos() {
        return alert_infos;
    }

    public void setAlertInfos(List<Map> alert_infos) {
        this.alert_infos = alert_infos;
    }


}
