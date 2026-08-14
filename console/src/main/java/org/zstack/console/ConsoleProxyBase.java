package org.zstack.console;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.Timer;
import org.zstack.header.console.*;
import org.zstack.header.console.ConsoleProxyCommands.DeleteProxyCmd;
import org.zstack.header.console.ConsoleProxyCommands.DeleteProxyRsp;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.utils.URLBuilder;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.net.URI;
import java.sql.Timestamp;

import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:07 PM
 * To change this template use File | Settings | File Templates.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ConsoleProxyBase implements ConsoleProxy {
    private static final CLogger logger = Utils.getLogger(ConsoleProxyBase.class);
    private ConsoleProxyInventory self;

    @Autowired
    private RESTFacade restf;
    @Autowired
    private ConsoleManager consoleMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private Timer timer;

    private final int agentPort;

    public ConsoleProxyBase(ConsoleProxyVO vo, int agentPort) {
        self = ConsoleProxyInventory.valueOf(vo);
        this.agentPort = agentPort;
    }

    public ConsoleProxyBase(ConsoleProxyInventory inv, int agentPort) {
        self = inv;
        this.agentPort = agentPort;
    }

    private void doEstablishConsoleProxyConnection(ConsoleUrl consoleUrl, final ReturnValueCompletion<ConsoleProxyInventory> completion) {
        URI uri = consoleUrl.getUri();

        final String targetSchema = uri.getScheme();
        final String targetHostname = uri.getHost();
        final int targetPort = uri.getPort();

        if (targetHostname == null || targetPort < 0) {
            completion.fail(operr("establish VNC: unexpected uri: %s", uri.toString()));
            return;
        }

        int idleTimeout = ConsoleGlobalConfig.PROXY_IDLE_TIMEOUT.value(Integer.class);
        long tokenTimeout = ConsoleGlobalConfig.VNC_TOKEN_TIMEOUT.value(Long.class);
        long expiredDate = timer.getCurrentTimeMillis() + tokenTimeout * 1000;

        ConsoleProxyCommands.EstablishProxyCmd cmd = new ConsoleProxyCommands.EstablishProxyCmd();
        cmd.setVmUuid(self.getVmInstanceUuid());
        cmd.setTargetSchema(targetSchema);
        cmd.setTargetHostname(targetHostname);
        cmd.setTargetPort(targetPort);
        cmd.setProxyHostname("0.0.0.0");
        if (ConsoleConstants.HTTP_SCHEMA.equals(self.getTargetSchema())) {
            cmd.setProxyPort(CoreGlobalProperty.HTTP_CONSOLE_PROXY_PORT);
        } else {
            cmd.setProxyPort(CoreGlobalProperty.CONSOLE_PROXY_PORT);
        }
        cmd.setSslCertFile(CoreGlobalProperty.CONSOLE_PROXY_CERT_FILE);
        cmd.setScheme(self.getScheme());
        cmd.setToken(self.getToken());
        cmd.setIdleTimeout(idleTimeout);
        cmd.setExpiredDate(expiredDate);

        ConsoleProxyTlsVersion tlsVersion = ConsoleProxyTlsVersion.valueOf(ConsoleGlobalConfig.PROXY_TLS_VERSION.value());
        if (tlsVersion != ConsoleProxyTlsVersion.NONE) {
            cmd.setTlsVersion(tlsVersion.toCommandParameter());
        }

        String agentUrl = URLBuilder.buildHttpUrl(self.getAgentIp(), agentPort, ConsoleConstants.CONSOLE_PROXY_ESTABLISH_PROXY_PATH);
        restf.asyncJsonPost(agentUrl, cmd, new JsonAsyncRESTCallback<ConsoleProxyCommands.EstablishProxyRsp>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(ConsoleProxyCommands.EstablishProxyRsp ret) {
                if (ret.isSuccess()) {
                    self.setTargetSchema(targetSchema);
                    self.setTargetHostname(targetHostname);
                    self.setTargetPort(targetPort);
                    self.setProxyPort(ret.getProxyPort());
                    self.setVersion(consoleUrl.getVersion());
                    self.setExpiredDate(new Timestamp(expiredDate));
                    completion.success(self);
                } else {
                    completion.fail(operr("operation error, because:%s", ret.getError()));
                }
            }

            @Override
            public Class<ConsoleProxyCommands.EstablishProxyRsp> getReturnClass() {
                return ConsoleProxyCommands.EstablishProxyRsp.class;
            }
        });
    }

    void doEstablishDirectConsoleConnection(ConsoleUrl consoleUrl, final ReturnValueCompletion<ConsoleProxyInventory> completion) {
        URI uri = consoleUrl.getUri();

        final String targetSchema = uri.getScheme();
        final String targetHostname = uri.getHost();
        final String targetPath = uri.getPath();
        final int targetPort = uri.getPort();

        if (targetHostname == null || targetPort < 0) {
            completion.fail(operr("establish VNC: unexpected uri: %s", uri.toString()));
            return;
        }

        self.setTargetSchema(targetSchema);
        self.setTargetHostname(targetHostname);
        self.setTargetPort(targetPort);
        self.setProxyHostname(targetHostname);
        self.setProxyPort(targetPort);
        self.setToken(targetPath);
        self.setScheme(targetSchema);
        self.setVersion(consoleUrl.getVersion());

        completion.success(self);
    }

    private void doEstablish(ConsoleUrl consoleUrl, final ReturnValueCompletion<ConsoleProxyInventory> completion) {
        if (consoleUrl.isNeedConsoleProxy()) {
            doEstablishConsoleProxyConnection(consoleUrl, completion);
        } else {
            doEstablishDirectConsoleConnection(consoleUrl, completion);
        }
    }

    @Override
    public void establishProxy(final VmInstanceInventory vm, final ReturnValueCompletion<ConsoleProxyInventory> completion) {
        ConsoleHypervisorBackend bkd = consoleMgr.getHypervisorConsoleBackend(HypervisorType.valueOf(vm.getHypervisorType()));
        bkd.generateConsoleUrl(vm, new ReturnValueCompletion<ConsoleUrl>(completion) {

            @Override
            public void success(ConsoleUrl consoleUrl) {
                doEstablish(consoleUrl, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });

    }

    @Override
    public void checkAvailability(final ReturnValueCompletion<Boolean> completion) {
        ConsoleProxyCommands.CheckAvailabilityCmd cmd = new ConsoleProxyCommands.CheckAvailabilityCmd();
        cmd.setProxyHostname(self.getProxyHostname());
        cmd.setProxyPort(self.getProxyPort());
        cmd.setTargetSchema(self.getTargetSchema());
        cmd.setTargetHostname(self.getTargetHostname());
        cmd.setTargetPort(self.getTargetPort());
        cmd.setProxyIdentity(self.getProxyIdentity());
        cmd.setToken(self.getToken());
        cmd.setScheme(self.getScheme());
        restf.asyncJsonPost(URLBuilder.buildHttpUrl(self.getAgentIp(), agentPort, ConsoleConstants.CONSOLE_PROXY_CHECK_PROXY_PATH),
                cmd, new JsonAsyncRESTCallback<ConsoleProxyCommands.CheckAvailabilityRsp>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(ConsoleProxyCommands.CheckAvailabilityRsp ret) {
                if (ret.isSuccess()) {
                    completion.success(ret.getAvailable());
                } else {
                    completion.fail(operr("unable to check console proxy availability, because %s", ret.getError()));
                }
            }

            @Override
            public Class<ConsoleProxyCommands.CheckAvailabilityRsp> getReturnClass() {
                return ConsoleProxyCommands.CheckAvailabilityRsp.class;
            }

        });
    }

    @Override
    public void deleteProxy(VmInstanceInventory vm, final Completion completion) {
        deleteProxy(vm.getUuid(), completion);
    }

    private void deleteProxy(String vmInstanceUuid, Completion completion) {
        DeleteProxyCmd cmd = new DeleteProxyCmd();
        cmd.setProxyHostname(self.getProxyHostname());
        cmd.setProxyPort(self.getProxyPort());
        cmd.setTargetSchema(self.getTargetSchema());
        cmd.setTargetHostname(self.getTargetHostname());
        cmd.setTargetPort(self.getTargetPort());
        cmd.setToken(self.getToken());
        cmd.setVmUuid(vmInstanceUuid);

        restf.asyncJsonPost(URLBuilder.buildHttpUrl(self.getAgentIp(), agentPort, ConsoleConstants.CONSOLE_PROXY_DELETE_PROXY_PATH), cmd,
                new JsonAsyncRESTCallback<DeleteProxyRsp>(completion) {
                    @Override
                    public void fail(ErrorCode err) {
                        completion.fail(err);
                    }

                    @Override
                    public void success(DeleteProxyRsp ret) {
                        if (ret.isSuccess()) {
                            completion.success();
                        } else {
                            completion.fail(operr("operation error, because:%s", ret.getError()));
                        }
                    }

                    @Override
                    public Class<DeleteProxyRsp> getReturnClass() {
                        return DeleteProxyRsp.class;
                    }
                });
    }

    @Override
    public void deleteProxy(ConsoleProxyInventory proxy, Completion completion) {
        deleteProxy(proxy.getVmInstanceUuid(), completion);
    }
}
