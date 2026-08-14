package org.zstack.image;

import org.zstack.compute.vm.MevocoImageSystemTags;
import org.zstack.compute.vm.MevocoVmSystemTags;
import org.zstack.header.image.*;
import org.zstack.header.message.Message;
import org.zstack.tag.SystemTagCreator;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by mingjian.deng on 17/1/4.
 */
public class MevocoImageBase extends ImageBase {
    public MevocoImageBase(ImageVO vo) {
        super(vo);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APISetImageQgaMsg) {
            handle((APISetImageQgaMsg) msg);
        } else if (msg instanceof APIGetImageQgaMsg) {
            handle((APIGetImageQgaMsg) msg);
        } else if (msg instanceof APISetImageSecurityLevelMsg) {
            handle((APISetImageSecurityLevelMsg) msg);
        } else {
            super.handleMessage(msg);
        }
    }

    private void handle(final APISetImageSecurityLevelMsg msg) {
        APISetImageSecurityLevelEvent evt = new APISetImageSecurityLevelEvent(msg.getId());

        if (msg.getSecurityLevel() != null) {
            SystemTagCreator creator = MevocoImageSystemTags.SECURITY_LEVEL.newSystemTagCreator(msg.getUuid());
            creator.setTagByTokens(map(
                    e(MevocoImageSystemTags.SECURITY_LEVEL_TOKEN, msg.getSecurityLevel())
            ));
            creator.recreate = true;
            creator.create();
        } else {
            MevocoImageSystemTags.SECURITY_LEVEL.delete(self.getUuid());
        }

        bus.publish(evt);
    }

    private void handle(final APISetImageQgaMsg msg) {
        APISetImageQgaEvent evt = new APISetImageQgaEvent(msg.getId());
        if (msg.getEnable()){
            SystemTagCreator creator = ImageSystemTags.IMAGE_INJECT_QEMUGA.newSystemTagCreator(msg.getUuid());
            creator.inherent = false;
            creator.recreate = true;
            creator.create();
        }else{
            ImageSystemTags.IMAGE_INJECT_QEMUGA.delete(msg.getUuid());
        }
        bus.publish(evt);
    }


    private void handle(final APIGetImageQgaMsg msg) {
        APIGetImageQgaReply reply = new APIGetImageQgaReply();
        String qemuga = ImageSystemTags.IMAGE_INJECT_QEMUGA.getTag(msg.getUuid());
        if (qemuga == null) {
            reply.setEnable(false);
        } else {
            reply.setEnable(true);
        }
        bus.reply(msg, reply);
    }

    @Override
    protected ImageVO getSelf() {
        return super.getSelf();
    }

    @Override
    protected ImageInventory getSelfInventory() {
        return super.getSelfInventory();
    }
}
