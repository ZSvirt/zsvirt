package org.zstack.guesttools.kvm;

import org.zstack.guesttools.GuestToolsAgentStatus;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.image.ImagePlatform;

import java.util.Objects;

import static org.zstack.utils.CollectionDSL.*;

/**
 * Created by Wenhao.Zhang on 21/07/19
 */
public class QueryGuestToolsInfoContext {
    private final String vmUuid;
    private final ImagePlatform platform;
    private final GuestToolsKvmCommands.GetVmGuestToolsInfoRsp apiResponse;
    private final PrometheusQueryHandler queryHandler;

    // for linux guest tools
    private GuestToolsVersion version = null;
    private GuestToolsAgentStatus status = null;

    @SuppressWarnings({"unchecked"})
    public QueryGuestToolsInfoContext(String vmUuid, ImagePlatform platform, PrometheusQueryHandler queryHandler) {
        this.vmUuid = vmUuid;
        this.platform = platform;
        this.queryHandler = queryHandler;

        this.apiResponse = new GuestToolsKvmCommands.GetVmGuestToolsInfoRsp();
        this.apiResponse.setFeatures(map());
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public ImagePlatform getPlatform() {
        return platform;
    }

    public GuestToolsVersion getVersion() {
        return this.version;
    }

    public String getVersionString() {
        return this.apiResponse.getVersion();
    }

    /**
     * version code corresponding table (linux guest tools)
     * 
     * ZStack Cloud 3.8.0                   - Linux Guest Tool 1.1.0
     * ZStack Cloud 3.8.18 +                - Linux Guest Tool 1.1.0.3.8.18
     * ZStack Cloud 3.8.6                   - Linux Guest Tool 1.1.1
     * ZStack Cloud 3.9.0 / 3.10.0 / 4.0.0  - Linux Guest Tool 1.1.1
     * ZStack Cloud 3.10.3                  - Linux Guest Tool 1.1.1.3.10.3
     * ZStack Cloud 4.1.0                   - Linux Guest Tool 4.1.0
     * ZStack Cloud 4.2.0 / 4.3.0           - Linux Guest Tool 4.2.0
     * ZStack Cloud 4.4.0 / 4.5.0           - Linux Guest Tool 4.4.0
     * ZStack Cloud 4.6.0                   - Linux Guest Tool 4.6.0
     */
    public void setVersion(String version) {
        this.apiResponse.setVersion(version);
    }

    public void setVersion(GuestToolsVersion version) {
        if (version == null) {
            this.version = null;
            return;
        }

        this.version = version;
        this.apiResponse.setVersion(version.toString());
    }

    public GuestToolsAgentStatus getStatus() {
        return status;
    }

    public void setStatus(GuestToolsAgentStatus status) {
        if (status == null) {
            this.status = null;
            return;
        }

        this.status = status;
        this.apiResponse.setStatus(status.toString());
    }

    public GuestToolsKvmCommands.GetVmGuestToolsInfoRsp getApiResponse() {
        return apiResponse;
    }

    public PrometheusQueryHandler getQueryHandler() {
        return queryHandler;
    }

    public void queryFromPrometheus(String metricName, Double defaultValue, ReturnValueCompletion<Double> completion) {
        Objects.requireNonNull(this.queryHandler);
        queryHandler.query(this.vmUuid, metricName, defaultValue, completion);
    }
}
