package org.zstack.storage.zbs;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.cbd.ClusterInfo;
import org.zstack.cbd.MdsInfo;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;
import org.zstack.utils.ssh.SshResult;

import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * @author Xingwei Yu
 * @date 2024/4/2 11:40
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public abstract class ZbsMdsBase {
    private MdsInfo self;

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected RESTFacade restf;

    public ZbsMdsBase(MdsInfo self) {
        this.self = self;
    }

    public MdsInfo getSelf() {
        return self;
    }

    public abstract void connect(Completion completion);
    public abstract void ping(ClusterInfo clusterInfo, Completion completion);
    protected abstract String makeHttpPath(String ip, String path);

    protected void checkSshAndTools() {
        Ssh ssh = new Ssh();
        try {
            ssh.setHostname(self.getAddr()).setUsername(self.getUsername()).setPassword(self.getPassword()).setPort(self.getPort())
                    .checkTool("zbs").setTimeout(60).runErrorByExceptionAndClose();
        } catch (SshException e) {
            throw new OperationFailureException(operr("failed to SSH or zbs-tools was not installed in MDS[%s], you need to check the SSH configuration and dependencies", self.getAddr()));
        }
    }

    protected void checkStorageHealth() {
        Ssh ssh = new Ssh();
        SshResult ret = null;
        try {
            ret = ssh.setHostname(self.getAddr()).setUsername(self.getUsername()).setPassword(self.getPassword()).setPort(self.getPort())
                    .shell("zbs status mds --format json").setTimeout(60).runAndClose();
        } catch (SshException e) {
            throw new OperationFailureException(operr("failed to get MDS[%s] metadata, you need to check the ZBS configuration", self.getAddr()));
        }

        if (ret.getReturnCode() != 0) {
            ret.setSshFailure(true);
            throw new SshException(ret.getStderr());
        }

        String stdout = ret.getStdout();
        if (!stdout.contains("leader")) {
            throw new SshException(stdout);
        }
    }

    public <T extends AgentResponse> T syncCall(final String path, final Object cmd, final Class<T> retClass, TimeUnit unit, long timeout) {
        return restf.syncJsonPost(makeHttpPath(self.getAddr(), path), cmd, retClass, unit, timeout);
    }

    public <T extends AgentResponse> void httpCall(final String path, final Object cmd, final Class<T> retClass, final ReturnValueCompletion<T> completion) {
        httpCall(path, cmd, retClass, completion, null, 0);
    }

    public <T extends AgentResponse> void httpCall(final String path, final Object cmd, final Class<T> retClass, final ReturnValueCompletion<T> completion, TimeUnit unit, long timeout) {
        JsonAsyncRESTCallback<T> callback = new JsonAsyncRESTCallback<T>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(T ret) {
                ErrorCode errorCode = ret.buildErrorCode();
                if (errorCode != null) {
                    completion.fail(errorCode);
                    return;
                }
                completion.success(ret);
            }

            @Override
            public Class<T> getReturnClass() {
                return retClass;
            }
        };

        if (unit == null) {
            restf.asyncJsonPost(makeHttpPath(self.getAddr(), path), cmd, callback);
        } else {
            restf.asyncJsonPost(makeHttpPath(self.getAddr(), path), cmd, callback, unit, timeout);
        }
    }

    public static class AgentResponse {
        private String error;
        private boolean success = true;

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.success = false;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public ErrorCode buildErrorCode() {
            if (success) {
                return null;
            }
            return operr("operation error, because:%s", error);
        }
    }

    public static class AgentCommand {
        private String uuid;
        private String addr;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getAddr() {
            return addr;
        }

        public void setAddr(String addr) {
            this.addr = addr;
        }
    }
}
