package org.zstack.sshkeypair;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.sshkeypair.*;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;

import java.sql.Timestamp;
import java.util.Date;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SshKeyPairBase {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;

    protected SshKeyPairVO self;

    public SshKeyPairBase(SshKeyPairVO self) {
        this.self = self;
    }

    @MessageSafe
    void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleAPIMessage((APIMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleAPIMessage(APIMessage msg) {
        if (msg instanceof APIUpdateSshKeyPairMsg) {
            handle((APIUpdateSshKeyPairMsg) msg);
        } else if (msg instanceof APIDeleteSshKeyPairMsg) {
            handle((APIDeleteSshKeyPairMsg) msg);
        } else if (msg instanceof APIAttachSshKeyPairToVmInstanceMsg) {
            handle((APIAttachSshKeyPairToVmInstanceMsg) msg);
        } else if (msg instanceof APIDetachSshKeyPairFromVmInstanceMsg) {
            handle((APIDetachSshKeyPairFromVmInstanceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    public void handle(APIUpdateSshKeyPairMsg msg) {
        APIUpdateSshKeyPairEvent event = new APIUpdateSshKeyPairEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                updateSshKeyPair(msg, event, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }
            @Override
            public String getSyncSignature() {
                return String.format("%s-%s", SshKeyPairConstant.OPERATE_SSH_KEY_PAIR_THREAD_NAME, msg.getSshKeyPairUuid());
            }

            @Override
            public String getName() {
                return String.format("update-sshKeyPair-%s", msg.getSshKeyPairUuid());
            }
        });
    }

    private void updateSshKeyPair(APIUpdateSshKeyPairMsg msg, APIUpdateSshKeyPairEvent event, Completion completion) {
        SshKeyPairVO vo = dbf.findByUuid(msg.getUuid(), SshKeyPairVO.class);

        boolean updated = false;

        if (msg.getName() != null) {
            vo.setName(msg.getName());
            updated = true;
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
            updated = true;
        }

        if (updated) {
            vo = dbf.updateAndRefresh(vo);
        }
        event.setInventory(SshKeyPairInventory.valueOf(vo));
        completion.success();
    }

    public void handle(APIDeleteSshKeyPairMsg msg) {
        APIDeleteSshKeyPairEvent event = new APIDeleteSshKeyPairEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                deleteSshKeyPair(msg, event, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }
            @Override
            public String getSyncSignature() {
                return String.format("%s-%s", SshKeyPairConstant.OPERATE_SSH_KEY_PAIR_THREAD_NAME, msg.getSshKeyPairUuid());
            }

            @Override
            public String getName() {
                return String.format("delete-sshKeyPair-%s", msg.getUuid());
            }
        });
    }

    private void deleteSshKeyPair(APIDeleteSshKeyPairMsg msg, APIDeleteSshKeyPairEvent event, Completion completion) {
        SshKeyPairVO vo = dbf.findByUuid(msg.getUuid(), SshKeyPairVO.class);

        dbf.remove(vo);

        completion.success();
    }

    public void handle(APIAttachSshKeyPairToVmInstanceMsg msg) {
        APIAttachSshKeyPairToVmInstanceEvent event = new APIAttachSshKeyPairToVmInstanceEvent(msg.getId());

        SshKeyPairVO keyPair = dbf.findByUuid(msg.getSshKeyPairUuid(), SshKeyPairVO.class);
        VmInstanceVO instance = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                AttachSshKeyPairToVmInstanceCommand cmd = new AttachSshKeyPairToVmInstanceCommand();
                cmd.setSshKeyPairUuid(keyPair.getUuid());
                cmd.setVmInstanceUuid(instance.getUuid());
                cmd.setPublicKey(keyPair.getPublicKey());

                KVMHostAsyncHttpCallMsg smsg = new KVMHostAsyncHttpCallMsg();
                smsg.setCommand(cmd);
                smsg.setHostUuid(instance.getHostUuid());
                smsg.setPath(SshKeyPairConstant.SSH_KEY_PAIR_ATTACH_TO_VM);

                bus.makeTargetServiceIdByResourceUuid(smsg, HostConstant.SERVICE_ID, instance.getHostUuid());
                bus.send(smsg, new CloudBusCallBack(chain) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            KVMHostAsyncHttpCallReply r = reply.castReply();
                            AttachSshKeyPairToVmInstanceRsp rsp = r.toResponse(AttachSshKeyPairToVmInstanceRsp.class);
                            if (rsp.isSuccess()) {
                                attachSshKeyPairToVmInDB();
                            } else {
                                reply.setError(operr("operation error, because: %s", rsp.getError()));
                                event.setError(reply.getError());
                            }
                        } else {
                            event.setError(reply.getError());
                        }
                        bus.publish(event);
                        chain.next();
                    }
                    void attachSshKeyPairToVmInDB() {
                        SshKeyPairRefVO vo = new SshKeyPairRefVO();
                        vo.setSshKeyPairUuid(msg.getSshKeyPairUuid());
                        vo.setResourceUuid(msg.getVmInstanceUuid());
                        vo.setResourceType(VmInstanceVO.class.getSimpleName());
                        vo.setCreateDate(new Timestamp(new Date().getTime()));
                        dbf.persist(vo);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("%s-%s", SshKeyPairConstant.OPERATE_SSH_KEY_PAIR_THREAD_NAME, msg.getSshKeyPairUuid());
            }

            @Override
            public String getName() {
                return String.format("attach-sshKeyPair-%s", msg.getSshKeyPairUuid());
            }
        });
    }

    public static class AttachSshKeyPairToVmInstanceCommand extends KVMAgentCommands.AgentCommand {
        public String sshKeyPairUuid;
        public String vmInstanceUuid;
        public String publicKey;

        public String getSshKeyPairUuid() {
            return sshKeyPairUuid;
        }

        public void setSshKeyPairUuid(String sshKeyPairUuid) {
            this.sshKeyPairUuid = sshKeyPairUuid;
        }

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }
    }

    public static class AttachSshKeyPairToVmInstanceRsp extends KVMAgentCommands.AgentResponse {
    }

    public void handle(APIDetachSshKeyPairFromVmInstanceMsg msg) {
        APIDetachSshKeyPairFromVmInstanceEvent event = new APIDetachSshKeyPairFromVmInstanceEvent(msg.getId());

        SshKeyPairVO keyPair = dbf.findByUuid(msg.getSshKeyPairUuid(), SshKeyPairVO.class);
        VmInstanceVO instance = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                DetachSshKeyPairFromVmInstanceCommand cmd = new DetachSshKeyPairFromVmInstanceCommand();
                cmd.setSshKeyPairUuid(keyPair.getUuid());
                cmd.setVmInstanceUuid(instance.getUuid());
                cmd.setPublicKey(keyPair.getPublicKey());

                KVMHostAsyncHttpCallMsg smsg = new KVMHostAsyncHttpCallMsg();
                smsg.setCommand(cmd);
                smsg.setHostUuid(instance.getHostUuid());
                smsg.setPath(SshKeyPairConstant.SSH_KEY_PAIR_DETACH_FROM_VM);

                bus.makeTargetServiceIdByResourceUuid(smsg, HostConstant.SERVICE_ID, instance.getHostUuid());
                bus.send(smsg, new CloudBusCallBack(chain) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            KVMHostAsyncHttpCallReply r = reply.castReply();
                            DetachSshKeyPairFromVmInstanceRsp rsp = r.toResponse(DetachSshKeyPairFromVmInstanceRsp.class);
                            if (rsp.isSuccess()) {
                                detachSshKeyPairFromVmInDB();
                            } else {
                                reply.setError(operr("operation error, because: %s", rsp.getError()));
                                event.setError(reply.getError());
                            }
                        } else {
                            event.setError(reply.getError());
                        }
                        bus.publish(event);
                        chain.next();
                    }
                    void detachSshKeyPairFromVmInDB() {
                        SQL.New(SshKeyPairRefVO.class)
                                .eq(SshKeyPairRefVO_.sshKeyPairUuid, msg.getSshKeyPairUuid())
                                .eq(SshKeyPairRefVO_.resourceUuid, msg.getVmInstanceUuid())
                                .eq(SshKeyPairRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                                .delete();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("%s-%s", SshKeyPairConstant.OPERATE_SSH_KEY_PAIR_THREAD_NAME, msg.getSshKeyPairUuid());
            }

            @Override
            public String getName() {
                return String.format("detach-sshKeyPair-%s", msg.getSshKeyPairUuid());
            }
        });
    }

    public static class DetachSshKeyPairFromVmInstanceCommand extends KVMAgentCommands.AgentCommand {
        public String sshKeyPairUuid;
        public String vmInstanceUuid;
        public String publicKey;

        public String getSshKeyPairUuid() {
            return sshKeyPairUuid;
        }

        public void setSshKeyPairUuid(String sshKeyPairUuid) {
            this.sshKeyPairUuid = sshKeyPairUuid;
        }

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }
    }

    public static class DetachSshKeyPairFromVmInstanceRsp extends KVMAgentCommands.AgentResponse {
    }
}
