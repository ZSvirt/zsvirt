package org.zstack.expon.sdk;

import java.util.ArrayList;
import java.util.List;

public abstract class ExponQueryRequest extends ExponRequest {
    public List<String> conditions = new ArrayList<>();
    public Long limit;
    public Long start;
    public String sortBy;
    public String sortDirection;

    public void addCond(String key, String value) {
        conditions.add(key + "=" + value);
    }
}
