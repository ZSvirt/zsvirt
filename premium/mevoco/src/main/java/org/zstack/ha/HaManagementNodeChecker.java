package org.zstack.ha;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.storage.primary.PrimaryStorageType;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;

import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/3/29.
 */
public class HaManagementNodeChecker implements HaHostChecker {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ThreadFacade thdf;

    @Override
    public void check(CheckerStruct struct, HaCheckerCompletion completion) {
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                doCheck(struct, completion);
                return null;
            }

            @Override
            public String getName() {
                return String.format("HaManagementNodeChecker.check Host[uuid:%s, ip:%s]", struct.getHostUuid(), struct.getHostIp());
            }

            @Override
            public String getSyncSignature() {
                return "HaManagementNodeChecker.check";
            }

            @Override
            public int getSyncLevel() {
                return 10;
            }
        });

    }

    private boolean arpingTargetHost(CheckerStruct struct) {
        ShellResult res = ShellUtils.runAndReturn(String.format("ip r get %s | grep -Eo 'dev.*src' | awk '{print $2}'", struct.getHostIp()));

        if (!res.isReturnCode(0)) {
            return false;
        }

        String networkInterface = StringUtils.strip(res.getStdout());

        res = ShellUtils.runAndReturn(String.format("arping -I %s %s -c 1 -w 3", networkInterface, struct.getHostIp()));

        return res.isReturnCode(0);
    }

    private boolean nmapScanTargetHost(CheckerStruct struct) {
        ShellResult res = ShellUtils.runAndReturn(String.format("nmap -sP -PI %s | grep 'Host is up'", struct.getHostIp()));

        return res.isReturnCode(0);
    }

    private void doCheck(CheckerStruct struct, HaCheckerCompletion completion) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            completion.noWay();
            return;
        }

        int success = 0;

        for (int i = 0; i < struct.getMaxTimes(); i++) {
            if (arpingTargetHost(struct) || nmapScanTargetHost(struct)) {
                success++;
                if (success >= struct.getSuccessTimes()) {
                    completion.success(null);
                    return;
                }
            }

            try {
                TimeUnit.SECONDS.sleep(struct.getInterval());
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

        if (success >= struct.getSuccessTimes()) {
            completion.success(null);
        } else if (success == 0) {
            completion.fail(operr("the management node fails to scan the host"));
        } else {
            completion.notStable();
        }
    }

    @Override
    public int getWeight() {
        return 1;
    }
}
