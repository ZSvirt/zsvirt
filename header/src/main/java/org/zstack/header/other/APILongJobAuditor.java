package org.zstack.header.other;

import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;

/**
 * Created by mingjian.deng on 2019/1/23.
 */
public interface APILongJobAuditor {
    APIAuditor.Result longJobAudit(LongJob job, LongJobVO vo, APIEvent rsp);
}
