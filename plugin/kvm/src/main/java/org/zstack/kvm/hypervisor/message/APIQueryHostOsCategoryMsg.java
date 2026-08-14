package org.zstack.kvm.hypervisor.message;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.kvm.hypervisor.datatype.HostOsCategoryInventory;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryHostOsCategoryReply.class, inventoryClass = HostOsCategoryInventory.class)
@RestRequest(
        path = "/hosts/os/category",
        responseClass = APIQueryHostOsCategoryReply.class,
        method = HttpMethod.GET
)
public class APIQueryHostOsCategoryMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("architecture=x86_64", "osReleaseVersion=\"centos core 7.6.1810\"");
    }
}
