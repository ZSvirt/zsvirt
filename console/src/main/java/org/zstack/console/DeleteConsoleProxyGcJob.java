package org.zstack.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.console.ConsoleBackend;
import org.zstack.header.console.ConsoleProxyAgentStatus;
import org.zstack.header.console.ConsoleProxyAgentVO;
import org.zstack.header.console.ConsoleProxyAgentVO_;
import org.zstack.header.console.ConsoleProxyInventory;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

public class DeleteConsoleProxyGcJob extends TimeBasedGarbageCollector {
    private static final CLogger logger = Utils.getLogger(DeleteConsoleProxyGcJob.class);

    @GC
    public ConsoleProxyInventory consoleProxy;

    @Autowired
    private ConsoleManager consoleMgr;

    @Override
    protected void triggerNow(GCCompletion completion) {
        ConsoleBackend backend = consoleMgr.getConsoleBackend();
        if (backend == null) {
            // no available backend, cancel the gc job
            completion.cancel();
            return;
        }

        ConsoleProxyAgentStatus status = Q.New(ConsoleProxyAgentVO.class)
                .select(ConsoleProxyAgentVO_.status)
                .eq(ConsoleProxyAgentVO_.consoleProxyOverriddenIp, consoleProxy.getAgentIp())
                .findValue();
        if (status == null) {
            logger.debug(String.format("console proxy not found on agent[ip: %s, uuid: %s]," +
                    " assume it has been deleted",
                    consoleProxy.getAgentIp(),
                    consoleProxy.getUuid()
            ));
            completion.cancel();
            return;
        }

        if (status != ConsoleProxyAgentStatus.Connected) {
            completion.fail(operr("console proxy[uuid: %s, status: %s] on agent[ip: %s]" +
                            " is not Connected, fail to delete it",
                    consoleProxy.getUuid(),
                    status,
                    consoleProxy.getAgentIp())
            );
            return;
        }

        backend.deleteConsoleSession(consoleProxy, new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }
}
