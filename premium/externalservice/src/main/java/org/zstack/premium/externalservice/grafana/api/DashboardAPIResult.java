package org.zstack.premium.externalservice.grafana.api;

/**
 * Created by mingjian.deng on 2019/8/22.
 */
public class DashboardAPIResult extends GrafanaAPIResult {
    public int id;
    public int version;
    public String title;
    public int org_id;
    public String uid;
    public int folder_id;
    public int is_folder;
    public GrafanaData data;
}
