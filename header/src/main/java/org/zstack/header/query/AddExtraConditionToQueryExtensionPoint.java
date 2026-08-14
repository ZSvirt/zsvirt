package org.zstack.header.query;

import java.util.List;

/**
 */
public interface AddExtraConditionToQueryExtensionPoint {
    List<Class> getMessageClassesForAddExtraConditionToQueryExtensionPoint();

    List<QueryCondition> getExtraQueryConditionForMessage(APIQueryMessage msg);
}
