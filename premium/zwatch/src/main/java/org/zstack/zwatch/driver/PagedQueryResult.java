package org.zstack.zwatch.driver;

import org.zstack.header.errorcode.ErrorCode;

import java.util.List;

/**
 * Created by lining on 2018/11/14.
 */
public class PagedQueryResult<T> {
    private List<T> data;
    private ErrorCode error;

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }
}
