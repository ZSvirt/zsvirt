package org.zstack.zwatch.migratedb;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.identity.AccountManager;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchConstants;
import org.zstack.zwatch.alarm.AlarmAction;
import org.zstack.zwatch.alarm.AlarmInventory;
import org.zstack.zwatch.alarm.AlarmLabelInventory;
import org.zstack.zwatch.alarm.EventSubscriptionVO;
import org.zstack.zwatch.datatype.AlarmDataV2;
import org.zstack.zwatch.datatype.EventData;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.utils.ParserUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MysqlAuditAction implements AlarmAction {
    private static final CLogger logger = Utils.getLogger(MysqlAuditAction.class);

    @Autowired
    private final MigrateDBEventDatabaseDriver mysqlDriver;

    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private DatabaseFacade dbf;

    public MysqlAuditAction() {
        //this.mysqlDriver = Platform.getComponentLoader().getComponentByBeanName("MigrateDBEventDatabaseDriver");
        this.mysqlDriver = new MigrateDBEventDatabaseDriver();
    }

    @Override
    public void takeAction(TakeAlarmActionParam param) {
        AlarmDataV2 data = new AlarmDataV2();
        AlarmInventory inv = param.alarm;
        data.setDataUuid(param.alarmDataUuid);
        data.setTime(System.currentTimeMillis());
        data.setAlarmName(inv.getName());
        data.setAlarmUuid(inv.getUuid());
        data.setAlarmStatus(param.currentStatus.toString());
        data.setMetricName(inv.getMetricName());
        data.setNamespace(inv.getNamespace());
        data.setPeriod(inv.getPeriod());
        data.setThreshold(inv.getThreshold());
        data.setMetricValue(param.currentValue);
        List<String> sortedLabels = inv.getLabels().stream()
                .map(l -> String.format("%s %s %s",
                        l.getKey(), Label.Operator.valueOf(l.getOperator()).name(), l.getValue()))
                .sorted() // Sort based on the keys
                .collect(Collectors.toList());
        data.setLabels(StringUtils.join(sortedLabels, ", "));
        data.setComparisonOperator(inv.getComparisonOperator().toString());

        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(inv.getUuid());
        data.setAccountUuid(accountUuid);

        data.setReadStatus(ZWatchConstants.DATA_READ_STATUS_UNREAD);
        data.setEmergencyLevel(inv.getEmergencyLevel());

        Namespace ns = Namespace.getMetricNameSpace(inv.getNamespace(), inv.getMetricName());
        if (ns != null) {
            if (ns.getResourceType() != null) {
                data.setResourceType(ns.getResourceType());
            }

            String identityLabelOfNamespace = ns.getIdentityLabelName();
            if (identityLabelOfNamespace != null) {
                Optional<AlarmLabelInventory> labelOfAlarm = inv.getLabels().stream().filter(l -> l.getKey().equals(identityLabelOfNamespace)).findFirst();
                labelOfAlarm.ifPresent(alarmLabelInventory -> data.setResourceUuid(alarmLabelInventory.getValue()));

                if (param.identifyLabel != null) {
                    Map<String, String> identifyLabel = ParserUtils.parseIdentifyLabel(param.identifyLabel);
                    String resourceUuid = identifyLabel.get(identityLabelOfNamespace);
                    if (resourceUuid != null) {
                        data.setResourceUuid(resourceUuid);
                        data.setNamespace(param.getNamespace(resourceUuid));
                    }

                    identifyLabel.remove(identityLabelOfNamespace);
                    data.setContext(JSONObjectUtil.toJsonString(identifyLabel));
                }
            } else {
                if (param.identifyLabel != null) {
                    Map<String, String> identifyLabel = ParserUtils.parseIdentifyLabel(param.identifyLabel);
                    data.setContext(JSONObjectUtil.toJsonString(identifyLabel));
                }
            }
        }

        mysqlDriver.alarm(data);
    }

    @Override
    public void takeAction(TakeEventSubscriptionActionParam param) {
        EventSubscriptionVO subscriptionVO = dbf.findByUuid(param.subscriptionUuid, EventSubscriptionVO.class);

        EventData eventData = param.event;
        eventData.setDataUuid(param.dataUuid);
        eventData.setNamespace(param.getNamespace(eventData.getResourceId()));
        eventData.setAccountUuid(param.subscriptionAccountUuid);
        eventData.setSubscriptionUuid(subscriptionVO.getUuid());
        eventData.setReadStatus(ZWatchConstants.DATA_READ_STATUS_UNREAD);
        if (subscriptionVO.getEmergencyLevel() != null) {
            eventData.setEmergencyLevel(subscriptionVO.getEmergencyLevel().name());
        }
        mysqlDriver.persist(Arrays.asList(eventData));
    }

    @Override
    public void takeActionForThirdpartyAlert(TakeEventSubscriptionActionParam param) {
        return;
    }
}
