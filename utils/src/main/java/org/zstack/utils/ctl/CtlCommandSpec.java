package org.zstack.utils.ctl;

import org.zstack.utils.DescriptionBuilder;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: DaoDao
 * @Date: 2021/12/17
 */
public abstract class CtlCommandSpec {
    private static final CLogger _logger = CLoggerImpl.getLogger(CtlCommandSpec.class);
    List<CtlCommandParam> params = new ArrayList<>();
    void addOption(CtlCommandParam param) {
        params.add(param);
    };

    String build() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("zstack-ctl %s", getCtlName()));
        for (CtlCommandParam param : params) {
            if (param.arguments == null || param.option == null) {
                String args = param.arguments != null ? param.arguments : param.option;
                sb.append(String.format(" %s", args));
                continue;
            }
            sb.append(String.format(" %s %s", param.option, param.arguments));
        }
        sb.append(" >/dev/null");

        return sb.toString();
    }

    ZStackCtlResult run() {
        ZStackCtlResult result = new ZStackCtlResult();
        String ctlCommand = build();
        _logger.debug(ctlCommand);
        ShellResult shellResult = ShellUtils.runAndReturn(ctlCommand);
        if (!shellResult.isReturnCode(0)) {
            result.setError(shellResult.getStderr());
            result.setSuccess(false);
            return result;
        }
        return result;
    }

    public ZStackCtlResult exec() {
        return run();
    }
    abstract String getCtlName();

    class CtlCommandParam {
        String option;
        String arguments;
    }
}
