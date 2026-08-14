package org.zstack.cloudformation;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.cloudformation.template.CloudFormationDeleter;
import org.zstack.cloudformation.template.struct.DeleteData;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.cloudformation.CloudFormationStackResourceRefVO;
import org.zstack.header.cloudformation.CloudFormationStackResourceRefVO_;
import org.zstack.header.cloudformation.ResourceStackInventory;
import org.zstack.header.cloudformation.ResourceStackVO;
import org.zstack.header.core.Completion;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/6/11.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CloudFormationCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(CloudFormationCascadeExtension.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    private static final String NAME = ResourceStackVO.class.getSimpleName();

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            completion.success();
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            completion.success();
        } else {
            completion.success();
        }
    }

    private void deleteResourcesInStack(List<CloudFormationStackResourceRefVO> refs, SessionInventory session) {
        List<DeleteData> datas = new ArrayList<>();
        for (CloudFormationStackResourceRefVO ref: refs) {
            DeleteData data = new DeleteData();
            String resourceName = Q.New(ResourceVO.class).eq(ResourceVO_.uuid, ref.getResourceUuid()).select(ResourceVO_.resourceName).findValue();
            data.setRound(ref.getRound());
            data.setReserve(ref.getReserve());
            data.setResourceUuid(ref.getResourceUuid());
            data.setResourceType(ref.getResourceType());
            data.setResourceName(resourceName);
            data.setSessionUuid(session.getUuid());
            data.setStackUuid(ref.getStackUuid());
            datas.add(data);
        }
        CloudFormationDeleter deleter = new CloudFormationDeleter();
        if (!datas.isEmpty()) {
            deleter.deleteResource(datas);
        }
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<ResourceStackInventory> stacks = stackFromAction(action);
        for (ResourceStackInventory stack : stacks) {
            List<CloudFormationStackResourceRefVO> refs = Q.New(CloudFormationStackResourceRefVO.class).
                    eq(CloudFormationStackResourceRefVO_.stackUuid, stack.getUuid()).list();
            deleteResourcesInStack(refs, ((CFCascadeAction)action).getSession());
            //TODO: should be cascade instead of it
            for (ClousFormationTemplateExtensionPoint exp: pluginRgty.getExtensionList(ClousFormationTemplateExtensionPoint.class)) {
                exp.afterDeleteResourceStack(stack);
            }
            dbf.removeByPrimaryKey(stack.getUuid(), ResourceStackVO.class);
        }
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Collections.emptyList();
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<ResourceStackInventory> ctx = stackFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }
        return null;
    }

    private void addStacks(List<ResourceStackInventory> list, AccountInventory account) {
        String sql = "select r from ResourceStackVO r, AccountResourceRefVO a where r.uuid = a.resourceUuid and" +
                " a.resourceType = ResourceStackVO and a.type = :type and a.accountUuid = (:auuid)";
        List<ResourceStackVO> vos = SQL.New(sql)
                .param("auuid", account.getUuid())
                .param("type", AccessLevel.Own)
                .list();
        if (!vos.isEmpty()) {
            list.addAll(ResourceStackInventory.valueOf(vos));
        }
    }

    private List<ResourceStackInventory> stackFromAction(CascadeAction action) {
        final List<ResourceStackInventory> list = new ArrayList<>();
        if (NAME.equals(action.getParentIssuer())) {
            list.addAll(action.getParentIssuerContext());
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<AccountInventory> accounts = action.getParentIssuerContext();
            if (accounts != null) {
                accounts.forEach(account -> addStacks(list, account));
            }
        }
        return list;
    }
}
