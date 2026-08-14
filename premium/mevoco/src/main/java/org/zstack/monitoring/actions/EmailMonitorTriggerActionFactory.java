package org.zstack.monitoring.actions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.monitoring.EmailTriggerEvent;

/**
 * Created by xing5 on 2017/7/7.
 */
public class EmailMonitorTriggerActionFactory implements MonitorTriggerActionFactory, MonitorTriggerActionExtension {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public String getMonitorActionType() {
        return MonitorTriggerActionConstants.EMAIL_MONITOR_TRIGGER_ACTION_TYPE;
    }

    @Override
    @Transactional
    public MonitorTriggerActionVO createMonitorTriggerAction(MonitorTriggerActionVO vo, APICreateMonitorTriggerActionMsg msg) {
        APICreateEmailMonitorTriggerActionMsg emsg = (APICreateEmailMonitorTriggerActionMsg) msg;

        EmailTriggerActionVO evo = new EmailTriggerActionVO(vo);
        evo.setEmail(emsg.getEmail());
        evo.setMediaUuid(emsg.getMediaUuid());

        return evo;
    }

    @Override
    public MonitorTriggerActionInventory getInventory(String uuid) {
        EmailTriggerActionVO vo = dbf.findByUuid(uuid, EmailTriggerActionVO.class);
        return EmailTriggerActionInventory.valueOf(vo);
    }

    @Override
    @AsyncThread
    public void afterCreateMonitorTriggerAction(MonitorTriggerActionVO vo) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        if(!(vo instanceof EmailTriggerActionVO)){
            return;
        }

        EmailTriggerEvent emailTriggerEvent = new EmailTriggerEvent(null, EmailTriggerActionInventory.valueOf((EmailTriggerActionVO)vo), null );
        emailTriggerEvent.sendEmail(String.format("Create email monitor trigger action[uuid:%s,name:%s] success", vo.getUuid(), vo.getName()));
    }
}
