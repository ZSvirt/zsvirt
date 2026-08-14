package org.zstack.cloudformation.template.struct;

import com.google.gson.Gson;

/**
 * Created by mingjian.deng on 2018/5/31.
 */
public interface ZStackResourceTemplateStruct {
    default String print() {
        return new Gson().toJson(this);
    }
}
