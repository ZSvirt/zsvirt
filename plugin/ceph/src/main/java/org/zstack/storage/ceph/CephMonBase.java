package org.zstack.storage.ceph;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.opaque.OpaqueScripts;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;
import org.zstack.utils.ssh.SshResult;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_RESPONSE_ERROR;

/**
 * Created by frank on 7/27/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public abstract class CephMonBase {
    protected CephMonAO self;

    @Autowired
    protected RESTFacade restf;

    public static class PingResult {
        public String failure;
        public boolean success;
        public String error;
    }

    public abstract void connect(Completion completion);

    public abstract void ping(ReturnValueCompletion<PingResult> completion);

    protected abstract int getAgentPort();

    protected abstract String makeHttpPath(String ip, String path);

    public CephMonBase(CephMonAO self) {
        this.self = self;
    }

    protected void checkTools() {
        Ssh ssh = new Ssh();
        try {
            ssh.setHostname(self.getHostname()).setUsername(self.getSshUsername()).setPassword(self.getSshPassword()).setPort(self.getSshPort())
                    .checkTool("ceph", "rbd").setTimeout(60).runErrorByExceptionAndClose();
        } catch (SshException e) {
            throw new OperationFailureException(operr("The problem may be caused by an incorrect user name or password or SSH port or unstable network environment"));
        }
    }

    protected void checkHealth() {
        Ssh ssh = new Ssh();
        SshResult ret = null;
        try {
            ret = ssh.setHostname(self.getHostname()).setUsername(self.getSshUsername()).setPassword(self.getSshPassword()).setPort(self.getSshPort())
                    .shell("ceph health").setTimeout(60).runAndClose();
        } catch (SshException e) {
            throw new OperationFailureException(operr("The problem may be caused by an incorrect user name or password or SSH port or unstable network environment"));
        }

        if (ret.getReturnCode() != 0) {
            ret.setSshFailure(true);
            throw new SshException("failed to check health to ceph mon").withResult(ret);
        }

        String stdOut = ret.getStdout();
        if (stdOut.contains("HEALTH_ERROR")) {
            throw new SshException("ceph health error").withResult(ret);
        }
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
            restf.asyncJsonPost(makeHttpPath(self.getHostname(), path), cmd, callback);
        } else {
            restf.asyncJsonPost(makeHttpPath(self.getHostname(), path), cmd, callback, unit, timeout);
        }
    }

    public static class AgentResponse implements OpaqueScripts {
        private String error;
        @Deprecated
        private boolean success = true;

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.success = false;
            this.error = error;
        }

        @Deprecated
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
            return operr(OPAQUE_KEY_RESPONSE_ERROR).withOpaque(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, Object> opaqueScripts() {
            if (!success) {
                return map(
                    e(OPAQUE_KEY_RESPONSE_ERROR, error)
                );
            }
            return Collections.emptyMap();
        }
    }

    public void httpCall(final String path, final Object cmd, final Completion completion) {
        restf.asyncJsonPost(makeHttpPath(self.getHostname(), path), cmd, new JsonAsyncRESTCallback<AgentResponse>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(AgentResponse ret) {
                if (ret.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(Platform.operr("operation error, because:%s", ret.getError()));
                }
            }

            @Override
            public Class<AgentResponse> getReturnClass() {
                return AgentResponse.class;
            }
        });
    }

    public CephMonAO getSelf() {
        return self;
    }
}
