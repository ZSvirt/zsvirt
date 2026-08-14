package org.zstack.zwatch.driver;

import org.zstack.zwatch.migratedb.AuditsVO;

/**
 * @Author: DaoDao
 * @Date: 2021/11/19
 */
public interface AfterSaveAuditsExtensionPoint {
    void saveEncryptAfterSaveAudits(AuditsVO auditsVO);
}
