package org.zstack.header.host;

import org.zstack.core.db.Q;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;

/**
 * Created by longtao.wu@zstack.com on 21/12/01
 */
public class ChangeVmEmulatorPinningMsg extends NeedReplyMessage implements HostMessage {
    private String uuid;
    @NoLogging
    private String emulatorPinning;


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getEmulatorPinning() {
        return emulatorPinning;
    }

    public void setEmulatorPinning(String emulatorPinning) {
        this.emulatorPinning = emulatorPinning;
    }

    @Override
    public String getHostUuid() {
        return Q.New(VmInstanceVO.class).select(VmInstanceVO_.hostUuid)
                .eq(VmInstanceVO_.uuid, uuid).findValue();
    }
}
