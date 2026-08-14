package org.zstack.header.message;

/**
 * Created by MaJin on 2019/7/4.
 */
public abstract class APICheckPasswordMessage extends APIMessage {
    public abstract String getPassword();
}
