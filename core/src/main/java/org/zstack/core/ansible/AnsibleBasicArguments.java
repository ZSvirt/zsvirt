package org.zstack.core.ansible;

import com.google.gson.annotations.SerializedName;
import org.zstack.header.log.NoLogging;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.io.Serializable;
import java.util.LinkedHashMap;

public class AnsibleBasicArguments implements Serializable {
    protected static final CLogger logger = Utils.getLogger(AnsibleBasicArguments.class);

    @SerializedName("pip_url")
    private String pipUrl;
    @SerializedName("trusted_host")
    private String trustedHost;
    @SerializedName("yum_server")
    private String yumServer;
    @SerializedName("remote_user")
    private String remoteUser;
    @NoLogging
    @SerializedName("remote_pass")
    private String remotePass;
    @SerializedName("remote_port")
    private String remotePort;
    @SerializedName("zstack_repo")
    private String zstackRepo;

    public String getZstackRepo() {
        return zstackRepo;
    }

    public void setZstackRepo(String zstackRepo) {
        this.zstackRepo = zstackRepo;
    }

    public String getPipUrl() {
        return pipUrl;
    }

    public void setPipUrl(String pipUrl) {
        this.pipUrl = pipUrl;
    }

    public String getTrustedHost() {
        return trustedHost;
    }

    public void setTrustedHost(String trustedHost) {
        this.trustedHost = trustedHost;
    }

    public String getYumServer() {
        return yumServer;
    }

    public void setYumServer(String yumServer) {
        this.yumServer = yumServer;
    }

    public String getRemoteUser() {
        return remoteUser;
    }

    public void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }

    public String getRemotePass() {
        return remotePass;
    }

    public void setRemotePass(String remotePass) {
        this.remotePass = remotePass;
    }

    public String getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(String remotePort) {
        this.remotePort = remotePort;
    }

    public LinkedHashMap<String, Object> toArgumentMap() {
        LinkedHashMap<String, Object> rehashedObject = JSONObjectUtil.rehashObject(this, LinkedHashMap.class);

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("object after rehashed: %s", JSONObjectUtil.toJsonString(rehashedObject)));
        }

        return rehashedObject;
    }
}
