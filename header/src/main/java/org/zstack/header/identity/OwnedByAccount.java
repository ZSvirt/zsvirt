package org.zstack.header.identity;

import org.zstack.header.vo.ResourceVO;
import org.zstack.utils.FieldUtils;

public interface OwnedByAccount {
    String getAccountUuid();
    void setAccountUuid(String accountUuid);

    default Class getResourceTypeClass() {
        return null;
    }

    static Class getResourceTypeClass(Object entity) {
        Class value = null;
        if (entity instanceof OwnedByAccount) {
            value = ((OwnedByAccount)entity).getResourceTypeClass();
        }
        return value == null ? entity.getClass() : value;
    }

    static String getResourceUuid(Object entity) {
        if (entity instanceof ResourceVO) {
            return ((ResourceVO) entity).getUuid();
        }

        return FieldUtils.getFieldValue("uuid", entity);
    }
}
