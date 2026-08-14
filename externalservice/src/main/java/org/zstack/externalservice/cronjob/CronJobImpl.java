package org.zstack.externalservice.cronjob;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.core.externalservice.AbstractLocalExternalService;
import org.zstack.header.core.external.service.ExternalServiceCapabilities;
import org.zstack.core.externalservice.ExternalServiceCapabilitiesBuilder;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Bash;
import org.zstack.utils.path.PathUtil;
import static org.zstack.core.Platform.operr;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CronJobImpl extends AbstractLocalExternalService implements CronJob {
    ExternalServiceCapabilities capabilities = ExternalServiceCapabilitiesBuilder
            .build()
            .reloadConfig(true);

    @Override
    protected String[] getCommandLineKeywords() {
        return new String[] {"crond", "-n"};
    }

    @Override
    public String getName() {
        return String.format("cron-job-on-machine-%s", Platform.getManagementServerIp());
    }

    @Override
    public void start() {
        if (isAlive()) {
            return;
        }

        new Bash() {
            @Override
            protected void scripts() {
                setE();

                sudoRun("service crond start");
            }
        }.execute();
    }

    @Override
    public boolean isAlive() {
        return getPID() != null;
    }

    @Override
    public ExternalServiceCapabilities getExternalServiceCapabilities() {
        return capabilities;
    }

    @Override
    public void reload() {
        if (!isAlive()) {
            throw new OperationFailureException(operr("crond is not running"));
        }

        new Bash() {
            @Override
            protected void scripts() {
                setE();

                sudoRun("service crond reload");
            }
        }.execute();
    }

    @Override
    public void addJob(String job) {
        if (!isAlive()) {
            throw new OperationFailureException(operr("crond is not running"));
        }

        new Bash() {
            @Override
            protected void scripts() {
                setE();

                sudoRun("crontab -l");

                Set<String> jobs = new HashSet<>();
                Collections.addAll(jobs, stdout().split("\n"));
                jobs.add(job);

                File tmpJobFile = new File(PathUtil.join(System.getProperty("java.io.tmpdir"), "crond"));
                try {
                    FileUtils.writeStringToFile(tmpJobFile, StringUtils.join(jobs, "\n"));
                } catch (IOException e) {
                    throw new CloudRuntimeException(e);
                }

                sudoRun("crontab %s", tmpJobFile.getAbsolutePath());
            }
        }.execute();
    }
}
