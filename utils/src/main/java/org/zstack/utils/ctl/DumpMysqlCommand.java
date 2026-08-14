package org.zstack.utils.ctl;

/**
 * @Author: DaoDao
 * @Date: 2021/12/17
 */
public class DumpMysqlCommand extends CtlCommandSpec{
    String ctlName= "dump_mysql";

    @Override
    String getCtlName() {
        return ctlName;
    }

    public DumpMysqlCommand fileName(String fileName) {
        CtlCommandParam param = new CtlCommandParam();
        param.option = "--file-name";
        param.arguments = fileName;
        addOption(param);
        return this;
    }

    public DumpMysqlCommand filePath(String filePath) {
        CtlCommandParam param = new CtlCommandParam();
        param.option = "--file-path";
        param.arguments = filePath;
        addOption(param);
        return this;
    }

    public DumpMysqlCommand keepAmount(long keepAmount) {
        CtlCommandParam param = new CtlCommandParam();
        param.option = "--keep-amount";
        param.arguments = String.valueOf(keepAmount);
        addOption(param);
        return this;
    }

    public DumpMysqlCommand hostInfo(String hostInfo) {
        CtlCommandParam param = new CtlCommandParam();
        param.option = "--host-info";
        param.arguments = String.valueOf(hostInfo);
        addOption(param);
        return this;
    }

    public DumpMysqlCommand deleteExpiredFile(String deleteExpiredFile) {
        CtlCommandParam param = new CtlCommandParam();
        param.option = "--delete-expired-file";
        param.arguments = deleteExpiredFile;
        addOption(param);
        return this;
    }

    public DumpMysqlCommand appendSqlFile(String appendSqlFile) {
        CtlCommandParam param = new CtlCommandParam();
        param.option = "--append-sql-file";
        param.arguments = appendSqlFile;
        addOption(param);
        return this;
    }
}
