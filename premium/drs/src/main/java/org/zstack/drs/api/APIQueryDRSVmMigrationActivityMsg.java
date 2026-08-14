package org.zstack.drs.api;

import org.springframework.http.HttpMethod;
import org.zstack.drs.entity.DRSAdviceInventory;
import org.zstack.drs.entity.DRSVmMigrationActivityInventory;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by lining on 2019/12/12.
 */
@AutoQuery(replyClass = APIQueryDRSVmMigrationActivityReply.class, inventoryClass = DRSVmMigrationActivityInventory.class)
@RestRequest(
        path = "/clusters/drs/vm-migration-activities",
        optionalPaths = {"/clusters/drs/vm-migration-activities/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryDRSVmMigrationActivityReply.class
)
public class APIQueryDRSVmMigrationActivityMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
