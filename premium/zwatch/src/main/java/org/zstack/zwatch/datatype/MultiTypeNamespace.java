package org.zstack.zwatch.datatype;

import org.zstack.core.db.Q;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.zwatch.ruleengine.RuleEvaluationResult;

import java.util.List;

public interface MultiTypeNamespace {
    default List<RuleEvaluationResult> filterRuleEvaluationResults(String alarmUuid, List<RuleEvaluationResult> results) {
        return results;
    }

    default boolean resourceAlarmIsMatch(String resourceUuid, String alarmUuid) {
        return true;
    }

    default boolean resourceEventIsMatch(String resourceUuid, String eventUuid) {
        return true;
    }

    default String getAlarmNameSpace(String resourceUuid, String alarmUuid, String namespace) {
        return namespace;
    }

    default String getAlarmI18nMetric(String resourceUuid, String alarmUuid, String i18nMetric) {
        return i18nMetric;
    }

    default String getEventNameSpace(String resourceUuid, String eventUuid, String namespace) {
        return namespace;
    }

    default List<String> getTags(String resourceUuid, String resourceType) {
        return Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, resourceUuid)
                .eq(SystemTagVO_.resourceType, resourceType).select(SystemTagVO_.tag).listValues();
    }
}
