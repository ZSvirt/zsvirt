package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;

/**
 * Created by miao on 16-10-18.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStorageHostRefVOFinder {
    @Autowired
    private DatabaseFacade dbf;

    public LocalStorageHostRefVO findByPrimaryKey(String hostUuid, String primaryStorageUuid) {
        SimpleQuery<LocalStorageHostRefVO> hq = dbf.createQuery(LocalStorageHostRefVO.class);
        hq.add(LocalStorageHostRefVO_.hostUuid, SimpleQuery.Op.EQ, hostUuid);
        hq.add(LocalStorageHostRefVO_.primaryStorageUuid, SimpleQuery.Op.EQ, primaryStorageUuid);
        LocalStorageHostRefVO ref = hq.find();
        return ref;
    }

    public boolean isExist(String hostUuid, String primaryStorageUuid) {
        SimpleQuery<LocalStorageHostRefVO> hq = dbf.createQuery(LocalStorageHostRefVO.class);
        hq.add(LocalStorageHostRefVO_.hostUuid, SimpleQuery.Op.EQ, hostUuid);
        hq.add(LocalStorageHostRefVO_.primaryStorageUuid, SimpleQuery.Op.EQ, primaryStorageUuid);
        return hq.isExists();
    }
}
