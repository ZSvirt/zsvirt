package org.zstack.nas;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;

/**
 * Created by mingjian.deng on 2018/3/6.
 */
public class NasFileSystemApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateNasFileSystemMsg) {
            validate((APICreateNasFileSystemMsg) msg);
        }

        return msg;
    }

    private void validate(final APICreateNasFileSystemMsg msg) {
        if (msg.getProtocol() == null) {
            msg.setProtocol(NasProtocolType.NFS.toString());
        }
    }
}
