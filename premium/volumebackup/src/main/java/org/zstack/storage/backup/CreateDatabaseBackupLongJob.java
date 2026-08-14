package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.database.backup.*;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * Created by MaJin on 2019/3/8.
 */

@LongJobFor(APICreateDatabaseBackupMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateDatabaseBackupLongJob implements LongJob {
    @Autowired
    private CloudBus bus;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        List<ErrorCode> errors = new ArrayList<>();
        APICreateDatabaseBackupEvent evt = new APICreateDatabaseBackupEvent(job.getApiId());
        DatabaseBackupLongJobParams params = JSONObjectUtil.toObject(job.getJobData(), DatabaseBackupLongJobParams.class);
        new While<>(params.getAlternativeBackupStorageUuids()).each((bsUuid, compl) -> {
            CreateDatabaseBackupMsg msg = buildMsg(params);
            msg.setBackupStorageUuid(bsUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, DatabaseBackupConstant.SERVICE_ID, bsUuid);
            bus.send(msg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        CreateDatabaseBackupReply r = reply.castReply();
                        evt.setInventory(r.getInventory());
                        compl.allDone();
                    } else {
                        errors.add(reply.getError());
                        compl.done();
                    }
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (evt.getInventory() == null && !errors.isEmpty()) {
                    completion.fail(errors.get(0));
                } else {
                    completion.success(evt);
                }
            }
        });

    }

    private CreateDatabaseBackupMsg buildMsg(DatabaseBackupLongJobParams params) {
        CreateDatabaseBackupMsg cmsg = new CreateDatabaseBackupMsg();
        cmsg.setName(params.getName());
        cmsg.setDescription(params.getDescription());
        return cmsg;
    }
}
