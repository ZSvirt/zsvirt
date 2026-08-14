package org.zstack.kvm;

import org.zstack.utils.gson.JSONObjectUtil;

import java.util.LinkedHashMap;

/**
 * Created by xing5 on 2016/4/19.
 */
public class KvmResponseWrapper {
    private LinkedHashMap response;
    private Object cache;

    public KvmResponseWrapper(LinkedHashMap response) {
        this.response = response;
    }

    // this method may be called multiple times
    // use a cache to save the JSON dump effort
    public <T> T getResponse(Class<T> type) {
        if (cache == null) {
            cache = JSONObjectUtil.rehashObject(response, type);
        }

        return (T) cache;
    }
}
