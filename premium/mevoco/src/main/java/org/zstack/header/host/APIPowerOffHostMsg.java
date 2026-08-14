package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICheckPasswordMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by MaJin on 2019/6/28.
 */
@RestRequest(
        path = "/hosts/power-off/actions",
        method = HttpMethod.PUT,
        responseClass = APIPowerOffHostEvent.class,
        isAction = true
)
public class APIPowerOffHostMsg extends APICheckPasswordMessage {
    @APIParam(nonempty = true, noTrim = true, password = true)
    @NoLogging
    private String adminPassword;
    @APIParam(nonempty = true, resourceType = HostVO.class)
    private List<String> hostUuids;
    @APIParam(required = false)
    private boolean waitTaskCompleted;
    @APIParam(required = false)
    private Long maxWaitTime;

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public Long getMaxWaitTime() {
        return maxWaitTime;
    }

    public void setMaxWaitTime(Long maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    public boolean isWaitTaskCompleted() {
        return waitTaskCompleted;
    }

    public void setWaitTaskCompleted(boolean waitTaskCompleted) {
        this.waitTaskCompleted = waitTaskCompleted;
    }

    @Override
    public String getPassword() {
        return adminPassword;
    }

    public static APIPowerOffHostMsg __example__() {
        APIPowerOffHostMsg msg = new APIPowerOffHostMsg();
        msg.setHostUuids(list(uuid(HostVO.class)));
        msg.setAdminPassword("password");
        return msg;
    }
}
