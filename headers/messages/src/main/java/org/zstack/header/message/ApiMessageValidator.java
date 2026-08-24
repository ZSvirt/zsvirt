package org.zstack.header.message;

import java.lang.reflect.Field;

/**
 * Created by xing5 on 2017/9/7.
 */
public interface ApiMessageValidator {
    void validate(APIMessage msg, Field field, Object value, APIParam param);
}
