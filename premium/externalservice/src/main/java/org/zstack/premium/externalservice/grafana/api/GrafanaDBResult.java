package org.zstack.premium.externalservice.grafana.api;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by mingjian.deng on 2019/8/22.
 */
public class GrafanaDBResult {
    public ErrorCode error;
    public boolean success = true;
    public String result;

    public void setError(ErrorCode error) {
        this.error = error;
        this.success = false;
    }

    public GrafanaDBResult(ErrorCode errCode) {
        setError(errCode);
    }

    public GrafanaDBResult(String result) {
        this.result = result;
    }
}
