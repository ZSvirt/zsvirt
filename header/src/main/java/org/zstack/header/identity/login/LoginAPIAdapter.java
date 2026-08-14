package org.zstack.header.identity.login;

import org.zstack.header.message.APIMessage;

public interface LoginAPIAdapter {
    Class getMessageClass();

    APILogInMsg transferToAPILogInMsg(APIMessage msg);
}
