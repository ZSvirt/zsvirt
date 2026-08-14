package org.zstack.cloudformation.template;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.cloudformation.CloudFormationUtils;
import org.zstack.cloudformation.StackEventStatus;
import org.zstack.cloudformation.template.struct.CloudFormationConstants;
import org.zstack.cloudformation.template.struct.CloudFormationErrorCode;
import org.zstack.cloudformation.template.struct.DeleteData;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.cloudformation.CloudFormationStackEventVO;
import org.zstack.header.cloudformation.CloudFormationStackEventVO_;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.sdk.ErrorCode;
import org.zstack.sdk.NonAPIParam;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mingjian.deng on 2018/6/13.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CloudFormationDeleter {
    private static final CLogger logger = Utils.getLogger(CloudFormationDeleter.class);
    @Autowired
    private DatabaseFacade dbf;

    private List<DeleteData> datas = new ArrayList<>();
    private boolean dryRun = false;

    private void startDeleteRecord(Object deleteAction, String resourceName, String stackUuid) {
        CloudFormationStackEventVO vo = new CloudFormationStackEventVO();
        vo.setStackUuid(stackUuid);
        vo.setAction(deleteAction.getClass().getSimpleName());
        vo.setResourceName(resourceName);
        vo.setActionStatus(StackEventStatus.RollbackStart);
        vo.setContent(new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                return fieldAttributes.getAnnotation(NonAPIParam.class) != null;
            }

            @Override
            public boolean shouldSkipClass(Class<?> aClass) {
                return false;
            }
        }).setPrettyPrinting().create().toJson(deleteAction));
        if (Q.New(CloudFormationStackEventVO.class).eq(CloudFormationStackEventVO_.resourceName, vo.getResourceName())
                .eq(CloudFormationStackEventVO_.stackUuid, vo.getStackUuid()).eq(CloudFormationStackEventVO_.action, deleteAction.getClass().getSimpleName())
                .eq(CloudFormationStackEventVO_.actionStatus, StackEventStatus.RollbackStart).isExists()) {
            return;
        }
        dbf.persistAndRefresh(vo);
    }

    private void finishDeleteRecord(String deleteAction, String resourceName, String stackUuid) {
        CloudFormationStackEventVO vo = new CloudFormationStackEventVO();
        vo.setStackUuid(stackUuid);
        vo.setAction(deleteAction);
        vo.setResourceName(resourceName);
        vo.setActionStatus(StackEventStatus.RollbackFinish);
        vo.setContent("success");
        if (Q.New(CloudFormationStackEventVO.class).eq(CloudFormationStackEventVO_.resourceName, vo.getResourceName())
                .eq(CloudFormationStackEventVO_.stackUuid, vo.getStackUuid()).eq(CloudFormationStackEventVO_.action, deleteAction)
                .eq(CloudFormationStackEventVO_.actionStatus, StackEventStatus.RollbackFinish).isExists()) {
            return;
        }
        String duration = getDuration(resourceName, stackUuid, deleteAction);
        if (duration != null) {
            vo.setDuration(duration);
        }

        dbf.persistAndRefresh(vo);
    }

    private void failedDeleteRecord(String deleteAction, String resourceName, String stackUuid, String reason) {
        CloudFormationStackEventVO vo = new CloudFormationStackEventVO();
        vo.setStackUuid(stackUuid);
        vo.setAction(deleteAction);
        vo.setResourceName(resourceName);
        vo.setActionStatus(StackEventStatus.RollbackFailed);
        vo.setContent(reason);
        if (Q.New(CloudFormationStackEventVO.class).eq(CloudFormationStackEventVO_.resourceName, resourceName)
                .eq(CloudFormationStackEventVO_.stackUuid, vo.getStackUuid()).eq(CloudFormationStackEventVO_.action, deleteAction)
                .eq(CloudFormationStackEventVO_.actionStatus, StackEventStatus.RollbackFailed).isExists()) {
            return;
        }
        String duration = getDuration(resourceName, stackUuid, deleteAction);
        if (duration != null) {
            vo.setDuration(duration);
        }

        dbf.persistAndRefresh(vo);
    }

    private static String getDuration(String resourceName, String stackUuid, String action) {
        CloudFormationStackEventVO start = Q.New(CloudFormationStackEventVO.class).eq(CloudFormationStackEventVO_.resourceName, resourceName)
                .eq(CloudFormationStackEventVO_.stackUuid, stackUuid).eq(CloudFormationStackEventVO_.action, action)
                .eq(CloudFormationStackEventVO_.actionStatus, StackEventStatus.RollbackStart).find();
        if (start != null) {
            return CloudFormationUtils.getDuration(System.currentTimeMillis() - start.getCreateDate().getTime());
        }
        return null;
    }

    private void delete(String uuid, String resourceType, String resourceName, String sessionUuid, String stackUuid) {
        String deleteAction = resourceType + CloudFormationConstants.actionSuffix;
        String specific = CloudFormationConstants.resourceDelete.get(resourceType);
        if (specific != null) {
            deleteAction = specific + CloudFormationConstants.actionSuffix;
        } else {
            deleteAction = CloudFormationConstants.deletePrefix + deleteAction;
        }
        deleteAction = CloudFormationConstants.sdkPackage + deleteAction;

        String action = deleteAction.substring(deleteAction.lastIndexOf(".") + 1);

        try {
            Object ob = Class.forName(deleteAction).newInstance();
            Method call = ob.getClass().getMethod("call");
            // set session uuid
            Field field = ob.getClass().getDeclaredField("sessionId");
            field.setAccessible(true);
            field.set(ob, sessionUuid);
            // set uuid
            field = ob.getClass().getDeclaredField("uuid");
            field.setAccessible(true);
            field.set(ob, uuid);
            // set deleteMode
            field = ob.getClass().getDeclaredField("deleteMode");
            field.setAccessible(true);
            field.set(ob, "Enforcing");
            // delete resource
            startDeleteRecord(ob, resourceName, stackUuid);
            Object result = call.invoke(ob);
            Field err = result.getClass().getField("error");
            err.setAccessible(true);
            if (err.get(result) != null) {
                // delete failed
                ErrorCode errSdk = (ErrorCode)err.get(result);
                failedDeleteRecord(action, resourceName, stackUuid, errSdk.details);
            } else {
                finishDeleteRecord(action, resourceName, stackUuid);
            }
        } catch (NoSuchFieldException | ClassNotFoundException | InstantiationException
                | NoSuchMethodException | InvocationTargetException e) {
            String causeName;
            if (e.getCause() != null) {
                causeName = e.getCause().getClass().getName() + ":";
            } else {
                causeName = "";
            }
            String message = causeName + e.getMessage();
            logger.warn(message);
            failedDeleteRecord(action, resourceName, stackUuid, message);
        } catch (IllegalAccessException e) {
            failedDeleteRecord(action, resourceName, stackUuid, e.getMessage());
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    private void expunge(String uuid, String resourceType, String resourceName, String sessionUuid, String stackUuid) {
        String deleteAction;
        String specific = CloudFormationConstants.resourceExpunge.get(resourceType);
        if (specific != null) {
            deleteAction = CloudFormationConstants.sdkPackage + specific + CloudFormationConstants.actionSuffix;
        } else {
            deleteAction = CloudFormationConstants.sdkPackage + CloudFormationConstants.expungePrefix + resourceType + CloudFormationConstants.actionSuffix;
        }
        String action = deleteAction.substring(deleteAction.lastIndexOf(".") + 1);

        try {
            Object ob = Class.forName(deleteAction).newInstance();
            Method call = ob.getClass().getMethod("call");
            // set session uuid
            Field field = ob.getClass().getDeclaredField("sessionId");
            field.setAccessible(true);
            field.set(ob, sessionUuid);
            // set uuid
            String param = CloudFormationConstants.resourceExpungeParameter.get(resourceType);
            param = param == null ? "uuid" : param;
            field = ob.getClass().getDeclaredField(param);
            field.setAccessible(true);
            field.set(ob, uuid);
            // delete resource
            startDeleteRecord(ob, resourceName, stackUuid);
            Object result = call.invoke(ob);
            Field err = result.getClass().getField("error");
            err.setAccessible(true);
            if (err.get(result) != null) {
                // delete failed
                ErrorCode errSdk = (ErrorCode)err.get(result);
                failedDeleteRecord(action, resourceName, stackUuid, errSdk.details);
            } else {
                finishDeleteRecord(action, resourceName, stackUuid);
            }
        } catch (NoSuchFieldException | ClassNotFoundException | InstantiationException
                | NoSuchMethodException | InvocationTargetException e) {
            String causeName;
            if (e.getCause() != null) {
                causeName = e.getCause().getClass().getName() + ":";
            } else {
                causeName = "";
            }

            String message = causeName + e.getMessage();
            logger.warn(message);
            failedDeleteRecord(action, resourceName, stackUuid, message);
        } catch (IllegalAccessException e) {
            failedDeleteRecord(action, resourceName, stackUuid, e.getMessage());
            throw new CloudRuntimeException(e.getMessage());
        }
    }
    /**
     *
     * @param resourceType: resourceType with full package name
     * @return
     */
    private static String getResourceFromResourceType(String resourceType) {
        String inventory = resourceType.substring(resourceType.lastIndexOf(".") + 1);
        if (!inventory.contains("Inventory")) {
            throw new CloudRuntimeException("resourceType is valid!");
        }
        return inventory.replace("Inventory", "");
    }

    public void deleteResource(String uuid, String type, String resourceName, String sessionUuid, String stackUuid) {
        logger.debug(String.format("Start delete resource: %s, name: %s, type: %s", uuid, resourceName, type));
        if (!Q.New(ResourceVO.class).eq(ResourceVO_.uuid, uuid).isExists()) {
            logger.warn(String.format("Resource [%s] is already deleted, skip delete.", uuid));
            return;
        }
        String r = getResourceFromResourceType(type);
        logger.debug(String.format("deleted resource: %s, name: %s", uuid, resourceName));
        delete(uuid, r, resourceName, sessionUuid, stackUuid);
        if (CloudFormationConstants.expungeResources.contains(r)) {
            logger.debug(String.format("Start expunge resource: %s, name: %s", uuid, resourceName));
            if (!Q.New(ResourceVO.class).eq(ResourceVO_.uuid, uuid).isExists()) {
                logger.warn(String.format("Resource [%s] is already deleted, skip expunge.", uuid));
                return;
            }
            expunge(uuid, r, resourceName, sessionUuid, stackUuid);
            logger.debug(String.format("expunged resource: %s, name: %s", uuid, resourceName));
        }
    }

    private CloudFormationErrorCode delete() {
        for (DeleteData data: datas) {
            if (data.isReserve()) {
                continue;
            }
            if (!dryRun) {
                deleteResource(data.getResourceUuid(), data.getResourceType(), data.getResourceName(),
                        data.getSessionUuid(), data.getStackUuid());
            }
        }
        return new CloudFormationErrorCode();
    }

    public CloudFormationErrorCode deleteResource(List<DeleteData> datas) {
        logger.debug("start delete resources: ");
        this.datas = datas.stream().sorted(Comparator.comparing(DeleteData::getRound).reversed()).collect(Collectors.toList());
        return delete();
    }

    public CloudFormationErrorCode dryRunDeleteResource(List<DeleteData> datas) {
        logger.debug("start dry run delete resources: ");
        this.dryRun = true;
        this.datas = datas.stream().sorted(Comparator.comparing(DeleteData::getRound).reversed()).collect(Collectors.toList());
        return delete();
    }
}
