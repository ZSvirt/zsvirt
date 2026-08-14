package org.zstack.monitoring.actions;

import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2017/7/7.
 */
@Inventory(mappingVOClass = EmailTriggerActionVO.class, collectionValueOfMethod = "valueOf1",
    parent = @Parent(inventoryClass = MonitorTriggerActionInventory.class, type = MonitorTriggerActionConstants.EMAIL_MONITOR_TRIGGER_ACTION_TYPE))
public class EmailTriggerActionInventory extends MonitorTriggerActionInventory {
    private String email;
    private String mediaUuid;

    public EmailTriggerActionInventory() {
    }

    public EmailTriggerActionInventory(MonitorTriggerActionVO vo) {
        super(vo);
    }

    public static EmailTriggerActionInventory valueOf(EmailTriggerActionVO vo) {
        EmailTriggerActionInventory inv = new EmailTriggerActionInventory(vo);
        inv.email = vo.getEmail();
        inv.mediaUuid = vo.getMediaUuid();
        return inv;
    }

    public static List<EmailTriggerActionInventory> valueOf1(Collection<EmailTriggerActionVO> vos) {
        List<EmailTriggerActionInventory> invs = new ArrayList<>();
        for (EmailTriggerActionVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMediaUuid() {
        return mediaUuid;
    }

    public void setMediaUuid(String mediaUuid) {
        this.mediaUuid = mediaUuid;
    }
}
