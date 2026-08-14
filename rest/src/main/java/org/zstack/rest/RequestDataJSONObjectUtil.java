package org.zstack.rest;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import org.zstack.header.message.GsonTransient;
import org.zstack.utils.gson.GsonUtil;

/**
 * Created by lining on 2019/8/9.
 */
public class RequestDataJSONObjectUtil {
    private static final Gson gson;

    static {
        gson = new GsonUtil().setSerializationExclusionStrategy(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getAnnotation(GsonTransient.class) != null;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        }).create();
    }

    public static String toJsonString(Object obj) {
        return gson.toJson(obj);
    }
}
