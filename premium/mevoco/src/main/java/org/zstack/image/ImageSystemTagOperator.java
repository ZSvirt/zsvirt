package org.zstack.image;

import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.TagType;

/**
 * Created by mingjian.deng on 16/11/18.
 */
public class ImageSystemTagOperator {
    public boolean isQemuGASystemTagOnVm (String resourceUuid, DatabaseFacade dbf) {
        SimpleQuery<SystemTagVO> query = dbf.createQuery(SystemTagVO.class);
        query.add(SystemTagVO_.resourceUuid, SimpleQuery.Op.EQ, resourceUuid);
        query.add(SystemTagVO_.type, SimpleQuery.Op.EQ, TagType.System);
        query.add(SystemTagVO_.tag, SimpleQuery.Op.EQ, ImageSystemTags.IMAGE_INJECT_QEMUGA.getTagFormat());
        SystemTagVO systemTag = query.find();
        if (systemTag != null) {
            return true;
        }
        return false;
    }
}
