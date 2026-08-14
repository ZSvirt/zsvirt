package org.zstack.externalservice.cronjob;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.externalservice.ExternalService;
import org.zstack.core.externalservice.ExternalServiceFactory;
import org.zstack.core.externalservice.ExternalServiceManager;
import org.zstack.core.externalservice.ExternalServiceType;

public class CronJobFactory implements ExternalServiceFactory {
    public static final ExternalServiceType type = new ExternalServiceType("CronJob");

    @Autowired
    private ExternalServiceManager manager;

    @Override
    public String getExternalServiceType() {
        return type.toString();
    }

    public CronJob getCronJob() {
        CronJob job = new CronJobImpl();
        return (CronJob) manager.getService(job.getName(), () -> job);
    }
}
