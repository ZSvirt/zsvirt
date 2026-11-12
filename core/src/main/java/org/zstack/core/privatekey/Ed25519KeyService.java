package org.zstack.core.privatekey;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.AbstractService;
import org.zstack.header.message.Message;
import org.zstack.header.privatekey.PrepareEd25519KeyMsg;
import org.zstack.header.privatekey.PrepareEd25519KeyReply;
import org.zstack.header.privatekey.PrivateKeyConstant;

import java.io.IOException;

import static org.zstack.core.Platform.operr;

public class Ed25519KeyService extends AbstractService {
    @Autowired
    private CloudBus bus;

    public void init() {
    }

    public void destroy() {
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof PrepareEd25519KeyMsg) {
            handle((PrepareEd25519KeyMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(PrepareEd25519KeyMsg msg) {
        PrepareEd25519KeyReply reply = new PrepareEd25519KeyReply();
        try {
            Ed25519KeyClient client = Platform.New(Ed25519KeyClient::new);
            reply.setPublicKey(client.ensureCommonsKeyPair());
            bus.reply(msg, reply);
        } catch (IOException e) {
            bus.replyErrorByMessageType(msg, operr("failed to prepare ED25519 key pair: %s", e.getMessage()));
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(PrivateKeyConstant.ED25519_SERVICE_ID);
    }
}
