package org.zstack.header.zql;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;

import java.util.List;
import java.util.Map;

public interface ReturnWithExtensionPoint {
    class ReturnWithExtensionParam {
        public List vos;
        public String expression;
        public boolean isFieldsQuery;
        public Integer primaryKeyIndexInVOs;
        public Class voClass;
    }

    String getReturnWithName();

    void returnWith(ReturnWithExtensionParam param, ReturnValueCompletion<Map> completion);
}
