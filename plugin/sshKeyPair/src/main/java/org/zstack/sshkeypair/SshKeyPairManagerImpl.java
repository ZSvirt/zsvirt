package org.zstack.sshkeypair;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.vm.SshKeyPairAssociateExtensionPoint;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.identity.APIChangeResourceOwnerMsg;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.sshkeypair.*;
import org.zstack.header.sshkeypair.quota.SshKeyPairQuotaConstant;
import org.zstack.header.sshkeypair.quota.SshKeyPairQuotaDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

public class SshKeyPairManagerImpl extends AbstractService implements
        SshKeyPairManager,
        ReportQuotaExtensionPoint,
        SshKeyPairAssociateExtensionPoint,
        Component  {
    private static final CLogger logger = Utils.getLogger(SshKeyPairManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private TagManager tagMgr;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof SshKeyPairMessage) {
            passThrough((SshKeyPairMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleAPIMessage((APIMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    public void handleAPIMessage(APIMessage msg) {
        if (msg instanceof APICreateSshKeyPairMsg) {
            handle((APICreateSshKeyPairMsg) msg);
        } else if (msg instanceof APIGenerateSshKeyPairMsg) {
            handle((APIGenerateSshKeyPairMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    public void passThrough(SshKeyPairMessage msg) {
        SshKeyPairVO vo = dbf.findByUuid(msg.getSshKeyPairUuid(), SshKeyPairVO.class);
        if (vo == null) {
            bus.replyErrorByMessageType((Message) msg, err(SysErrors.RESOURCE_NOT_FOUND, "unable to find sshKeyPair[uuid=%s]", msg.getSshKeyPairUuid()));
            return;
        }

        new SshKeyPairBase(vo).handleMessage((Message) msg);
    }

    public void handle(APICreateSshKeyPairMsg msg){
        APICreateSshKeyPairEvent event = new APICreateSshKeyPairEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                createSshKeyPair(msg, event, new Completion(chain) {
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
                return String.format("%s-%s", SshKeyPairConstant.OPERATE_SSH_KEY_PAIR_THREAD_NAME, msg.getName());
            }

            @Override
            public String getName() {
                return String.format("create-sshKeyPair-name-%s", msg.getName());
            }
        });
    }


    public void handle(APIGenerateSshKeyPairMsg msg) {
        APIGenerateSshKeyPairReply reply = new APIGenerateSshKeyPairReply();

        ByteArrayOutputStream privKey = new ByteArrayOutputStream();
        ByteArrayOutputStream pubKey = new ByteArrayOutputStream();
        try {
            JSch jsch = new JSch();
            KeyPair jschKeyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 2048);

            jschKeyPair.writePrivateKey(privKey);
            jschKeyPair.writePublicKey(pubKey, null);
        } catch (JSchException e) {
            bus.replyErrorByMessageType((Message) msg, err(
                    SysErrors.INTERNAL,
                    "Cannot generate sshKeyPair, error: %s", e.toString()));
        }

        SshKeyPairVO keyPair = new SshKeyPairVO();
        keyPair.setUuid(Platform.getUuid());
        keyPair.setName(msg.getName());
        keyPair.setDescription(msg.getDescription());
        keyPair.setPublicKey(pubKey.toString());
        keyPair.setAccountUuid(msg.getSession().getAccountUuid());
        keyPair.setCreateDate(new Timestamp(new Date().getTime()));
        keyPair = dbf.persistAndRefresh(keyPair);

        tagMgr.createTags(msg.getSystemTags(), msg.getUserTags(), keyPair.getUuid(), SshKeyPairVO.class.getSimpleName());

        SshPrivateKeyPairInventory inv = SshPrivateKeyPairInventory.valueOf(keyPair);
        inv.setPrivateKey(privKey.toString());

        reply.setInventory(inv);
        bus.reply(msg, reply);
    }

    public void createSshKeyPair(APICreateSshKeyPairMsg msg, APICreateSshKeyPairEvent event, Completion completion){
        String keyContent = msg.getPublicKey();
        logger.warn(keyContent);
        try {
            JSch jsch = new JSch();
            KeyPair publicKey = KeyPair.load(jsch, null, keyContent.getBytes());
            String fg = publicKey.getFingerPrint();
        } catch (Exception e) {
            completion.fail(operr("failed to load the public key: %s, err: %s", keyContent, e.toString()));
            return;
        }

        SshKeyPairVO keyPair = new SshKeyPairVO();
        keyPair.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        keyPair.setName(msg.getName());
        keyPair.setDescription(msg.getDescription());
        keyPair.setPublicKey(msg.getPublicKey());
        keyPair.setAccountUuid(msg.getSession().getAccountUuid());
        keyPair.setCreateDate(new Timestamp(new Date().getTime()));
        keyPair = dbf.persistAndRefresh(keyPair);

        tagMgr.createTagsFromAPICreateMessage(msg, keyPair.getUuid(), VolumeVO.class.getSimpleName());

        event.setInventory(SshKeyPairInventory.valueOf(keyPair));
        completion.success();
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SshKeyPairManager.SERVICE_ID);
    }

    @Override
    public List<Quota> reportQuota() {
        Quota quota = new Quota();
        quota.defineQuota(new SshKeyPairQuotaDefinition());
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateSshKeyPairMsg.class).
                addCounterQuota(SshKeyPairQuotaConstant.SSH_KEY_PAIR_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIGenerateSshKeyPairMsg.class).
                addCounterQuota(SshKeyPairQuotaConstant.SSH_KEY_PAIR_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(SshKeyPairVO.class)
                        .eq(SshKeyPairVO_.uuid, msg.getResourceUuid())
                        .isExists())
                .addCounterQuota(SshKeyPairQuotaConstant.SSH_KEY_PAIR_NUM));
        return list(quota);
    }

    @Override
    public ErrorCode associateSshKeyPair(String vmUuid, List<String> keyPairUuids) {
        for (String uuid: keyPairUuids) {
            boolean isExist = Q.New(SshKeyPairVO.class)
                    .eq(SshKeyPairVO_.uuid, uuid)
                    .isExists();
            if(!isExist) {
                return operr("ssh key pair[uuid:%s] can not associated to vm[uuid:%s] due to the key not found",
                        uuid, vmUuid);
            }
            SshKeyPairRefVO refVo = new SshKeyPairRefVO();
            refVo.setSshKeyPairUuid(uuid);
            refVo.setResourceUuid(vmUuid);
            refVo.setResourceType(VmInstanceVO.class.getSimpleName());
            refVo.setCreateDate(new Timestamp(new Date().getTime()));
            dbf.persist(refVo);
        }
        return null;
    }

    @Override
    public List<String> fetchAssociatedSshKeyPairs(String vmUuid) {
        List<String> sshKeyPairs = new ArrayList<>();
        List<String> uuids = Q.New(SshKeyPairRefVO.class)
                .eq(SshKeyPairRefVO_.resourceUuid, vmUuid)
                .eq(SshKeyPairRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .select(SshKeyPairRefVO_.sshKeyPairUuid)
                .listValues();
        if (!uuids.isEmpty()) {
         sshKeyPairs = Q.New(SshKeyPairVO.class)
                .in(SshKeyPairVO_.uuid, uuids)
                .select(SshKeyPairVO_.publicKey)
                .listValues();
        }
        return sshKeyPairs;
    }

    @Override
    public void cloneSshKeyPairsToVm(String originVmUuid, String destVmUuid) {
        List<String> sshKeyPairUuids = Q.New(SshKeyPairRefVO.class)
                .select(SshKeyPairRefVO_.sshKeyPairUuid)
                .eq(SshKeyPairRefVO_.resourceUuid, originVmUuid)
                .eq(SshKeyPairRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .listValues();
        for (String sshKeyPairUuid: sshKeyPairUuids) {
            SshKeyPairRefVO refVo = new SshKeyPairRefVO();
            refVo.setSshKeyPairUuid(sshKeyPairUuid);
            refVo.setResourceUuid(destVmUuid);
            refVo.setResourceType(VmInstanceVO.class.getSimpleName());
            refVo.setCreateDate(new Timestamp(new Date().getTime()));
            dbf.persist(refVo);
        }
    }
}
