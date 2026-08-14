package org.zstack.zwatch.datatype;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;

import java.util.List;

public abstract class ApiMultiAuditor extends ApiAuditor {
    public ApiMultiAuditor(Class apiClass) {
        super(apiClass);
    }

    public final Result audit(APIMessage msg, APIEvent rsp) {
        return null;
    }

    public abstract List<Result> multiAudit(APIMessage msg, APIEvent rsp);
}
