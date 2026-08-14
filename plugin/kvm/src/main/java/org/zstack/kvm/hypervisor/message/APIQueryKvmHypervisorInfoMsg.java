package org.zstack.kvm.hypervisor.message;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoInventory;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by Wenhao.Zhang on 23/02/23
 */
@AutoQuery(replyClass = APIQueryKvmHypervisorInfoReply.class, inventoryClass = KvmHypervisorInfoInventory.class)
@RestRequest(
        path = "/hosts/kvm/hypervisor/info",
        responseClass = APIQueryKvmHypervisorInfoReply.class,
        method = HttpMethod.GET
)
public class APIQueryKvmHypervisorInfoMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }
}
