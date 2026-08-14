package org.zstack.header.core.encrypt;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;

import java.util.List;
import java.util.Map;

/**
 * @author hanyu.liang
 * @date 2023/4/10 15:35
 */
public interface IntegrityVerificationResourceFactory {
    String getResourceType();

    void doIntegrityVerification(Integer integrityDataRangeInDays, Completion completion);

    void doCheckBatchResourceIntegrity(final List<String> resourceUuids, ReturnValueCompletion<Map<String, Boolean>> returnValueCompletion);

    default void upgradeExtension(){
    }

    default void doIntegrityAfterSaveDbRecord(Object entity) {
    }

    default void doIntegrityAfterUpdateDbRecord(Object entity) {
    }
}
