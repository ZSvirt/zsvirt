package org.zstack.utils.ctl;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * @Author: DaoDao
 * @Date: 2021/12/16
 */
public class ZStackCtlHelper {
    private static final CLogger logger = Utils.getLogger(ZStackCtlHelper.class);
    public String command;
    public String arguments;


    @Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
    public static class ZStackCtlBuilder {
        private ZStackCtlHelper helper = new ZStackCtlHelper();

        public ZStackCtlBuilder command(String command) {
            helper.command = command;
            return this;
        }

        public ZStackCtlBuilder arguments(String ... args) {
            StringBuilder sb = new StringBuilder();
            for (String com : args) {
                sb.append(" " + com);
            }
            helper.arguments = sb.toString();
            return this;
        }

        public ZStackCtlResult run() {
            DebugUtils.Assert(helper.command != null, "command cannot be null");
            StringBuilder sb = new StringBuilder(String.format("zstack-ctl %s" , helper.command));
            if (helper.arguments != null) {
                sb.append(" " + helper.arguments);
            }
            sb.append(" >/dev/null");

            ZStackCtlResult result = new ZStackCtlResult();
            ShellResult shellResult = ShellUtils.runAndReturn(sb.toString());

            if (!shellResult.isReturnCode(0)) {
                result.setError(shellResult.getStderr());
                result.setSuccess(false);
                return result;
            }

            return result;
        }

    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public static ZStackCtlBuilder New() {
        return new ZStackCtlBuilder();
    }
}
