package org.zstack.ipsec;

/**
 * Created by xing5 on 2016/11/3.
 */
public interface IPsecManager {
    public IPsecBackend getBackend(String l3Uuid);
}
