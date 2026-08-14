package org.zstack.utils.zsha2;

import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;


/**
 * Created by mingjian.deng on 2020/4/2.
 */
public class ZSha2Helper {
    private static final CLogger logger = Utils.getLogger(ZSha2Helper.class);

    public static boolean isMNHaEnvironment() {
        return PathUtil.exists("/usr/local/bin/zsha2");
    }

    public static ZSha2Info getInfo() {
        return getInfo(true);
    }

    public static ZSha2Info getInfo(boolean checkZSha2Status) {
        ShellResult result;
        if (checkZSha2Status) {
            result = ShellUtils.runAndReturn("sudo -i /usr/local/bin/zsha2 status", false);
            if (!result.isReturnCode(0)) {
                throw new RuntimeException(String.format("cannot get zsha2 status, because %s", result.getStderr()));
            }
        }

        result = ShellUtils.runAndReturn("/usr/local/bin/zsha2 show-config");
        if (!result.isReturnCode(0)) {
            throw new RuntimeException(String.format("cannot get zsha2 config, because %s, maybe you need upgrade zsha2", result.getStderr()));
        }

        ZSha2Info info = JSONObjectUtil.toObject(result.getStdout(), ZSha2Info.class);

        info.setMaster(ShellUtils.runAndReturn(String.format(
                "ip addr show %s | grep -q '[^0-9]%s[^0-9]'", info.getNic(), info.getDbvip())).isReturnCode(0));
        return info;
    }

    public static ZSha2StatusJsonInfo getStatusInfo() {
        ShellResult result;

        result = ShellUtils.runAndReturn("/usr/local/bin/zsha2 status -json");
        if (!result.isReturnCode(0)) {
            throw new RuntimeException("cannot get zsha2 status json: " + result.getStderr());
        }

        return JSONObjectUtil.toObject(result.getStdout(), ZSha2StatusJsonInfo.class);
    }

    public static boolean checkConnectivityOfPeerNode() {
        return getStatusInfo().isPeerReachable();
    }
}
