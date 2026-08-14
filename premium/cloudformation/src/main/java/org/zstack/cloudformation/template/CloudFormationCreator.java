package org.zstack.cloudformation.template;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LazilyParsedNumber;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.cloudformation.CloudFormationErrors;
import org.zstack.cloudformation.CloudFormationExtensionPoint;
import org.zstack.cloudformation.CloudFormationUtils;
import org.zstack.cloudformation.StackEventStatus;
import org.zstack.cloudformation.template.struct.ActionStruct;
import org.zstack.cloudformation.template.struct.CfnActions;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.cloudformation.template.struct.CloudFormationConstants;
import org.zstack.cloudformation.template.struct.CloudFormationErrorCode;
import org.zstack.cloudformation.template.struct.OutputStruct;
import org.zstack.cloudformation.template.struct.ResourceStruct;
import org.zstack.cloudformation.template.struct.ResourceType;
import org.zstack.cloudformation.template.struct.ResultStruct;
import org.zstack.cloudformation.template.struct.StackData;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import static org.zstack.core.Platform.operr;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.Q;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.header.cloudformation.CloudFormationStackEventVO;
import org.zstack.header.cloudformation.CloudFormationStackEventVO_;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeType;
import org.zstack.sdk.AbstractAction;
import org.zstack.sdk.ApiException;
import org.zstack.sdk.ErrorCode;
import org.zstack.sdk.SourceClassMap;
import org.zstack.sdk.VolumeInventory;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by mingjian.deng on 2018/5/31.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CloudFormationCreator {
    private static final CLogger logger = Utils.getLogger(CloudFormationCreator.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private EventFacade evtf;

    private Stack<ResultStruct> created = new Stack<>();
    private CfnActions actions = new CfnActions();
    private List<ResultStruct> rollbackList = new ArrayList<>();
    private int round = 0;
    private StackData stackData;
    private boolean dryRun = false;

    private Set<ResultStruct> vpcs = new HashSet<>();

    private GLock tlock;

    public Object replace(String value, List<ResourceStruct> resources) {
        if (value.contains("${") && value.contains("}")) {
            List<String> needToReplaces = split(value);
            if (needToReplaces.size() == 1 && needToReplaces.get(0).equals(value)) {
                Object o = replaceDynamics(value, resources);
                if (o == null) {
                    throw new OperationFailureException(operr("cannot find resource of properties set before!"));
                }
                return o;
            } else {
                for (String needToReplace: needToReplaces) {
                    String tmp = (String)replaceDynamics(needToReplace, resources);
                    value = value.replace(needToReplace, tmp);
                }
                return value;
            }
        }
        return null;
    }

    private static List<String> split(String value) {
        List<String> result = new ArrayList<>();
        Stack<Integer> stack = new Stack<>();
        char[] bytes = value.toCharArray();
        int start = 0;
        for (; start + 1 < bytes.length; start ++) {
            Character c = bytes[start];
            Character c1 = bytes[start + 1];
            if (c.toString().equals("$") && c1.toString().equals("{")) {
                stack.push(start);
                for (int current = start + 1; current < bytes.length; current ++) {
                    Character b = bytes[current];
                    if (b.toString().equals("}")) {
                        int tmp = stack.pop();
                        if (stack.empty()) {
                            result.add(value.substring(tmp, current + 1));
                            start = current;
                            break;
                        }
                    } else if (b.toString().equals("$")){
                        if (current + 1 < bytes.length) {
                            Character b1 = bytes[current + 1];
                            if (b1.toString().equals("{")) {
                                stack.push(current);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private Object replaceDynamics(String value, List<ResourceStruct> resources) {
        logger.debug("replaceDynamics value: " + value);
        if (!value.contains("::")) {
            // in this condition, value format like ${xxx}, this isn't zstack's verb, but set by user
            //TODO: we don't support value format like ${key::value} set by user, not set by zstack
            return value;
        }
        if (appearNumber(value, "\\$\\{") == 0) {
            throw new OperationFailureException(operr("invalid dynamic variables, which must contained ${: %s", value));
        }
        if (appearNumber(value, "\\$\\{") == 1) {
            String first = value.substring(0, value.indexOf("${"));
            String left = value.substring(value.lastIndexOf("}") + 1, value.length());
            if (first.equals("") && left.equals("")) {
                return replaceValue(value.substring(value.indexOf("${") + 2, value.lastIndexOf("}")), null, resources);
            } else {
                return first + replaceValue(value.substring(value.indexOf("${") + 2, value.lastIndexOf("}")), null, resources) + left;
            }
        }
        String dynamaic = value.substring(value.indexOf("${") + 2, value.lastIndexOf("}"));

        String t2 = dynamaic.substring(dynamaic.lastIndexOf(CloudFormationConstants.split) + 2, dynamaic.length());
        Object last = replaceDynamics(dynamaic.substring(0, dynamaic.lastIndexOf(CloudFormationConstants.split)), resources);
        return replaceValue(last.getClass().getSimpleName() + CloudFormationConstants.split + t2, last, resources);
    }

    private Object replaceValue(String value, Object last, List<ResourceStruct> resources) {
        logger.debug("replaceValue value: " + value);
        String[] t = value.split(CloudFormationConstants.split);
        if (t.length < 2) {
            throw new OperationFailureException(operr("verb must contain '::'!"));
        }

        if ("ZStack".equals(t[0]) || "Cloud".equals(t[0])) {
            switch (t[1]){
                case "StackName":
                    return value.replace(value, stackData.getStackName());
                case "StackUuid":
                    return value.replace(value, stackData.getStackUuid());
                case "AccountUuid":
                    return value.replace(value, stackData.getAccountUuid());
                case "AccountName":
                    return value.replace(value, stackData.getAccountName());
                default:
                    throw new OperationFailureException(operr(String.format("invalid variable: %s", value)));
            }
        } else if (last == null) {
            for (ResourceStruct r : resources) {
                if (t[0].equals(r.getResourceName())) {
                    logger.debug(String.format("set resource field, resource: [%s], field: [%s]", t[0], t[1]));
                    if (r.getResults() == null) {
                        return null;
                    }
                    for (Field field : r.getResults().getClass().getDeclaredFields()) {
                        field.setAccessible(true);
                        Class clz = field.getType();
                        try {
                            Object inventory = field.get(r.getResults());
                            Field f = clz.getField(t[1]);
                            f.setAccessible(true);
                            Object found = f.get(inventory);
                            if (found != null) {
                                return found;
                            }
                        } catch (IllegalAccessException | NoSuchFieldException e) {
                            throw new OperationFailureException(operr(e.getMessage()));
                        }
                    }
                    return null;
                }
            }
        } else {
            if (isInteger(t[1])) {
                if (!(last instanceof List)) {
                    throw new OperationFailureException(operr("need List for resource [%s] output here, but got %s.", t[0], last.getClass().getName()));
                }
                return ((List)last).get(Integer.valueOf(t[1]));
            } else {
                try {
                    Field f = last.getClass().getField(t[1]);
                    f.setAccessible(true);
                    return f.get(last);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    throw new OperationFailureException(operr(e.getMessage()));
                }
            }
        }
        return null;
    }

    private static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    private static int appearNumber(String srcText, String findText) {
        int count = 0;
        Pattern p = Pattern.compile(findText);
        Matcher m = p.matcher(srcText);
        while (m.find()) {
            count++;
        }
        return count;
    }

    private ActionStruct getActionStructByResourceName(String resourceName) {
        for (ActionStruct s: actions.getActions()) {
            if (resourceName.equals(s.getResourceName())) {
                return s;
            }
        }
        throw new OperationFailureException(operr(String.format("resource [%s] must be set before!", resourceName)));

    }

    public static String userdata = "userdata::";

    private String replaceUserdata(String value) {
        // if userdata not base64 encode, we encode it
        if (value.startsWith(userdata)) {
            String data = value.substring(userdata.length());
            String decode = new String(Base64.decodeBase64(data));
            if (!data.equals(Base64.encodeBase64String(decode.getBytes()))) {
                return userdata + Base64.encodeBase64String(data.getBytes());
            } else {
                return value;
            }
        } else {
            return value;
        }
    }

    private void setField(Object o, String name, String key, Object value) {
        try {
            Field f = o.getClass().getDeclaredField(key);
            f.setAccessible(true);
            if (value instanceof LazilyParsedNumber) {
                LazilyParsedNumber tmp = (LazilyParsedNumber)value;
                if (Integer.class.isAssignableFrom(f.getType()) || Integer.TYPE.isAssignableFrom(f.getType())) {
                    f.set(o, tmp.intValue());
                } else if (Long.class.isAssignableFrom(f.getType()) || Long.TYPE.isAssignableFrom(f.getType())) {
                    f.set(o, tmp.longValue());
                } else {
                    f.set(o, tmp.doubleValue());
                }
            } else if (value instanceof List){
                List<Object> replaced = new ArrayList<>();
                for (Object tmp: (List)value) {
                    if (tmp instanceof String) {
                        replaced.add(replaceUserdata((String)tmp));
                    } else {
                        replaced.add(tmp);
                    }
                }
                f.set(o, replaced);
            } else {
                f.set(o, value);
            }
        } catch (NoSuchFieldException e) {
            logger.warn(String.format("%s: %s has no such field: %s", e.getClass().getSimpleName(),
                    e.getClass().getSimpleName(), key)); // only log it
        } catch (ReflectiveOperationException e) {
            if (e.getCause() != null) {
                throw new OperationFailureException(operr(e.getCause().getMessage()));
            } else {
                throw new OperationFailureException(operr(e.getMessage()));
            }
        }
    }

    private void setRound(ResourceStruct resource) {
        ActionStruct s = getActionStructByResourceName(resource.getResourceName());
        s.setRound(round);
    }

    private void setStruct(ResourceStruct resource, List<ResourceStruct> resources) {
        resource.getProperties().forEach((key, value) -> {
            if (value instanceof List) {
                List<Object> replaced = new ArrayList<>();
                for (Object tmp: (List)value) {
                    Object o = replace(tmp.toString(), resources);
                    if (o != null) {
                        replaced.add(o);
                    } else {
                        replaced.add(tmp);
                    }
                }
                resource.getProperties().put(key, replaced);
            } else {
                Object o = replace(value.toString(), resources);
                if (o != null) {
                    resource.getProperties().put(key, o);
                }
            }
        });
    }

    private CloudFormationErrorCode doCreate(List<ResourceStruct> availables, List<ResourceStruct> resources) {
        List<String> texts = new ArrayList<>(availables.size());
        boolean previewError = false;
        for(ResourceStruct a: availables) {
            if (!a.getInDegree().isEmpty()) {
                continue;
            }
            setRound(a);
            CloudFormationErrorCode err = create(a, resources);
            if (!err.isSuccess()) {
                if (dryRun) {
                    previewError = true;
                    continue;
                }

                if (stackData.isRollback()) {
                    rollback();
                    actions.getResults().removeAll(rollbackList);
                }
                return err;
            } else {
                texts.add(a.getResourceName());
            }
            resources.forEach(r -> {
                if (r.getResourceName().equals(a.getResourceName())) {
                    r.setCreated(true);
                } else if (r.getInDegree().contains(a.getResourceName())) {
                    //TODO: replace o to real output value
                    Object o = r.getProperties().get(a.getResourceName());

                    r.getInDegree().remove(a.getResourceName());
                }
            });
        }
        logger.debug(String.format("expect to create %d resource, but in fact %d created: [%s]",
                availables.size(), texts.size(), StringUtils.join(texts, ',')));

        CloudFormationErrorCode cloudFormationErrorCode = new CloudFormationErrorCode();
        if (previewError) {
            cloudFormationErrorCode.setErrorCode(Platform.argerr("Some actions are invalid"));
        }
        return cloudFormationErrorCode;
    }

    private void initialActions(final List<ResourceStruct> resources) {
        resources.forEach(r -> {
            ActionStruct s = new ActionStruct();
            s.setResourceName(r.getResourceName());
            for (String d: r.getInDegree()) {
                s.getInDegree().add(d);
            }
            actions.getActions().add(s);
        });
    }

    private List<EventCallback> registerCallbacks() {
        List<EventCallback> callbacks = new ArrayList<>();
        EventCallback vrouterCallback = new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                L3NetworkConstant.VRouterData d = (L3NetworkConstant.VRouterData)data;
                ResultStruct vrouter = new ResultStruct();
                vrouter.setRound(0); // we rollback vrouter at last round
                vrouter.setReserve(true);
                vrouter.setResourceUuid(d.vrouterUuid);
                vrouter.setResourceType(VmInstanceInventory.class.getName());
                VmInstanceVO vo = dbf.findByUuid(d.vrouterUuid, VmInstanceVO.class);
                if (vo == null) {
                    return;
                }
                vrouter.setResourceName(vo.getName());
                vpcs.add(vrouter);
            }
        };

        evtf.on(L3NetworkConstant.VROUTER_CREATE_EVENT_PATH, vrouterCallback);
        callbacks.add(vrouterCallback);
        return callbacks;
    }

    private void cancelCallbacks(List<EventCallback> callbacks) {
        if (callbacks == null) {
            return;
        }
        callbacks.forEach(c ->evtf.off(c));
    }

    @Deferred
    private void run(List<ResourceStruct> resources) {
        DebugUtils.Assert(stackData.getTimeout() != null && stackData.getTimeout() != 0, "timeout cannot be 0, create stack failed!");
        initialActions(resources);
        List<ResourceStruct> availables = findAvailableResources(resources);

        tlock = new GLock(String.format("timeout-for-stack-%s", stackData.getStackUuid()), 100);

        List<EventCallback> callbacks = registerCallbacks();
        Defer.defer(() -> {
            cancelCallbacks(callbacks);
        });

        while (!availables.isEmpty()) {
            logger.debug(String.format("Start create resources[%s] from stack[%s]", availables.stream().
                    map(ResourceStruct::getResourceName).collect(Collectors.toList()), stackData.getStackUuid()));
            CloudFormationErrorCode err = doCreate(availables, resources);
            if (!err.isSuccess()) {
                actions.setErrCode(err);
                break;
            }
            round ++;
            logger.debug(String.format("Successfully created resources[%s] from stack[%s]", availables.stream().
                    map(ResourceStruct::getResourceName).collect(Collectors.toList()), stackData.getStackUuid()));
            availables = findAvailableResources(resources);
        }

        List<String> createdUuids = created.stream().map(ResultStruct::getResourceUuid).collect(Collectors.toList());
        vpcs.forEach(vpc -> {
            if (createdUuids.contains(vpc.getResourceUuid())) {
                return;
            }
            actions.getResults().add(vpc);
        });

        actions.getActions().sort((o1, o2) -> {
            if (o1.getRound() > o2.getRound()) {
                return 1;
            } else if (o1.getRound() < o2.getRound()) {
                return -1;
            }
            return 0;
        });
    }

    private List<ResourceStruct> findAvailableResources(List<ResourceStruct> resources) {
        return resources.stream().
                filter(r -> !r.isCreated()).filter(r -> r.getInDegree().size() == 0).collect(Collectors.toList());
    }

    private void startRecode(ResourceStruct resource) {
        logger.debug(String.format("start create resource: %s from stack: %s", resource.getResourceName(), stackData.getStackUuid()));
        CloudFormationStackEventVO vo = new CloudFormationStackEventVO();
        vo.setStackUuid(stackData.getStackUuid());
        vo.setAction(resource.getAction());
        vo.setResourceName(resource.getResourceName());
        vo.setActionStatus(StackEventStatus.Start);
        vo.setContent(new GsonBuilder().setPrettyPrinting().create().toJson(resource.getProperties()));
        if (Q.New(CloudFormationStackEventVO.class).eq(CloudFormationStackEventVO_.resourceName, vo.getResourceName())
                .eq(CloudFormationStackEventVO_.stackUuid, vo.getStackUuid())
                .eq(CloudFormationStackEventVO_.actionStatus, StackEventStatus.Start).isExists()) {
            return;
        }
        dbf.persistAndRefresh(vo);
    }

    private void decreaseTimeout(long duration) {
        Long timeout = stackData.getTimeout();
        if (timeout == -1) {
            return;
        }
        tlock.lock();

        timeout -= duration;
        if (timeout < 0) {
            timeout = 0L;
        }
        stackData.setTimeout(timeout);
        tlock.unlock();
    }

    private void finishRecode(ResourceStruct resource) {
        logger.debug(String.format("finish create resource: %s from stack: %s", resource.getResourceName(), stackData.getStackUuid()));
        CloudFormationStackEventVO vo = new CloudFormationStackEventVO();
        vo.setStackUuid(stackData.getStackUuid());
        vo.setAction(resource.getAction());
        vo.setResourceName(resource.getResourceName());
        vo.setActionStatus(StackEventStatus.Finish);
        vo.setContent(new GsonBuilder().setPrettyPrinting().create().toJson(resource.getResults()));
        if (Q.New(CloudFormationStackEventVO.class).eq(CloudFormationStackEventVO_.resourceName, resource.getResourceName())
                .eq(CloudFormationStackEventVO_.stackUuid, vo.getStackUuid()).eq(CloudFormationStackEventVO_.action, resource.getAction())
                .eq(CloudFormationStackEventVO_.actionStatus, StackEventStatus.Finish).isExists()) {
            return;
        }

        CloudFormationStackEventVO start = Q.New(CloudFormationStackEventVO.class).eq(CloudFormationStackEventVO_.resourceName, resource.getResourceName())
                .eq(CloudFormationStackEventVO_.stackUuid, stackData.getStackUuid()).eq(CloudFormationStackEventVO_.action, resource.getAction())
                .eq(CloudFormationStackEventVO_.actionStatus, StackEventStatus.Start).find();
        if (start != null) {
            long duration = System.currentTimeMillis() - start.getCreateDate().getTime();
            vo.setDuration(CloudFormationUtils.getDuration(duration));
            if (stackData.getTimeout() != -1) {
                decreaseTimeout(duration);
            }
        }

        dbf.persistAndRefresh(vo);
    }

    private static String getDuration(String resourceName, String stackUuid, String action) {
        CloudFormationStackEventVO start = Q.New(CloudFormationStackEventVO.class).eq(CloudFormationStackEventVO_.resourceName, resourceName)
                .eq(CloudFormationStackEventVO_.stackUuid, stackUuid).eq(CloudFormationStackEventVO_.action, action)
                .eq(CloudFormationStackEventVO_.actionStatus, StackEventStatus.Start).find();
        if (start != null) {
            return CloudFormationUtils.getDuration(System.currentTimeMillis() - start.getCreateDate().getTime());
        }
        return null;
    }

    private void failedRecode(ResourceStruct resource, String reason) {
        logger.debug(String.format("failed create resource: %s because %s", resource.getResourceName(), reason));
        if (reason == null) {
            reason = "null error details";
        }
        CloudFormationStackEventVO vo = new CloudFormationStackEventVO();
        vo.setStackUuid(stackData.getStackUuid());
        vo.setAction(resource.getAction());
        vo.setResourceName(resource.getResourceName());
        vo.setActionStatus(StackEventStatus.Failed);
        vo.setContent(reason);
        if (Q.New(CloudFormationStackEventVO.class).eq(CloudFormationStackEventVO_.resourceName, resource.getResourceName())
                .eq(CloudFormationStackEventVO_.stackUuid, vo.getStackUuid()).eq(CloudFormationStackEventVO_.action, resource.getAction())
                .eq(CloudFormationStackEventVO_.actionStatus, StackEventStatus.Failed).isExists()) {
            return;
        }
        String duration = getDuration(resource.getResourceName(), stackData.getStackUuid(), resource.getAction());
        if (duration != null) {
            vo.setDuration(duration);
        }

        dbf.persistAndRefresh(vo);
    }

    private void setApiTimeout(Object action) {
        try {
            Field f = action.getClass().getField("apiTimeout");
            f.setAccessible(true);
            f.set(action, TimeUnit.MILLISECONDS.toSeconds(stackData.getTimeout()));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            logger.warn(String.format("set timeout failed for %s, caused: %s", action.getClass().getName(), e.getMessage()));
            throw new OperationFailureException(operr(e.getMessage()));
        }
    }

    private CloudFormationErrorCode create(ResourceStruct resource, List<ResourceStruct> resources) {
        CloudFormationErrorCode code = new CloudFormationErrorCode();
        String clz = CloudFormationConstants.sdkPackage + resource.getAction();
        try {
            Object action = Class.forName(clz).newInstance();
            Set<Map.Entry<String, Object>> params = resource.getProperties().entrySet();
            if (!dryRun) {
                // replace variables like "${l3::uuid}" to real values from the bellow actions' results
                setStruct(resource, resources);
            }
            params.forEach(p -> {
                setField(action, resource.getResourceName(), p.getKey(), p.getValue());
            });

            setApiTimeout(action);

            if (action instanceof AbstractAction) {
                ActionStruct actionStruct = getActionStructByResourceName(resource.getResourceName());
                actionStruct.setActionName(action.getClass().getName());
                actionStruct.setActions(action);

                if (dryRun) {
                    try {
                        Field f = action.getClass().getDeclaredField("sessionId");
                        f.setAccessible(true);
                        f.set(action, Platform.FAKE_UUID);
                        ((AbstractAction)action).checkParameters();
                        f.set(action, null);
                    } catch (ApiException e) {
                        actionStruct.setError(e.getMessage());
                        code.setErrorCode(Platform.argerr(e.getMessage()));
                        return code;
                    }
                }
            }
            Method call = action.getClass().getMethod("call");
            if (!dryRun) {
                try {
                    if (stackData.getSessionUuid() != null) {
                        Field f = action.getClass().getDeclaredField("sessionId");
                        f.setAccessible(true);
                        f.set(action, stackData.getSessionUuid());
                    } else {
                        Field accessKeyId = action.getClass().getDeclaredField("accessKeyId");
                        accessKeyId.setAccessible(true);
                        accessKeyId.set(action, stackData.getAccessKeyId());
                        Field accessKeySecret = action.getClass().getDeclaredField("accessKeySecret");
                        accessKeySecret.setAccessible(true);
                        accessKeySecret.set(action, stackData.getAccessKeySecret());
                    }
                    startRecode(resource);   // start record

                    for (CloudFormationExtensionPoint exp: pluginRgty.getExtensionList(CloudFormationExtensionPoint.class)) {
                        exp.beforeCloudFormationAction(action);
                    }

                    Object result = call.invoke(action);
                    Field err = result.getClass().getField("error");
                    err.setAccessible(true);
                    Object ob = err.get(result);
                    if (ob != null) {
                        ErrorCode errSdk = (ErrorCode)ob;
                        code.setErrorCode(Platform.err(CloudFormationErrors.CREATE_FAILED, String.format("action '%s' failed, resourceName is '%s', due to: %s",
                                action.getClass().getSimpleName(), resource.getResourceName(), errSdk.details)));
                        failedRecode(resource, errSdk.details);   // failed record
                    } else {
                        Field field = result.getClass().getField("value");
                        field.setAccessible(true);
                        ob = field.get(result);
                        resource.setResults(ob);
                        finishRecode(resource);   // finish record

                        if (resource.getType() == ResourceType.Resource) {
                            // ob is xxxResult in xxxAction
                            setResourceResult(ob, resource.getResourceName(), resource.getDeletePolicy());
                        }

                        for (CloudFormationExtensionPoint exp: pluginRgty.getExtensionList(CloudFormationExtensionPoint.class)) {
                            exp.afterCloudFormationAction(action, ob, stackData);
                        }
                    }
                } catch (NoSuchFieldException e) {
                    code.setErrorCode(Platform.operr(e.getMessage()));
                    failedRecode(resource, e.getMessage());   // failed record
                } catch (InvocationTargetException e) {
                    code.setErrorCode(Platform.operr(e.getTargetException().getMessage()));
                    failedRecode(resource, e.getTargetException().getMessage());   // failed record
                }
            } else {
                ResultStruct result = new ResultStruct();
                result.setResourceUuid(UUID.randomUUID().toString().replace("-", ""));
                result.setResourceType(resource.getAction().getClass().getName()); // Fake type
                result.setResourceName(resource.getResourceName());
                if (CoreGlobalProperty.UNIT_TEST_ON && resource.isMockFailed()) {
                    code.setErrorCode(new org.zstack.header.errorcode.ErrorCode("Test.1000", "err-description", "err-details"));
                } else {
                    created.push(result);
                }
            }
        }  catch (Exception e) {
            code.setErrorCode(Platform.operr(e.getMessage()));
            String causeName;
            if (e.getCause() != null) {
                causeName = e.getCause().getClass().getName() + ":";
            } else {
                causeName = "";
            }
            failedRecode(resource, causeName + e.getMessage());   // failed record
        }
        return code;
    }

    private void setDataVolumeResult(List<VolumeInventory> volumes, boolean reserve, String resourceName) {
        for (VolumeInventory volume: volumes) {
            if (!volume.getType().equals(VolumeType.Data.name())) {
                continue;
            }
            ResultStruct result = new ResultStruct();
            result.setResourceUuid(volume.uuid);
            result.setResourceType(org.zstack.header.volume.VolumeInventory.class.getName());
            result.setResourceName(resourceName);
            result.setRound(round);
            result.setReserve(reserve);
            actions.getResults().add(result);
            created.push(result);
        }
    }

    private void setResourceResult(Object ob, String resourceName, String deletionPolicy) {
        for (Field field: ob.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object inventory = field.get(ob);
                String src = SourceClassMap.dstToSrcMapping.get(inventory.getClass().getName());
                if (src == null) {
                    continue;
                }
                Field f = field.getType().getField("uuid");
                if (f.get(inventory) == null) {
                    continue;
                }
                f.setAccessible(true);
                Class<?> clz = Class.forName(src);

                ResultStruct result = new ResultStruct();
                result.setResourceUuid((String)f.get(inventory));
                result.setResourceType(clz.getName());
                result.setResourceName(resourceName);
                result.setRound(round);

                if (deletionPolicy != null && deletionPolicy.equals("Retain")) {
                    result.setReserve(true);
                } else {
                    result.setReserve(false);
                }
                actions.getResults().add(result);
                created.push(result);

                if (VmInstanceInventory.class.isAssignableFrom(clz)) {
                    Field v = field.getType().getField("allVolumes");
                    v.setAccessible(true);
                    if (v.get(inventory) != null) {
                        List<VolumeInventory> volumes = (List<VolumeInventory>)v.get(inventory);
                        setDataVolumeResult(volumes, result.isReserve(), resourceName);
                    }
                }
                break;
            } catch (NoSuchFieldException e) {
                logger.warn("no such field: uuid, possibly it is not zstack resource.");
            } catch (IllegalAccessException | ClassNotFoundException e) {
                throw new OperationFailureException(operr(e.getMessage()));
            }
        }
    }

    private void rollback() {
        if (created.empty()) {
            return;
        }

        ResultStruct resource = created.pop();

        logger.debug(String.format("start rollback resource: %s, resourceType: %s from stack: %s", resource.getResourceUuid(), resource.getResourceType(), stackData.getStackUuid()));
        if (!dryRun) {
            CloudFormationDeleter deleter = new CloudFormationDeleter();
            deleter.deleteResource(resource.getResourceUuid(), resource.getResourceType(), resource.getResourceName(), stackData.getSessionUuid(), stackData.getStackUuid());
            rollbackList.add(resource);
        }
        rollback();
    }

    public CfnActions dryRun(CfnResults result) {
        logger.debug("start dry run create resources: ");
        dryRun = true;
        StackData stackData = new StackData();
        stackData.setRollback(true);
        stackData.setTimeout(-1L);
        this.stackData = stackData;

        List<ResourceStruct> dryRunList = new ArrayList<>();
        for (ResourceStruct s: result.getResources()) {
            ResourceStruct d = s.clone();
            dryRunList.add(d);
        }
        run(dryRunList);
        return actions;
    }

    public CfnActions createResource(CfnResults result, StackData stackData) {
        logger.debug(String.format("start create resources: %s, timeout: %d from stack: %s", stackData.getStackName(), stackData.getTimeout(), stackData.getStackUuid()));
        logger.debug(JSONObjectUtil.toJsonString(result));
        this.stackData = stackData;
        run(result.getResources());
        if (actions.isSuccess()) {
            writeOutputs(result.getOutputs(), result.getResources());
        }
        return actions;
    }

    private void writeOutputs(List<OutputStruct> outputs, List<ResourceStruct> results) {
        outputs.forEach(output -> {
            Object o = replace(String.valueOf(output.getContent()), results);
            if (o != null) {
                output.setContent(o);
            }
        });
    }
}
