package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/host/mount-block-device",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIMountBlockDeviceEvent.class
)
public class APIMountBlockDeviceMsg extends APIMessage {
    @APIParam(maxLength = 255)
    private String username;
    @APIParam(required = false, maxLength = 255)
    @NoLogging
    private String password;
    @APIParam(numberRange = {1, 65535})
    private Integer sshPort;
    @APIParam(maxLength = 255)
    private String hostName;
    // regex guards against shell metachar injection in downstream ssh commands.
    @APIParam(maxLength = 255)
    private String path;
    @APIParam(maxLength = 2048)
    private String mountPoint;
    @APIParam(required = false, maxLength = 255, validValues = {"ext4", "xfs"})
    private String filesystemType = "xfs";

    public static class mkfsCommd {
        public static String buildMkfsCommd(String filesystemType, String blockDevicePath) {
            if (filesystemType.equals("ext4")) {
                return "mkfs.ext4 -F " + blockDevicePath;
            } else if (filesystemType.equals("xfs")) {
                return "mkfs.xfs -f " + blockDevicePath;
            }
            throw new IllegalArgumentException("unsupported filesystem type: " + filesystemType);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMountPoint() {
        return mountPoint;
    }

    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
    }

    public String getFilesystemType() {

        return filesystemType;
    }

    public void setFilesystemType(String filesystemType) {
        this.filesystemType = filesystemType;
    }

    public static APIMountBlockDeviceMsg __example__() {
        APIMountBlockDeviceMsg msg = new APIMountBlockDeviceMsg();
        msg.setUsername("username");
        msg.setPassword("password");
        msg.setSshPort(22);
        msg.setHostName("192.168.1.1");
        msg.setPath("/dev/vdb");
        msg.setMountPoint("/root/data");
        msg.setFilesystemType("xfs");
        return msg;
    }
}