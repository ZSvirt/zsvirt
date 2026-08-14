package org.zstack.header.sshkeypair;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * @Author: ya.wang
 * @Date: 8/11/23 4:17 PM
 */
@GlobalPropertyDefinition
public class SshKeyPairGlobalProperty {
    @GlobalProperty(name="upgradeSshKeyPairFromSystemTag", defaultValue = "false")
    public static boolean UPGRADE_SSH_KEY_PAIR_FROM_SYSTEM_TAG;
}
