package org.zstack.utils.ctl;

/**
 * @Author: DaoDao
 * @Date: 2021/12/17
 */
public class RestoreMysqlCommand extends CtlCommandSpec{
    private String ctlName = "restore_mysql";

    @Override
    String getCtlName() {
        return ctlName;
    }

    public RestoreMysqlCommand fromFile(String fromFile) {
        CtlCommandParam param = new CtlCommandParam();
        param.option = "--from-file";
        param.arguments = fromFile;
        addOption(param);
        return this;
    }

    public RestoreMysqlCommand mysqlRootPassword(String password) {
        CtlCommandParam param = new CtlCommandParam();
        param.option = "--mysql-root-password";
        param.arguments = password;
        addOption(param);
        return this;
    }

    public RestoreMysqlCommand uiMysqlRootPassword(String password) {
        CtlCommandParam param = new CtlCommandParam();
        param.option = "--ui-mysql-root-password";
        param.arguments = password;
        addOption(param);
        return this;
    }

    public RestoreMysqlCommand skipUi() {
        CtlCommandParam param = new CtlCommandParam();
        param.option = "--skip-ui";
        addOption(param);
        return this;
    }

    public RestoreMysqlCommand onlyRestoreSelf() {
        CtlCommandParam param = new CtlCommandParam();
        param.option = "--only-restore-self";
        addOption(param);
        return this;
    }

    public RestoreMysqlCommand skipCheck() {
        CtlCommandParam param = new CtlCommandParam();
        param.option = "--skip-check";
        addOption(param);
        return this;
    }
}
