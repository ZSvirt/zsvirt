package org.zstack.softwarePackage.compute;

import org.zstack.header.log.NoLogging;

public class UploadSoftwarePackageToVmSpec {
    private String targetPath;
    private String username;
    private int sshPort;
    @NoLogging
    private String password;

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
