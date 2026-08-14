package org.zstack.cloudformation;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.cloudformation.*;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.AccountManager;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.argerr;

/**
 * Created by mingjian.deng on 2018/6/5.
 */
@InterceptorForService("cloudformation")
public class CloudFormationInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(CloudFormationInterceptor.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acmgr;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddStackTemplateMsg) {
            validate((APIAddStackTemplateMsg) msg);
        } else if (msg instanceof APIPreviewResourceStackMsg) {
            validate((APIPreviewResourceStackMsg) msg);
        } else if (msg instanceof APICreateResourceStackMsg) {
            validate((APICreateResourceStackMsg) msg);
        } else if (msg instanceof APIUpdateStackTemplateMsg) {
            validate((APIUpdateStackTemplateMsg) msg);
        } else if (msg instanceof APIUpdateResourceStackMsg) {
            validate((APIUpdateResourceStackMsg) msg);
        } else if (msg instanceof APICheckStackTemplateParametersMsg) {
            validate((APICheckStackTemplateParametersMsg) msg);
        } else if (msg instanceof APIRestartResourceStackMsg) {
            validate((APIRestartResourceStackMsg) msg);
        } else if (msg instanceof APIGetSupportedCloudFormationResourcesMsg) {
            validate((APIGetSupportedCloudFormationResourcesMsg) msg);
        } else if (msg instanceof APIDecodeStackTemplateMsg) {
            validate((APIDecodeStackTemplateMsg) msg);
        }
        return msg;
    }

    private void validate(final APIGetSupportedCloudFormationResourcesMsg msg) {
        if (msg.getVersion() == null) {
            msg.setVersion(ResourceStackVersion.v1.name());
        }
    }

    private void validate(final APIRestartResourceStackMsg msg) {
        List<ResourceStackStatus> validStatus = CollectionDSL.list(ResourceStackStatus.Failed, ResourceStackStatus.Rollbacked);
        ResourceStackVO vo = dbf.findByUuid(msg.getUuid(), ResourceStackVO.class);
        if (vo == null) {
            throw new ApiMessageInterceptionException(argerr("cannot find such ResourceStackVO by uuid [%s]", msg.getUuid()));
        }

        if (!validStatus.contains(vo.getStatus())) {
            throw new ApiMessageInterceptionException(argerr("restart resource stack only support %s status!", validStatus));
        }
    }

    private void validate(final APICheckStackTemplateParametersMsg msg) {
        if (msg.getTemplateContent() == null && msg.getUuid() == null) {
            throw new ApiMessageInterceptionException(argerr("templateContent and uuid mustn't both be empty or both be set!"));
        }

        if (msg.getTemplateContent() != null && msg.getUuid() != null) {
            throw new ApiMessageInterceptionException(argerr("templateContent and uuid mustn't both be empty or both be set!"));
        }

        if (msg.getTemplateContent() != null) {
            CloudFormationUtils.validateTemplate(msg.getTemplateContent(), false);
        }

        if (msg.getUuid() != null) {
            StackTemplateVO vo = dbf.findByUuid(msg.getUuid(), StackTemplateVO.class);
            CloudFormationUtils.validateTemplate(vo.getContent(), false);
        }
    }

    private void validate(final APIUpdateResourceStackMsg msg) {
        if (msg.getTemplateContent() == null && msg.getParameters() == null) {
            return;
        }
        ResourceStackVO vo = dbf.findByUuid(msg.getUuid(), ResourceStackVO.class);
        List<ResourceStackStatus> validStatus = CollectionDSL.list(ResourceStackStatus.Deleted, ResourceStackStatus.Failed, ResourceStackStatus.Rollbacked);
        if (msg.getParameters() != null || msg.getTemplateContent() != null) {
            if (!validStatus.contains(vo.getStatus())) {
                throw new ApiMessageInterceptionException(argerr("expect %s status!", validStatus));
            }
        }

        if (msg.getTemplateContent() != null && msg.getParameters() != null) {
            CloudFormationUtils.validateTemplate(msg.getTemplateContent(), msg.getParameters());
        } else if (msg.getTemplateContent() != null && msg.getParameters() == null) {
            CloudFormationUtils.validateTemplate(msg.getTemplateContent(), vo.getParamContent());
        } else if (msg.getTemplateContent() == null && msg.getParameters() != null) {
            CloudFormationUtils.validateTemplate(vo.getTemplateContent(), msg.getParameters());
        }
    }

    private void validate(final APICreateResourceStackMsg msg) {
        if (msg.getTemplateContent() != null) {
            CloudFormationUtils.validateTemplate(msg.getTemplateContent(), msg.getParameters());
        } else if (msg.getTemplateUuid() != null) {
            StackTemplateVO template = Q.New(StackTemplateVO.class).eq(StackTemplateVO_.uuid, msg.getTemplateUuid()).find();
            CloudFormationUtils.validateTemplate(template.getContent(), msg.getParameters());
        } else {
            throw new ApiMessageInterceptionException(argerr("templateContent and templateUuid mustn't both be empty!"));
        }
    }

    private void validate(final APIAddStackTemplateMsg msg) {
        CloudFormationUtils.validateTemplate(msg.getTemplateContent(), false);
        if (msg.getTemplateContent() == null && msg.getUrl() == null) {
            throw new ApiMessageInterceptionException(argerr("templateContent and url mustn't both be empty or both be set!"));
        }

        if (msg.getTemplateContent() != null && msg.getUrl() != null) {
            throw new ApiMessageInterceptionException(argerr("templateContent and url mustn't both be empty or both be set!"));
        }
    }

    private void validate(final APIUpdateStackTemplateMsg msg) {
        if (msg.getTemplateContent() != null) {
            CloudFormationUtils.validateTemplate(msg.getTemplateContent(), false);
        }
        List<String> tags = CloudFormationSystemTags.SYSTEM_TEMPLATE.getTags(msg.getUuid(), StackTemplateVO.class);
        // system template
        if (tags != null && tags.contains(CloudFormationSystemTags.SYSTEM_TEMPLATE_TOKEN)) {
            if (msg.getTemplateContent() != null || msg.getName() != null || msg.getDescription() != null) {
                throw new ApiMessageInterceptionException(argerr("cannot delete or update system template: %s", msg.getUuid()));
            }

            if (msg.getState() != null && !acmgr.isAdmin(msg.getSession())) {
                throw new ApiMessageInterceptionException(argerr("only admin could enable/disable system StackTemplate"));
            }
        }
    }

    private void validate(final APIPreviewResourceStackMsg msg) {
        if (msg.getTemplateContent() == null && msg.getUuid() == null) {
            throw new ApiMessageInterceptionException(argerr("templateContent and uuid mustn't both be empty or both be set!"));
        }

        if (msg.getTemplateContent() != null && msg.getUuid() != null) {
            throw new ApiMessageInterceptionException(argerr("templateContent and uuid mustn't both be empty or both be set!"));
        }

        if (msg.getTemplateContent() != null) {
            CloudFormationUtils.validateTemplate(msg.getTemplateContent(), msg.getParameters(), msg.getPreParameters());
        } else if (msg.getUuid() != null) {
            StackTemplateVO vo = dbf.findByUuid(msg.getUuid(), StackTemplateVO.class);
            CloudFormationUtils.validateTemplate(vo.getContent(), msg.getParameters(), msg.getPreParameters());
        }
    }

    private void validate(final APIDecodeStackTemplateMsg msg) {
        if (msg.getTemplateContent() == null && msg.getUuid() == null) {
            throw new ApiMessageInterceptionException(argerr("templateContent and uuid mustn't both be empty or both be set!"));
        }

        if (msg.getTemplateContent() != null && msg.getUuid() != null) {
            throw new ApiMessageInterceptionException(argerr("templateContent and uuid mustn't both be empty or both be set!"));
        }

        if (msg.getTemplateContent() != null) {
            CloudFormationUtils.validateTemplate(msg.getTemplateContent(), msg.getParameters(), msg.getPreparameters());
        } else if (msg.getUuid() != null) {
            StackTemplateVO vo = dbf.findByUuid(msg.getUuid(), StackTemplateVO.class);
            CloudFormationUtils.validateTemplate(vo.getContent(), msg.getParameters(), msg.getPreparameters());
        }
    }
}
