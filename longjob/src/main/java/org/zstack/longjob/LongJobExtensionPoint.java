package org.zstack.longjob;

import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;

/**
 * Created by mingjian.deng on 2019/1/22.
 */
public interface LongJobExtensionPoint {
    void afterJobFinished(LongJob job, LongJobVO vo, APIEvent evt);
    void afterJobFailed(LongJob job, LongJobVO vo, APIEvent evt);
    void afterJobFinished(LongJob job, LongJobVO vo);
}
