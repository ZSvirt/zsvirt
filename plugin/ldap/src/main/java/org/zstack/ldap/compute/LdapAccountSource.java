package org.zstack.ldap.compute;

import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.Message;
import org.zstack.identity.imports.header.SyncTaskResult;
import org.zstack.identity.imports.header.SyncTaskSpec;
import org.zstack.identity.imports.source.AbstractAccountSourceBase;
import org.zstack.ldap.LdapConstant;
import org.zstack.ldap.entity.LdapServerInventory;
import org.zstack.ldap.entity.LdapServerVO;
import org.zstack.ldap.header.LdapAccountSourceSpec;
import org.zstack.ldap.header.LdapSyncTaskSpec;
import org.zstack.ldap.message.UpdateLdapAccountSourceMsg;
import org.zstack.ldap.message.UpdateLdapAccountSourceReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Objects;

import static org.zstack.core.Platform.err;
import static org.zstack.identity.imports.AccountImportsManager.accountSourceQueueSyncSignature;
import static org.zstack.ldap.LdapErrors.UNABLE_TO_FIND_LDAP_SERVER;
import static org.zstack.ldap.LdapGlobalConfig.*;

/**
 * Created by Wenhao.Zhang on 2024/06/03
 */
public class LdapAccountSource extends AbstractAccountSourceBase {
    private static final CLogger logger = Utils.getLogger(LdapAccountSource.class);

    protected LdapAccountSource(LdapServerVO self) {
        super(self);
    }

    public LdapServerVO getSelf() {
        return (LdapServerVO) self;
    }

    private LdapServerVO refreshVO() {
        final LdapServerVO serverVO = databaseFacade.findByUuid(self.getUuid(), LdapServerVO.class);
        if (serverVO == null) {
            throw new OperationFailureException(
                    err(UNABLE_TO_FIND_LDAP_SERVER, "ldapServer[uuid=%s, name=%s] has been deleted",
                    self.getUuid(), self.getResourceName()));
        }
        self = serverVO;
        return serverVO;
    }

    @Override
    public String type() {
        return LdapConstant.LOGIN_TYPE;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof UpdateLdapAccountSourceMsg) {
            handle((UpdateLdapAccountSourceMsg) msg);
        } else {
            super.handleMessage(msg);
        }
    }

    private void handle(UpdateLdapAccountSourceMsg message) {
        UpdateLdapAccountSourceReply reply = new UpdateLdapAccountSourceReply();
        final String sourceUuid = message.getSourceUuid();

        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                final LdapServerInventory inventory = updateLdapAccountSource(message.getSpec());
                chain.next();
                reply.setInventory(inventory);
                bus.reply(message, reply);
            }

            @Override
            public String getSyncSignature() {
                return accountSourceQueueSyncSignature(sourceUuid);
            }

            @Override
            public String getName() {
                return "update-ldap-account-source-" + sourceUuid;
            }
        });
    }

    private LdapServerInventory updateLdapAccountSource(LdapAccountSourceSpec spec) {
        final LdapServerVO vo = refreshVO();

        if (spec.getServerName() != null) {
            vo.setResourceName(spec.getServerName());
        }
        if (spec.getDescription() != null) {
            vo.setDescription(spec.getDescription());
        }
        if (spec.getUrl() != null) {
            vo.setUrl(spec.getUrl());
        }
        if (spec.getBaseDn() != null) {
            vo.setBase(spec.getBaseDn());
        }
        if (spec.getLogInUserName() != null) {
            vo.setUsername(spec.getLogInUserName());
        }
        if (spec.getLogInPassword() != null) {
            vo.setPassword(spec.getLogInPassword());
        }
        if (spec.getEncryption() != null) {
            vo.setEncryption(spec.getEncryption().toString());
        }
        if (spec.getServerType() != null) {
            vo.setServerType(spec.getServerType());
        }
        if (spec.getCreateAccountStrategy() != null) {
            vo.setCreateAccountStrategy(spec.getCreateAccountStrategy());
        }
        if (spec.getDeleteAccountStrategy() != null) {
            vo.setDeleteAccountStrategy(spec.getDeleteAccountStrategy());
        }
        if (spec.getUsernameProperty() != null) {
            vo.setUsernameProperty(spec.getUsernameProperty());
        }
        if (spec.getFilter() != null) {
            vo.setFilter(spec.getFilter());
        }

        return LdapServerInventory.valueOf(databaseFacade.updateAndRefresh(vo));
    }

    @Override
    protected void syncAccountsFromSource(SyncTaskSpec spec, ReturnValueCompletion<SyncTaskResult> completion) {
        final LdapServerVO vo = getSelf();

        final LdapSyncTaskSpec ldapSpec = new LdapSyncTaskSpec(spec);
        ldapSpec.setFilter(vo.getFilter());
        ldapSpec.setUsernameProperty(vo.getUsernameProperty());
        ldapSpec.setServerType(vo.getServerType());
        ldapSpec.setMaxAccountCount(resourceConfigFacade.getResourceConfigValue(
                LDAP_MAXIMUM_SYNC_USERS, self.getUuid(), Integer.class));

        new LdapSyncHelper(ldapSpec).run(new ReturnValueCompletion<SyncTaskResult>(completion) {
            @Override
            public void success(SyncTaskResult result) {
                completion.success(result);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    protected void destroySource(Completion completion) {
        databaseFacade.removeByPrimaryKey(self.getUuid(), LdapServerVO.class);

        final String currentLdapUuid = CURRENT_LDAP_SERVER_UUID.value(String.class);
        if (Objects.equals(currentLdapUuid, self.getUuid())) {
            logger.debug(String.format("update GlobalConfig[%s] to NONE: LdapServer[uuid=%s] has been destroyed",
                    CURRENT_LDAP_SERVER_UUID.getName(), currentLdapUuid));
            CURRENT_LDAP_SERVER_UUID.updateValue(LdapConstant.CURRENT_LDAP_UUID_NONE);
        }

        completion.success();
    }
}
