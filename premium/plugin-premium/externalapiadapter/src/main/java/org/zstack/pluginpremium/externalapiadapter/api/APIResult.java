package org.zstack.pluginpremium.externalapiadapter.api;

/**
 * Created by lining on 2018/5/10.
 */
public class APIResult {
    private Object value;
    
    private APIError error;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public APIError getError() {
        return error;
    }

    public void setError(APIError error) {
        this.error = error;
    }
}
