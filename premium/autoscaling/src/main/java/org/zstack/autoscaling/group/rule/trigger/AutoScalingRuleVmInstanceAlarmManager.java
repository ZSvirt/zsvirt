package org.zstack.autoscaling.group.rule.trigger;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.autoscaling.group.AutoScalingGroupStateChangedExtensionPoint;
import org.zstack.autoscaling.group.AutoScalingGroupStateEvent;
import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.autoscaling.group.ScalingResourceType;
import org.zstack.autoscaling.group.activity.action.AutoScalingCreateInstancesActionExtensionPoint;
import org.zstack.autoscaling.group.activity.action.AutoScalingGroupCreateInstancesActionMsg;
import org.zstack.autoscaling.group.activity.action.AutoScalingGroupRemoveInstancesActionExtensionPoint;
import org.zstack.autoscaling.group.activity.action.CreateInstancesResult;
import org.zstack.autoscaling.group.activity.action.RemoveInstancesResult;
import org.zstack.autoscaling.group.instance.AutoScalingGroupInstanceVO;
import org.zstack.autoscaling.group.instance.AutoScalingGroupInstanceVO_;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.NopeWhileDoneCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.alarm.AlarmConstants;
import org.zstack.zwatch.alarm.AlarmLabelVO;
import org.zstack.zwatch.alarm.AlarmLabelVO_;
import org.zstack.zwatch.alarm.AlarmStateEvent;
import org.zstack.zwatch.alarm.ChangeAlarmStateMsg;
import org.zstack.zwatch.alarm.UpdateAlarmLabelMsg;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lining on 2018/9/16.
 */
public class AutoScalingRuleVmInstanceAlarmManager implements AutoScalingCreateInstancesActionExtensionPoint, AutoScalingGroupRemoveInstancesActionExtensionPoint, AutoScalingGroupStateChangedExtensionPoint {
    private static final CLogger logger = Utils.getLogger(AutoScalingRuleVmInstanceAlarmManager.class);

    @Autowired
    DatabaseFacade dbf;
    @Autowired
    CloudBus bus;

    public static Map<String, AutoScalingRuleTriggerFactory> factoryMap = Collections.synchronizedMap(new HashMap<String, AutoScalingRuleTriggerFactory>());
    @Override
    public void beforeCreateInstances(AutoScalingGroupCreateInstancesActionMsg msg) {
        //do nothing
    }

    @Override
    public void preCreateInstances(AutoScalingGroupCreateInstancesActionMsg msg) {
        //do nothing
    }

    @Override
    public void afterCreateInstancesSuccess(AutoScalingGroupCreateInstancesActionMsg msg, CreateInstancesResult result) {
        //update alarm target
        String autoScalingGroupUuid = msg.getAutoScalingGroupUuid();

        if (!checkAutoScalingGroupResourceType(autoScalingGroupUuid)) {
            return;
        }

        List<AutoScalingRuleAlarmTriggerVO> alarmTriggerVOList = getAlarmTrigger(autoScalingGroupUuid);
        if (alarmTriggerVOList == null || alarmTriggerVOList.isEmpty()) {
            return;
        }

        updateAlarmTargetVmInstanceList(autoScalingGroupUuid, alarmTriggerVOList);
    }

    private UpdateAlarmLabelMsg makeUpdateAlarmLabelMsg(AutoScalingRuleAlarmTriggerVO alarmTriggerVO, String instanceUuidListStr) {
        List<AlarmLabelVO> labelVOS = Q.New(AlarmLabelVO.class)
                .eq(AlarmLabelVO_.alarmUuid, alarmTriggerVO.getAlarmUuid())
                .eq(AlarmLabelVO_.key, VmNamespace.LabelNames.VMUuid.toString())
                .list();

        if (labelVOS == null || labelVOS.isEmpty()) {
            return null;
        }

        UpdateAlarmLabelMsg updateAlarmLabelMsg = new UpdateAlarmLabelMsg();
        updateAlarmLabelMsg.setUuid(labelVOS.get(0).getUuid());
        updateAlarmLabelMsg.setAlarmUuid(alarmTriggerVO.getAlarmUuid());
        updateAlarmLabelMsg.setValue(instanceUuidListStr);
        bus.makeLocalServiceId(updateAlarmLabelMsg, AlarmConstants.SERVICE_ID);
        return updateAlarmLabelMsg;
    }

    @Override
    public void beforeRemoveInstances(String scalingGroupUuid, List<String> instanceUuids) {
        // do nothing
    }

    @Override
    public void preRemoveInstances(String scalingGroupUuid, List<String> instanceUuids) {
        // do nothing
    }

    // update alarm target
    @Override
    public void afterRemoveInstancesSuccess(String scalingGroupUuid, RemoveInstancesResult result) {
        if (!checkAutoScalingGroupResourceType(scalingGroupUuid)) {
            return;
        }

        List<AutoScalingRuleAlarmTriggerVO> alarmTriggerVOList = getAlarmTrigger(scalingGroupUuid);
        if (alarmTriggerVOList == null || alarmTriggerVOList.isEmpty()) {
            return;
        }

        updateAlarmTargetVmInstanceList(scalingGroupUuid, alarmTriggerVOList);
    }

    private boolean checkAutoScalingGroupResourceType(String autoScalingGroupUuid) {
        AutoScalingGroupVO groupVO = dbf.findByUuid(autoScalingGroupUuid, AutoScalingGroupVO.class);
        if (groupVO.getScalingResourceType() == ScalingResourceType.VmInstance) {
            return true;
        }
        return false;
    }

    private List<AutoScalingRuleAlarmTriggerVO> getAlarmTrigger(String autoScalingGroupUuid) {
        List<AutoScalingRuleAlarmTriggerVO> alarmTriggerVOList = SQL.New("select t1 from AutoScalingRuleVO t0, AutoScalingRuleAlarmTriggerVO t1" +
                " where t0.scalingGroupUuid = :autoScalingGroupUuid and t0.uuid = t1.ruleUuid")
                .param("autoScalingGroupUuid", autoScalingGroupUuid)
                .list();
        return alarmTriggerVOList;
    }

    private void updateAlarmTargetVmInstanceList(String autoScalingGroupUuid, List<AutoScalingRuleAlarmTriggerVO> alarmTriggerVOList) {
        List<String> instanceUuidList = Q.New(AutoScalingGroupInstanceVO.class)
                .select(AutoScalingGroupInstanceVO_.instanceUuid)
                .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, autoScalingGroupUuid)
                .listValues();
        String instanceUuidListStr = String.join("|" , instanceUuidList);

        List<UpdateAlarmLabelMsg> updateAlarmLabelMsgs = new ArrayList<>();
        for (AutoScalingRuleAlarmTriggerVO alarmTriggerVO : alarmTriggerVOList) {
            UpdateAlarmLabelMsg updateAlarmLabelMsg = makeUpdateAlarmLabelMsg(alarmTriggerVO, instanceUuidListStr);
            if (updateAlarmLabelMsg == null) {
                continue;
            }
            updateAlarmLabelMsgs.add(updateAlarmLabelMsg);
        }

        if (updateAlarmLabelMsgs.isEmpty()) {
            return;
        }

        new While<>(updateAlarmLabelMsgs).step((updateAlarmLabelMsg, completion) -> {
            bus.send(updateAlarmLabelMsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply rly) {
                    if (!rly.isSuccess()) {
                        String error = String.format("auoScalingGroup[uuid=%s] update alarm[uuid=%s] label[uuid=%s] failed, %s",
                                autoScalingGroupUuid, updateAlarmLabelMsg.getAlarmUuid(), updateAlarmLabelMsg.getUuid(), rly.getError().toString());
                        logger.error(error);
                    }

                    completion.done();
                }
            });
        }, 10).run(new NopeWhileDoneCompletion());
    }

    @Override
    public void afterToggleAutoScalingGroupState(String autoScalingGroupUuid, String newStateEvent) {
        List<AutoScalingRuleAlarmTriggerVO> alarmTriggerVOList = getAlarmTrigger(autoScalingGroupUuid);

        if (alarmTriggerVOList == null || alarmTriggerVOList.isEmpty()) {
            return;
        }

        for (AutoScalingRuleAlarmTriggerVO triggerVO : alarmTriggerVOList){
            ChangeAlarmStateMsg changeAlarmStateMsg = new ChangeAlarmStateMsg();
            changeAlarmStateMsg.setUuid(triggerVO.getAlarmUuid());
            if (newStateEvent.equalsIgnoreCase(AutoScalingGroupStateEvent.enable.toString())){
                changeAlarmStateMsg.setStateEvent(AlarmStateEvent.enable);
            }else{
                changeAlarmStateMsg.setStateEvent(AlarmStateEvent.disable);
            }

            bus.makeLocalServiceId(changeAlarmStateMsg, AlarmConstants.SERVICE_ID);
            bus.send(changeAlarmStateMsg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.error(String.format("change alarm[%s] state fail, %s", triggerVO.getAlarmUuid(), reply.getError().getDetails()));
                    }
                }
            });
        }
    }
}
