package org.zstack.utils.ctl;

/**
 * @Author: DaoDao
 * @Date: 2022/8/24
 */
public class ConfigureCommand extends CtlCommandSpec {
    String ctlName= "configure";
    @Override
    String getCtlName() {
        return ctlName;
    }

    public ConfigureCommand Configure(String arguments) {
        CtlCommandParam param = new CtlCommandParam();
        param.arguments = arguments;
        addOption(param);
        return this;
    }
}
