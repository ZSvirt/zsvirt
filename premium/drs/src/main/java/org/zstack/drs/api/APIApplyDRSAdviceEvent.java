package org.zstack.drs.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Create by lining at 2019/12/12
 */
@RestResponse(fieldsTo = "all")
public class APIApplyDRSAdviceEvent extends APIEvent {
    private String vmMigrationActivityUuid;

    public APIApplyDRSAdviceEvent(String apiId) {
        super(apiId);
    }

    public APIApplyDRSAdviceEvent() {
        super(null);
    }

    public String getVmMigrationActivityUuid() {
        return vmMigrationActivityUuid;
    }

    public void setVmMigrationActivityUuid(String vmMigrationActivityUuid) {
        this.vmMigrationActivityUuid = vmMigrationActivityUuid;
    }
}
