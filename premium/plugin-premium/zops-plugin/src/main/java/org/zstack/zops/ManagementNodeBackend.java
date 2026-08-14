package org.zstack.zops;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.zops.utils.CommandResult;
import org.zstack.utils.Utils;
import org.zstack.utils.ctl.ZStackCtlHelper;
import org.zstack.utils.ctl.ZStackCtlResult;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.util.List;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ManagementNodeBackend extends AbstractHostBackend {
    private static final CLogger logger = Utils.getLogger(ManagementNodeBackend.class);

    @Autowired
    private CloudBus bus;

    private String uuid;

    ManagementNodeBackend(String uuid, String hostname){
        this.types.add(HostType.ManagementNode);
        this.uuid = uuid;
        this.hostname = hostname;
        this.client.setHostname(hostname);
    }

    ManagementNodeBackend(ManagementNodeBaseInfo mnInfo) {
        this(mnInfo.getUuid(), mnInfo.getHostname());
        extraIps.addAll(mnInfo.getExtraIps());
        types.addAll(mnInfo.getTypes());
    }

    @Override
    public void getChronyServers(ReturnValueCompletion<List<ChronyServerInfoPair>> completion) {
        if (uuid.equals(Platform.getManagementServerId())) {
            super.getChronyServers(completion);
        } else {
            GetChronyServersManagementNodeMsg gmsg = new GetChronyServersManagementNodeMsg();
            send(gmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }
                    GetChronyServersManagementNodeReply gp = reply.castReply();
                    completion.success(gp.getServers());
                }
            });
        }
    }

    @Override
    public void getCephMonHealthStatus(ReturnValueCompletion<String> completion) {
        if (uuid.equals(Platform.getManagementServerId())) {
            super.getCephMonHealthStatus(completion);
        } else {
            GetCephMonHealthStatusManagementNodeMsg gmsg = new GetCephMonHealthStatusManagementNodeMsg();
            send(gmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }
                    GetCephMonHealthStatusManagementNodeReply gp = reply.castReply();
                    completion.success(gp.getStatus());
                }
            });
        }
    }

    @Override
    public void checkMultiHostsNetworkReachable(List<String> targetHostname, ReturnValueCompletion<List<NetworkReachablePair>> completion) {
        if (uuid.equals(Platform.getManagementServerId())) {
            super.checkMultiHostsNetworkReachable(targetHostname, completion);
        } else {
            CheckMultiHostsNetworkReachableManagementNodeMsg cmsg = new CheckMultiHostsNetworkReachableManagementNodeMsg();
            cmsg.setTargetHostname(targetHostname);
            send(cmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }
                    CheckMultiHostsNetworkReachableManagementNodeReply cp = reply.castReply();
                    completion.success(cp.getResult());
                }
            });
        }
    }

    public void updateManagementNodeChronyProperties(List<String> chronyServer, Completion completion) {
        if (uuid.equals(Platform.getManagementServerId())) {
            doUpdateManagementNodeChronyProperties(chronyServer, completion);
        } else {
            UpdateManagementNodeChronyPropertiesManagementNodeMsg umsg = new UpdateManagementNodeChronyPropertiesManagementNodeMsg();
            umsg.setChronyServer(chronyServer);
            send(umsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }
                    completion.success();
                }
            });
        }
    }

    public void doUpdateManagementNodeChronyProperties(List<String> chronyServer, Completion completion) {
        String zstackHome = PathUtil.getZStackHomeFolder();
        CommandResult res = client.command(String.format("sed -i '/^chrony\\.serverIp\\.[0-9]/d' %s/apache-tomcat/webapps/zstack/WEB-INF/classes/zstack.properties", zstackHome), true).run();
        if (res.getRetCode() != 0) {
            completion.fail(operr("fail to delete old chrony server in zstack.properties in %s, because:%s", hostname, res.getStderr()));
            return;
        }

        StringBuilder chronyConfig = new StringBuilder();
        int n = 0;
        for (String s : chronyServer) {
            chronyConfig.append(String.format("chrony.serverIp.%d=%s ", n++, s));
        }

        res = client.command(String.format("zstack-ctl configure %s", chronyConfig).trim(), true).run();
        if (res.getRetCode() != 0) {
            completion.fail(operr("fail to config chrony %s server in zstack.properties in %s, because:%s", chronyServer, hostname, res.getStderr()));
            return;
        }

        CoreGlobalProperty.CHRONY_SERVERS = chronyServer;
        completion.success();
    }

    @Override
    public void updateChronyServers(List<String> chronyServers, Completion completion) {
        if (uuid.equals(Platform.getManagementServerId())) {
            super.updateChronyServers(chronyServers, completion);
        } else {
            UpdateChronyServersManagementNodeMsg umsg = new UpdateChronyServersManagementNodeMsg();
            umsg.setChronyServers(chronyServers);
            send(umsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }
                    completion.success();
                }
            });
        }
    }

    @Override
    public void syncChronyServers(Completion completion) {
        if (uuid.equals(Platform.getManagementServerId())) {
            super.syncChronyServers(completion);
        } else {
            SyncChronyServersManagementNodeMsg smsg = new SyncChronyServersManagementNodeMsg();

            send(smsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }
                    completion.success();
                }
            });
        }
    }

    void handleMessage(ManagementNodeMessage msg) {
        if (msg instanceof GetChronyServersManagementNodeMsg) {
            handle((GetChronyServersManagementNodeMsg) msg);
        } else if (msg instanceof CheckMultiHostsNetworkReachableManagementNodeMsg) {
            handle((CheckMultiHostsNetworkReachableManagementNodeMsg) msg);
        } else if (msg instanceof GetCephMonHealthStatusManagementNodeMsg) {
            handle((GetCephMonHealthStatusManagementNodeMsg) msg);
        } else if (msg instanceof UpdateChronyServersManagementNodeMsg) {
            handle((UpdateChronyServersManagementNodeMsg) msg);
        } else if (msg instanceof UpdateManagementNodeChronyPropertiesManagementNodeMsg) {
            handle((UpdateManagementNodeChronyPropertiesManagementNodeMsg) msg);
        } else if (msg instanceof SyncChronyServersManagementNodeMsg) {
            handle((SyncChronyServersManagementNodeMsg) msg);
        }
    }

    private void handle(SyncChronyServersManagementNodeMsg msg) {
        SyncChronyServersManagementNodeReply reply = new SyncChronyServersManagementNodeReply();
        syncChronyServers(new Completion(null) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(UpdateManagementNodeChronyPropertiesManagementNodeMsg msg) {
        UpdateManagementNodeChronyPropertiesManagementNodeReply reply = new UpdateManagementNodeChronyPropertiesManagementNodeReply();
        updateManagementNodeChronyProperties(msg.getChronyServer(), new Completion(null) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(GetChronyServersManagementNodeMsg msg) {
        GetChronyServersManagementNodeReply reply = new GetChronyServersManagementNodeReply();
        getChronyServers(new ReturnValueCompletion<List<ChronyServerInfoPair>>(null) {
            @Override
            public void success(List<ChronyServerInfoPair> returnValue) {
                reply.setServers(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CheckMultiHostsNetworkReachableManagementNodeMsg msg) {
        CheckMultiHostsNetworkReachableManagementNodeReply reply = new CheckMultiHostsNetworkReachableManagementNodeReply();
        checkMultiHostsNetworkReachable(msg.getTargetHostname(), new ReturnValueCompletion<List<NetworkReachablePair>>(null) {
            @Override
            public void success(List<NetworkReachablePair> returnValue) {
                reply.setResult(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(GetCephMonHealthStatusManagementNodeMsg msg) {
        GetCephMonHealthStatusManagementNodeReply reply = new GetCephMonHealthStatusManagementNodeReply();
        getCephMonHealthStatus(new ReturnValueCompletion<String>(null) {
            @Override
            public void success(String returnValue) {
                reply.setStatus(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(UpdateChronyServersManagementNodeMsg msg) {
        UpdateChronyServersManagementNodeReply reply = new UpdateChronyServersManagementNodeReply();
        updateChronyServers(msg.getChronyServers(), new Completion(null) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    void send(NeedReplyMessage msg, CloudBusCallBack callback) {
        bus.makeServiceIdByManagementNodeId(msg, ZOpsConstants.SERVICE_ID, uuid);
        bus.send(msg, callback);
    }
}
