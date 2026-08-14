package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.thread.AsyncTimer;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.ConnectBackupStorageMsg;

import java.util.concurrent.TimeUnit;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BackupStorageReconnectTask extends AsyncTimer {
    protected String uuid;
    private NoErrorCompletion completion;

    @Autowired
    protected CloudBus bus;

    public BackupStorageReconnectTask(String uuid, NoErrorCompletion completion) {
        super(TimeUnit.SECONDS, BackupStorageGlobalConfig.PING_INTERVAL.value(Long.class));
        this.uuid = uuid;
        this.completion = completion;

        __name__ = String.format("backup-storage-%s-reconnect-task", uuid);
    }

    @Override
    protected void execute() {
        ConnectBackupStorageMsg cmsg = new ConnectBackupStorageMsg();
        cmsg.setBackupStorageUuid(uuid);
        cmsg.setNewAdd(false);
        bus.makeTargetServiceIdByResourceUuid(cmsg, BackupStorageConstant.SERVICE_ID, uuid);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    // reconnect the backup storage successfully, cancel this reconnect task
                    cancel();
                    completion.done();
                } else {
                    // still fail to reconnect the backup storage, continue this reconnect task
                    continueToRunThisTimer();
                }
            }
        });
    }

    protected boolean taskIsCanceled(){
        return isCanceled();
    }
}
