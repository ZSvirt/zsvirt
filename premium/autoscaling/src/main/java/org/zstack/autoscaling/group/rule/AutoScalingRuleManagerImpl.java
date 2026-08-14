package org.zstack.autoscaling.group.rule;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.autoscaling.group.AutoScalingGroupSystemTags;
import org.zstack.autoscaling.group.AutoScalingGroupVO;
import org.zstack.autoscaling.group.activity.AutoScalingGroupActivityStatus;
import org.zstack.autoscaling.group.activity.AutoScalingGroupActivityVO;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleAlarmTriggerVO;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleAlarmTriggerVO_;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleSchedulerJobTriggerVO;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleSchedulerJobTriggerVO_;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleTriggerState;
import static org.zstack.core.Platform.operr;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.Component;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.scheduler.autoscalinggroup.TakeAutoScalingSchedulerJobExtensionPoint;
import org.zstack.tag.SystemTagCreator;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ruleengine.Rule;
import org.zstack.zwatch.ruleengine.RuleEvaluationResult;
import org.zstack.zwatch.ruleengine.RuleEvaluationResultListener;
import org.zstack.zwatch.ruleengine.RuleManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by lining on 2018/9/22.
 */
public class AutoScalingRuleManagerImpl implements AutoScalingRuleManager,Component, TakeAutoScalingSchedulerJobExtensionPoint {
    protected static final CLogger logger = Utils.getLogger(AutoScalingRuleManagerImpl.class);
    @Autowired
    RuleManager ruleMgr;
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        installRuleStateChangeListener();

        return true;
    }

    private void installRuleStateChangeListener() {
        ruleMgr.installRuleStateChangeListener(new RuleEvaluationResultListener() {
            @Override
            public void stateChanged(RuleEvaluationResult res, Rule rule) {
                // do nothing
            }

            @Override
            public void problemState(RuleEvaluationResult res, Rule rule) {
                triggerAutoScalingRule(res, rule);
            }
        });
    }

    @Override
    public void triggerAutoScalingRule(RuleEvaluationResult res, Rule alarmRule) {
        String alarmUuid = alarmRule.getUuid();

        // 1.Find the right rule
        List<AutoScalingRuleVO> rules = SQL.New("select t2 from AutoScalingRuleAlarmTriggerVO t0, " +
                "AutoScalingRuleVO t2 " +
                "where t0.ruleUuid = t2.uuid " +
                "and t0.alarmUuid = :alarmUuid " +
                "and t0.state = :ruleTriggerState " +
                "and t2.state = :ruleState")
                .param("alarmUuid", alarmUuid)
                .param("ruleTriggerState", AutoScalingRuleTriggerState.Enabled)
                .param("ruleState",  AutoScalingRuleState.Enabled)
                .list();

        if (rules == null || rules.isEmpty()) {
            return;
        }
        assert rules.size() == 1;

        // 2.Calculate cooling time
        AutoScalingRuleVO ruleVO = rules.get(0);

        String cooldownDataStr = AutoScalingGroupSystemTags.COOLDOWN_DATE.getTokenByResourceUuid(ruleVO.getScalingGroupUuid(), AutoScalingGroupSystemTags.COOLDOWN_DATE_TOKEN);
        if (cooldownDataStr != null) {
            long cooldownDate = Long.parseLong(cooldownDataStr);
            if (System.currentTimeMillis() < cooldownDate) {
                //log
                return;
            }
        }

        // 3.Trigger rule
        String ruleTriggerUuid = Q.New(AutoScalingRuleAlarmTriggerVO.class)
                .select(AutoScalingRuleAlarmTriggerVO_.uuid)
                .eq(AutoScalingRuleAlarmTriggerVO_.alarmUuid, alarmUuid)
                .findValue();
        TriggerAutoScalingGroupRuleMsg msg = new TriggerAutoScalingGroupRuleMsg();
        msg.setAutoScalingRuleUuid(ruleVO.getUuid());
        msg.setAutoScalingGroupUuid(ruleVO.getScalingGroupUuid());
        msg.setAutoScalingRuleTriggerUUid(ruleTriggerUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, AutoScalingConstants.SERVICE_ID, ruleVO.getScalingGroupUuid());

        // 4.Save cooldown data
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply rly) {
                if (!rly.isSuccess()) {
                    //log
                    return;
                }
                TriggerAutoScalingGroupRuleReply reply = (TriggerAutoScalingGroupRuleReply) rly;
                updateAutoScalingGroupCooldownDate(reply.getAutoScalingGroupActivityUuid(), ruleVO);
            }
        });
    }

    private void updateAutoScalingGroupCooldownDate(String autoScalingGroupActivityUuid, AutoScalingRuleVO ruleVO) {
        AutoScalingGroupActivityVO activityVO = dbf.findByUuid(autoScalingGroupActivityUuid, AutoScalingGroupActivityVO.class);
        if (activityVO.getStatus() == AutoScalingGroupActivityStatus.Successful) {
            updateAutoScalingGroupCooldownDate(ruleVO);
        }
    }

    private void updateAutoScalingGroupCooldownDate(AutoScalingRuleVO ruleVO) {
        Long ruleCooldown = ruleVO.getCooldown();

        String groupUuid = ruleVO.getScalingGroupUuid();
        AutoScalingGroupVO groupVO = dbf.findByUuid(groupUuid, AutoScalingGroupVO.class);
        Long defaultCooldown = groupVO.getDefaultCooldown();

        long cooldown = ruleCooldown != null ? ruleCooldown : defaultCooldown;
        if (cooldown == 0) {
            return;
        }

        long cooldownDate = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(cooldown);
        SystemTagCreator creator = AutoScalingGroupSystemTags.COOLDOWN_DATE.newSystemTagCreator(groupUuid);
        creator.ignoreIfExisting = true;
        creator.inherent = false;
        creator.recreate = true;
        creator.unique = true;
        creator.setTagByTokens(map(e(AutoScalingGroupSystemTags.COOLDOWN_DATE_TOKEN, cooldownDate)));
        creator.create();
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void takeAutoScalingSchedulerJob(String schedulerJobUuid, String ruleUuid, ReturnValueCompletion completion) {
        AutoScalingRuleVO ruleVO = SQL.New("select t2 from AutoScalingRuleSchedulerJobTriggerVO t0, " +
                "AutoScalingRuleVO t2 " +
                "where t0.ruleUuid = t2.uuid " +
                "and t0.schedulerJobUuid = :schedulerJobUuid " +
                "and t0.state = :ruleTriggerState " +
                "and t2.state = :ruleState " +
                "and t2.uuid = :ruleUuid")
                .param("schedulerJobUuid", schedulerJobUuid)
                .param("ruleTriggerState", AutoScalingRuleTriggerState.Enabled)
                .param("ruleState",  AutoScalingRuleState.Enabled)
                .param("ruleUuid", ruleUuid)
                .find();

        if (ruleVO == null) {
            AutoScalingRuleSchedulerJobTriggerVO triggerVO = Q.New(AutoScalingRuleSchedulerJobTriggerVO.class)
                    .eq(AutoScalingRuleSchedulerJobTriggerVO_.schedulerJobUuid, schedulerJobUuid)
                    .find();
            completion.fail(operr("AutoScalingRuleSchedulerJobTriggerVO[uuid:%s] is %s, state change is not allowed", triggerVO.getUuid(), triggerVO.getState()));
            return;
        }

        String cooldownDataStr = AutoScalingGroupSystemTags.COOLDOWN_DATE.getTokenByResourceUuid(ruleVO.getScalingGroupUuid(), AutoScalingGroupSystemTags.COOLDOWN_DATE_TOKEN);
        if (cooldownDataStr != null) {
            long cooldownDate = Long.parseLong(cooldownDataStr);
            if (System.currentTimeMillis() < cooldownDate) {
                logger.warn(String.format("AutoScalingRuleSchedulerJobTriggerVO[uuid:%s] is be in cooldownDate", ruleUuid));
                completion.fail(operr("AutoScalingRuleSchedulerJobTriggerVO[uuid:%s] is be in cooldownDate", ruleUuid));
                return;
            }
        }

        // 3.Trigger rule
        String ruleTriggerUuid = Q.New(AutoScalingRuleSchedulerJobTriggerVO.class)
                .select(AutoScalingRuleSchedulerJobTriggerVO_.uuid)
                .eq(AutoScalingRuleSchedulerJobTriggerVO_.schedulerJobUuid, schedulerJobUuid)
                .findValue();
        TriggerAutoScalingGroupRuleMsg groupRuleMsg = new TriggerAutoScalingGroupRuleMsg();
        groupRuleMsg.setAutoScalingRuleUuid(ruleVO.getUuid());
        groupRuleMsg.setAutoScalingGroupUuid(ruleVO.getScalingGroupUuid());
        groupRuleMsg.setAutoScalingRuleTriggerUUid(ruleTriggerUuid);
        bus.makeTargetServiceIdByResourceUuid(groupRuleMsg, AutoScalingConstants.SERVICE_ID, ruleVO.getScalingGroupUuid());

        // 4.Save cooldown data
        bus.send(groupRuleMsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply rly) {
                if (!rly.isSuccess()) {
                    logger.warn(rly.getError().getDetails());
                    completion.fail(rly.getError());
                    return;
                }

                TriggerAutoScalingGroupRuleReply reply = (TriggerAutoScalingGroupRuleReply) rly;
                updateAutoScalingGroupCooldownDate(reply.getAutoScalingGroupActivityUuid(), ruleVO);

                completion.success(rly);
            }
        });
    }

    @Override
    public Message buildRequest(String schedulerJobUuid, String ruleUuid) {
        AutoScalingRuleVO ruleVO = dbf.findByUuid(ruleUuid, AutoScalingRuleVO.class);

        String ruleTriggerUuid = Q.New(AutoScalingRuleSchedulerJobTriggerVO.class)
                .select(AutoScalingRuleSchedulerJobTriggerVO_.uuid)
                .eq(AutoScalingRuleSchedulerJobTriggerVO_.schedulerJobUuid, schedulerJobUuid)
                .eq(AutoScalingRuleSchedulerJobTriggerVO_.ruleUuid, ruleUuid)
                .findValue();

        TriggerAutoScalingGroupRuleMsg groupRuleMsg = new TriggerAutoScalingGroupRuleMsg();
        groupRuleMsg.setAutoScalingRuleUuid(ruleVO.getUuid());
        groupRuleMsg.setAutoScalingGroupUuid(ruleVO.getScalingGroupUuid());
        groupRuleMsg.setAutoScalingRuleTriggerUUid(ruleTriggerUuid);
        bus.makeTargetServiceIdByResourceUuid(groupRuleMsg, AutoScalingConstants.SERVICE_ID, ruleVO.getScalingGroupUuid());
        return groupRuleMsg;
    }

    @Override
    public ErrorCode allowStateChange(String ruleUuid) {
        if (Q.New(AutoScalingRuleVO.class)
                .eq(AutoScalingRuleVO_.uuid, ruleUuid)
                .eq(AutoScalingRuleVO_.state, AutoScalingRuleState.Disabled)
                .isExists()) {
            return operr("AutoScalingRuleVO[uuid:%s] is %s, state change is not allowed", ruleUuid, AutoScalingRuleState.Disabled.toString());
        }

        return null;
    }
}
