package org.zstack.directory;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.utils.CharacterUtils;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

/**
 * @author shenjin
 * @date 2022/12/8 11:31
 */
@InterceptorForService("directory")
public class DirectoryApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DirectoryFactory factory;

    String regex = "^[\\u4e00-\\u9fa5a-zA-Z0-9\\s()（）【】@._+-]+$";
    //indicates the resource types that the directory allows to bind
    private static final List<String> ALLOW_RESOURCE_TYPES = asList(VmInstanceVO.class.getSimpleName());

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateDirectoryMsg) {
            validate((APICreateDirectoryMsg) msg);
        } else if (msg instanceof APIUpdateDirectoryMsg) {
            validate((APIUpdateDirectoryMsg) msg);
        } else if (msg instanceof APIAddResourcesToDirectoryMsg) {
            validate((APIAddResourcesToDirectoryMsg) msg);
        } else if (msg instanceof APIMoveResourcesToDirectoryMsg) {
            validate((APIMoveResourcesToDirectoryMsg) msg);
        } else if (msg instanceof APIRemoveResourcesFromDirectoryMsg) {
            validate((APIRemoveResourcesFromDirectoryMsg) msg);
        }
        setServiceId(msg);
        return msg;
    }

    private void validate(APIRemoveResourcesFromDirectoryMsg msg) {
        List<String> types = Q.New(ResourceVO.class).select(ResourceVO_.resourceType).in(ResourceVO_.uuid, msg.getResourceUuids()).listValues();
        types = types.stream().distinct().collect(Collectors.toList());
        checkType(types);
        for (String type : types) {
            DirectoryChecker checker = factory.directoryCheckers.get(type);
            ErrorCode result = checker.check(msg.getDirectoryUuid(), msg.getResourceUuids());
            if (result != null) {
                throw new ApiMessageInterceptionException(result);
            }
        }
    }

    private void validate(APIMoveResourcesToDirectoryMsg msg) {
        List<String> types = Q.New(ResourceVO.class).select(ResourceVO_.resourceType).in(ResourceVO_.uuid, msg.getResourceUuids()).listValues();
        types = types.stream().distinct().collect(Collectors.toList());
        checkType(types);
        for (String type : types) {
            DirectoryChecker checker = factory.directoryCheckers.get(type);
            ErrorCode result = checker.check(msg.getDirectoryUuid(), msg.getResourceUuids());
            if (result != null) {
                throw new ApiMessageInterceptionException(result);
            }
        }
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof OperateDirectoryMessage) {
            bus.makeTargetServiceIdByResourceUuid(msg, DirectoryManager.SERVICE_ID, DirectoryConstant.OPERATE_DIRECTORY_THREAD_NAME);
        } else if (msg instanceof DirectoryMessage) {
            DirectoryMessage dmsg = (DirectoryMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, DirectoryManager.SERVICE_ID, dmsg.getDirectoryUuid());
        }
    }

    private void validate(APIAddResourcesToDirectoryMsg msg) {
        List<String> types = Q.New(ResourceVO.class).select(ResourceVO_.resourceType).in(ResourceVO_.uuid, msg.getResourceUuids()).listValues();
        types = types.stream().distinct().collect(Collectors.toList());
        checkType(types);
        for (String type : types) {
            DirectoryChecker checker = factory.directoryCheckers.get(type);
            ErrorCode result = checker.check(msg.getDirectoryUuid(), msg.getResourceUuids());
            if (result != null) {
                throw new ApiMessageInterceptionException(result);
            }
        }
        List<String> resourceUuids = msg.getResourceUuids();
        List<ResourceDirectoryRefVO> resources = Q.New(ResourceDirectoryRefVO.class).in(ResourceDirectoryRefVO_.resourceUuid, resourceUuids).list();
        if (!resources.isEmpty()) {
            List<String> list = resources.stream().map(ResourceDirectoryRefVO::getResourceUuid).collect(Collectors.toList());
            throw new ApiMessageInterceptionException(argerr("resources %s has already been bound to directory uuid[%s] , multiple paths are not supported", list, msg.getDirectoryUuid()));
        }
    }

    private void checkType(List<String> types) {
        if (!ALLOW_RESOURCE_TYPES.containsAll(types)) {
            List<String> list = types.stream().filter(s -> !ALLOW_RESOURCE_TYPES.contains(s)).collect(Collectors.toList());
            throw new ApiMessageInterceptionException(argerr("resource types %s are not supported by directory, allowed types are %s", list, ALLOW_RESOURCE_TYPES));
        }
    }

    private void validate(APIUpdateDirectoryMsg msg) {
        boolean result = CharacterUtils.checkCharactersByRegex(regex, msg.getName());
        if (!result) {
            throw new ApiMessageInterceptionException(argerr("name contains unsupported characters," +
                    " name can only contain Chinese characters, English letters, " +
                    "numbers, spaces, and the following characters: ()（）【】@._-+ "));
        }
    }

    private void validate(APICreateDirectoryMsg msg) {
        //judge whether special characters are included
        boolean result = CharacterUtils.checkCharactersByRegex(regex, msg.getName());
        if (!result) {
            throw new ApiMessageInterceptionException(argerr("name contains unsupported characters," +
                    " name can only contain Chinese characters, English letters, " +
                    "numbers, spaces, and the following characters: ()（）【】@._-+ "));
        }
    }
}
