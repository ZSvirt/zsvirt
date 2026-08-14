package org.zstack.externalservice.cronjob;

import org.zstack.core.externalservice.LocalExternalService;

public interface CronJob extends LocalExternalService {
    void addJob(String job);
}
