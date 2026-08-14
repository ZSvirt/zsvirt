package org.zstack.zwatch.datatype;

import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;

/**
 * Created by mingjian.deng on 2019/1/23.
 */
public abstract class ApiLongJobAuditor extends ApiAuditor {
    public ApiLongJobAuditor(Class apiClass) {
        super(apiClass);
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return null;
    }

    public abstract Result longJobAudit(LongJob job, LongJobVO vo, APIEvent rsp);
}
