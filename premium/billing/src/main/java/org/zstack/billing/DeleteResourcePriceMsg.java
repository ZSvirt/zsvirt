package org.zstack.billing;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2019/11/12.
 */
public class DeleteResourcePriceMsg extends NeedReplyMessage {
    private String uuid;

    private String tableUuid;

    private boolean cutoffPrice;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTableUuid() {
        return tableUuid;
    }

    public void setTableUuid(String tableUuid) {
        this.tableUuid = tableUuid;
    }

    public boolean isCutoffPrice() {
        return cutoffPrice;
    }

    public void setCutoffPrice(boolean cutoffPrice) {
        this.cutoffPrice = cutoffPrice;
    }
}
