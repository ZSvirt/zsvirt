package org.zstack.header.image;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2020/9/14.
 */
public class CreateTemporaryRootVolumeTemplateFromVolumeMsg extends NeedReplyMessage implements CreateTemporaryRootVolumeTemplateMessage {
    private String volumeUuid;
    private String guestOsType;
    private String platform;
    private String architecture;
    private boolean system;
    private SessionInventory session;
    private boolean virtio = true;

    @Override
    public String getName() {
        return "temporary-image-from-volume-" + volumeUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
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
