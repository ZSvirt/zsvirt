package org.zstack.header.core.encrypt;

import org.zstack.header.vo.ResourceVO;

/**
 * @Author: DaoDao
 * @Date: 2021/12/2
 */
public interface EncryptAfterSaveDbRecordExtensionPoint {
    void encryptAfterSaveDbRecord(ResourceVO resourceVO);
    void encryptAfterUpdateDbRecord(ResourceVO resourceVO);
}
