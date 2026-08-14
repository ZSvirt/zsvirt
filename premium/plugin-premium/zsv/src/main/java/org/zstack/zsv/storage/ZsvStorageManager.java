package org.zstack.zsv.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.core.workflow.ShareFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMHostVO;
import org.zstack.kvm.KVMHostVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zsv.ZsvConstant;
import org.zstack.zsv.core.header.GetManagementNodeRolesMsg;
import org.zstack.zsv.core.header.GetManagementNodeRolesReply;
import org.zstack.zsv.storage.api.APICheckCephPluginMsg;
import org.zstack.zsv.storage.api.APICheckCephPluginReply;
import org.zstack.zsv.storage.api.HostSshParameter;
import org.zstack.zsv.storage.compute.CephPlugin;
import org.zstack.zsv.storage.entity.CephPluginConnectionView;

import javax.persistence.Tuple;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.emptyMap;
import static org.zstack.core.Platform.multiErr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.isEmpty;

public class ZsvStorageManager extends AbstractService implements Component {
    private final static CLogger logger = Utils.getLogger(ZsvStorageManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired(required = false)
    private List<CephPlugin> cephPlugins;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APICheckCephPluginMsg) {
            handle((APICheckCephPluginMsg) msg);
        }
    }

    private void handle(APICheckCephPluginMsg msg) {
        APICheckCephPluginReply reply = new APICheckCephPluginReply();

        if (isEmpty(cephPlugins)) {
            bus.reply(msg, reply);
            return;
        }

        FlowChain chain = new ShareFlowChain();
        chain.setName("check-ceph-plugin");
        chain.then(new ShareFlow() {
            Set<String> ipList = new HashSet<>();
            Map<String, String> mnUuidByIp = new HashMap<>();
            Map<String, String> hostUuidByIp = new HashMap<>();
            Map<String, HostSshParameter> sshParameterByIp = new HashMap<>();

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "build-ip-from-host-uuid";

                    @Override
                    public boolean skip(Map data) {
                        return isEmpty(msg.getHostUuidList());
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<KVMHostVO> kvmHosts = Q.New(KVMHostVO.class).in(KVMHostVO_.uuid, msg.getHostUuidList()).list();
                        for (KVMHostVO kvmHost : kvmHosts) {
                            HostSshParameter sshParam = new HostSshParameter(kvmHost.getManagementIp(), kvmHost.getUsername(), kvmHost.getPassword(), kvmHost.getPort());
                            sshParameterByIp.put(kvmHost.getManagementIp(), sshParam);
                            hostUuidByIp.put(kvmHost.getManagementIp(), kvmHost.getUuid());
                        }
                        ipList.addAll(hostUuidByIp.keySet());
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "build-ip-from-external-host";

                    @Override
                    public boolean skip(Map data) {
                        return isEmpty(msg.getExternalHosts());
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (HostSshParameter sshParam : msg.getExternalHosts()) {
                            sshParameterByIp.put(sshParam.getIp(), sshParam);
                        }
                        ipList.addAll(sshParameterByIp.keySet());
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "build-ip-from-mn";

                    @Override
                    public boolean skip(Map data) {
                        return !msg.isManagementNode();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<Tuple> mnTuples = Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid, ManagementNodeVO_.hostName).listTuple();
                        if (CoreGlobalProperty.UNIT_TEST_ON) {
                            for (Tuple mnTuple : mnTuples) {
                                String managementNodeUuid = mnTuple.get(0, String.class);
                                String hostName = mnTuple.get(1, String.class);
                                HostSshParameter sshParam = new HostSshParameter(hostName);
                                sshParameterByIp.put(hostName, sshParam);
                                mnUuidByIp.put(hostName, managementNodeUuid);
                                ipList.add(hostName);
                            }
                            trigger.next();
                            return;
                        }

                        List<Tuple> needGetHostInfoMn = new ArrayList<>();
                        for (Tuple mnTuple : mnTuples) {
                            String managementNodeUuid = mnTuple.get(0, String.class);
                            String hostName = mnTuple.get(1, String.class);

                            KVMHostVO host = Q.New(KVMHostVO.class).eq(KVMHostVO_.managementIp, hostName).find();
                            if (host == null) {
                                needGetHostInfoMn.add(mnTuple);
                                continue;
                            }

                            HostSshParameter sshParam = new HostSshParameter(host.getManagementIp(), host.getUsername(), host.getPassword(), host.getPort());
                            sshParameterByIp.put(host.getManagementIp(), sshParam);
                            mnUuidByIp.put(host.getManagementIp(), managementNodeUuid);
                            ipList.addAll(sshParameterByIp.keySet());
                        }

                        if (needGetHostInfoMn.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        new While<>(needGetHostInfoMn).each((tuple, whileCompletion) -> {
                            String managementNodeUuid = tuple.get(0, String.class);

                            GetManagementNodeRolesMsg innerMsg = new GetManagementNodeRolesMsg();
                            bus.makeServiceIdByManagementNodeId(innerMsg, ZsvConstant.SERVICE_ID, managementNodeUuid);
                            bus.send(innerMsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply messageReply) {
                                    if (!messageReply.isSuccess()) {
                                        whileCompletion.addError(operr("failed to check if management node is also compute node, node: %s", managementNodeUuid));
                                        whileCompletion.done();
                                        return;
                                    }

                                    GetManagementNodeRolesReply innerReply = messageReply.castReply();
                                    if (!innerReply.isHost()) {
                                        whileCompletion.addError(operr("management node[uuid:%s] must be a compute node", managementNodeUuid));
                                        whileCompletion.done();
                                        return;
                                    }

                                    String hostUuid = innerReply.getHostUuid();
                                    if (hostUuid == null) {
                                        whileCompletion.addError(operr("failed to retrieve host uuid [management uuid:%s]", managementNodeUuid));
                                        whileCompletion.done();
                                        return;
                                    }

                                    KVMHostVO mnHost = Q.New(KVMHostVO.class).eq(KVMHostVO_.uuid, hostUuid).find();
                                    HostSshParameter sshParam = new HostSshParameter(mnHost.getManagementIp(), mnHost.getUsername(), mnHost.getPassword(), mnHost.getPort());
                                    sshParameterByIp.put(mnHost.getManagementIp(), sshParam);
                                    mnUuidByIp.put(mnHost.getManagementIp(), managementNodeUuid);
                                    ipList.addAll(sshParameterByIp.keySet());
                                }
                            });
                        }).run(new WhileDoneCompletion(msg) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(multiErr(errorCodeList.getCauses(), "failed to check if management node is also compute node"));
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "check-ceph-plugin";

                    public boolean skip(Map data) {
                        return isEmpty(ipList);
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<CephPluginConnectionView> views = Collections.synchronizedList(new ArrayList<>());
                        new While<>(ipList).step((ip, whileCompletion) -> {
                            CephPluginConnectionView view = new CephPluginConnectionView();
                            view.setIp(ip);

                            CephPlugin selectedPlugin = null;

                            boolean isSkipCheckPackageInstalled = false;
                            boolean isPackageInstalled = false;

                            HostSshParameter hostSshParameter = sshParameterByIp.get(view.getIp());
                            if (hostSshParameter.skipCheckPackageInstalled()) {
                                isSkipCheckPackageInstalled = true;
                            }

                            if (!CoreGlobalProperty.UNIT_TEST_ON && !isSkipCheckPackageInstalled) {
                                for (CephPlugin cephPlugin : cephPlugins) {
                                    boolean installed = cephPlugin.isPackageInstalled(hostSshParameter);
                                    if (installed) {
                                        selectedPlugin = cephPlugin;
                                        view.setPluginType(cephPlugin.type());
                                        logger.debug(String.format("ceph plugin[%s] is installed on host[%s]", cephPlugin.type(), view.getIp()));
                                        isPackageInstalled = true;
                                        break;
                                    }
                                }
                            }

                            if (CoreGlobalProperty.UNIT_TEST_ON) {
                                isPackageInstalled = true;
                                isSkipCheckPackageInstalled = true;
                            }

                            if (isPackageInstalled) {
                                for (CephPlugin cephPlugin : cephPlugins) {
                                    if (!Objects.equals(cephPlugin.type(), view.getPluginType())) {
                                        continue;
                                    }
                                    boolean success = cephPlugin.testConnection(view.getIp());
                                    logger.debug(String.format("%s to connect ceph plugin[%s] on host[%s]",
                                            success ? "success" : "failed", cephPlugin.type(), view.getIp()));
                                    break;
                                }
                            }

                            if (isSkipCheckPackageInstalled) {
                                for (CephPlugin cephPlugin : cephPlugins) {
                                    boolean success = cephPlugin.testConnection(view.getIp());
                                    if (success) {
                                        selectedPlugin = cephPlugin;
                                        view.setPluginType(cephPlugin.type());
                                        break;
                                    }
                                }
                            }

                            if (selectedPlugin == null) {
                                whileCompletion.done();
                                return;
                            }

                            if (mnUuidByIp.containsKey(view.getIp())) {
                                view.setManagementNodeUuid(mnUuidByIp.get(view.getIp()));
                            }
                            if (hostUuidByIp.containsKey(view.getIp())) {
                                view.setHostUuid(hostUuidByIp.get(view.getIp()));
                            }

                            views.add(view);

                            selectedPlugin.loadProperties(view, new ReturnValueCompletion<Map<String, Object>>(whileCompletion) {
                                @Override
                                public void success(Map<String, Object> returnValue) {
                                    view.setPluginProperties(returnValue);
                                    whileCompletion.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    logger.warn(String.format("failed to load ceph[ip=%s].properties: %s", view.getIp(), errorCode));
                                    view.setPluginProperties(new HashMap<>());
                                    whileCompletion.done();
                                }
                            });
                        }, 3).run(new WhileDoneCompletion(msg) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                reply.setInventories(views);
                                trigger.next();
                            }
                        });
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ZsvStorageConstant.SERVICE_ID);
    }
}
