package org.zstack.header.image;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2021/3/18.
 */
public class CreateTemporaryRootVolumeTemplateFromVolumeSnapshotMsg extends NeedReplyMessage
        implements CreateTemporaryRootVolumeTemplateMessage, CreateTemplateFromSnapshotMessage {
    private String snapshotUuid;
    private String guestOsType;
    private String platform;
    private String architecture;
    private boolean system;
    private SessionInventory session;
    private boolean virtio = true;

    @Override
    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    @Override
    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    @Override
    public String getName() {
        return "temporary-image-from-volume-snapshot-" + snapshotUuid;
    }

    @Override
    public String getGuestOsType() {
        return guestOsType;
    }

    @Override
    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    @Override
    public boolean isVirtio() {
        return virtio;
    }

    @Override
    public void setVirtio(boolean virtio) {
        this.virtio = virtio;
    }

    @Override
    public String getPlatform() {
        return platform;
    }

    @Override
    public void setPlatform(String platform) {
        this.platform = platform;
    }

    @Override
    public String getArchitecture() {
        return architecture;
    }

    @Override
    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    @Override
    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }
    @Override

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }
}
