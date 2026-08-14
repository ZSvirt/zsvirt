package org.zstack.zwatch.namespace;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.longjob.LongJobExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by mingjian.deng on 2019/1/22.
 */
public class LongJobAuditExtension implements LongJobExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LongJobAuditExtension.class);
    @Autowired
    private NamespaceEventManager eventMgr;

    @Override
    public void afterJobFinished(LongJob job, LongJobVO vo, APIEvent evt) {
        if (evt != null && job.getAuditType() != null) {
            logger.debug(String.format("audit longjob finished %s for resourceType: %s, msgId: %s", vo.getUuid(), job.getAuditType().getSimpleName(), evt.getApiId()));
        }

        eventMgr.longJobAudit(job, vo, evt);
    }

    @Override
    public void afterJobFailed(LongJob job, LongJobVO vo, APIEvent evt) {
        if (job.getAuditType() != null) {
            logger.debug(String.format("audit longjob failed %s for resourceType: %s, msgId: %s", vo.getUuid(), job.getAuditType().getSimpleName(), evt.getApiId()));
        }
        eventMgr.longJobAudit(job, vo, evt);
    }

    @Override
    public void afterJobFinished(LongJob job, LongJobVO vo) {
        APIEvent evt = new APIEvent(vo.getApiId());
        eventMgr.longJobAudit(job, vo, evt);
    }
}
