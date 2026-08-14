package org.zstack.tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.tag.*;
import org.zstack.identity.QuotaUtil;
import org.zstack.identity.ResourceHelper;

import javax.persistence.TypedQuery;

import static org.zstack.core.Platform.*;

/**
 */
@InterceptorForService("tag")
public class TagApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeleteTagMsg) {
            validate((APIDeleteTagMsg) msg);
        } else if (msg instanceof APIAbstractCreateTagMsg) {
            validate((APIAbstractCreateTagMsg) msg);
        } else if (msg instanceof APIUpdateSystemTagMsg) {
            validate((APIUpdateSystemTagMsg) msg);
        } else if (msg instanceof APICreateSystemTagsMsg) {
            validate((APICreateSystemTagsMsg) msg);
        }

        return msg;
    }

    private void validate(APICreateSystemTagsMsg msg) {
        if (!tagMgr.getManagedEntityNames().contains(msg.getResourceType())) {
            throw new ApiMessageInterceptionException(argerr("no resource type[%s] found in tag system", msg.getResourceType()));
        }

        for (String tag : msg.getTags()) {
            // early return if new system tag match resource config
            if (tagMgr.isResourceConfigSystemTag(tag)) {
                return;
            }

            try {
                tagMgr.validateSystemTag(msg.getResourceUuid(), msg.getResourceType(), tag);
            } catch (OperationFailureException oe) {
                throw new ApiMessageInterceptionException(oe.getErrorCode());
            }
            checkIfResourceHasThisTagType(msg.getResourceUuid(), msg.getResourceType());
        }

    }

    private void validate(APIUpdateSystemTagMsg msg) {
        SystemTagVO vo = dbf.findByUuid(msg.getUuid(), SystemTagVO.class);
        try {
            tagMgr.validateSystemTag(vo.getResourceUuid(), vo.getResourceType(), msg.getTag());
        } catch (OperationFailureException oe) {
            throw new ApiMessageInterceptionException(oe.getErrorCode());
        }
    }


    private void validate(APIAbstractCreateTagMsg msg) {
        if (!tagMgr.getManagedEntityNames().contains(msg.getResourceType())) {
            throw new ApiMessageInterceptionException(argerr("no resource type[%s] found in tag system", msg.getResourceType()));
        }

        if (msg instanceof APICreateSystemTagMsg) {
            // early return if new system tag match resource config
            if (tagMgr.isResourceConfigSystemTag(msg.getTag())) {
                return;
            }

            try {
                tagMgr.validateSystemTag(msg.getResourceUuid(), msg.getResourceType(), msg.getTag());
            } catch (OperationFailureException oe) {
                throw new ApiMessageInterceptionException(oe.getErrorCode());
            }
            checkIfResourceHasThisTagType(msg.getResourceUuid(), msg.getResourceType());
        }
    }

    private void validate(APIDeleteTagMsg msg) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.add(SystemTagVO_.uuid, Op.EQ, msg.getUuid());
        q.add(SystemTagVO_.inherent, Op.EQ, true);
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("tag[uuid:%s] is an inherent system tag, can not be removed", msg.getUuid()));
        }

        boolean userTag = dbf.isExist(msg.getUuid(), UserTagVO.class);
        boolean sysTag = dbf.isExist(msg.getUuid(), SystemTagVO.class);
        boolean tagPattern = dbf.isExist(msg.getUuid(), TagPatternVO.class);
        if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
            if (userTag) {
                checkAccountForUserTag(msg);
            } else if (sysTag) {
                checkAccountForSystemTag(msg);
            } else if (tagPattern) {
                checkAccountForTagPattern(msg);
            }
        }
    }

    @Transactional(readOnly = true)
    private void checkIfResourceHasThisTagType(String resourceUuid, String resourceType) {
        String sql = String.format("select count(vo.uuid) from %s vo where " +
                " vo.uuid = :resourceUuid", resourceType);
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("resourceUuid", resourceUuid);

        Long size = q.getSingleResult();
        if (size <= 0) {
            throw new ApiMessageInterceptionException(argerr("The argument :'resourceType' doesn't match uuid"));
        }

    }

    @Transactional(readOnly = true)
    private void checkAccountForSystemTag(APIDeleteTagMsg msg) {
        String accountUuid = SystemTagUtils.findSystemTagOwner(msg.getUuid());
        if (!msg.getSession().getAccountUuid().equals(accountUuid)) {
            throw new ApiMessageInterceptionException(err(IdentityErrors.PERMISSION_DENIED,
                    "permission denied. The system tag[uuid: %s] refer to a resource not belonging to the account[uuid: %s]",
                    msg.getUuid(), msg.getSession().getAccountUuid()
            ));
        }
    }

    @Transactional(readOnly = true)
    private void checkAccountForUserTag(APIDeleteTagMsg msg) {
        String accountUuid = SystemTagUtils.findUserTagOwner(msg.getUuid());
        if (!msg.getSession().getAccountUuid().equals(accountUuid)) {
            throw new ApiMessageInterceptionException(err(IdentityErrors.PERMISSION_DENIED,
                    "permission denied. The user tag[uuid: %s] refer to a resource not belonging to the account[uuid: %s]",
                    msg.getUuid(), msg.getSession().getAccountUuid()
            ));
        }
    }

    private void checkAccountForTagPattern(APIDeleteTagMsg msg) {
        String accountUuid = ResourceHelper.findResourceOwner(msg.getUuid());
        if (!msg.getSession().getAccountUuid().equals(accountUuid)) {
            throw new ApiMessageInterceptionException(err(IdentityErrors.PERMISSION_DENIED,
                    "permission denied. The tag pattern[uuid: %s] refer to a resource not belonging to the account[uuid: %s]",
                    msg.getUuid(), msg.getSession().getAccountUuid()
            ));
        }
    }
}
