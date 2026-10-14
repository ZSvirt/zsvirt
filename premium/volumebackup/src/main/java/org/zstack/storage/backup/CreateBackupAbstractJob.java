package org.zstack.storage.backup;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.scheduler.CascadeUpdate;
import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.header.storage.backup.BackupRetentionPolicy;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.scheduler.AbstractSchedulerJob;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

public abstract class CreateBackupAbstractJob<T, K> extends AbstractSchedulerJob<T, K> {
    private static final CLogger logger = Utils.getLogger(CreateBackupAbstractJob.class);
    protected BackupRetentionPolicy retentionPolicy;
    protected BackupRetentionPolicy remoteRetentionPolicy;
    @CascadeUpdate(resourceType = BackupStorageVO.class, disableWhenEmpty = true)
    protected List<String> backupStorageUuids;
    @CascadeUpdate(resourceType = BackupStorageVO.class)
    protected String remoteBackupStorageUuid;
    protected BackupQosStruct backupQosStruct;

    private static final List<Field> QOS_FIELDS = FieldUtils.getAllFields(BackupQosStruct.class);
    private static final Set<String> QOS_FIELD_NAMES = QOS_FIELDS.stream().map(Field::getName).collect(Collectors.toSet());

    public CreateBackupAbstractJob(CreateSchedulerJobDescMsg msg) {
        super(msg);
    }

    public CreateBackupAbstractJob() {
        super();
    }

    protected void mergeFields(Map<String, String> parameters) {
        if (parameters == null) {
            throw new OperationFailureException(Platform.operr("missing job parameters"));
        }

        mergeBackupStorage(parameters);
        mergeRetentionPolicy(parameters);
        mergeQosStruct(parameters);
        if (remoteBackupStorageUuid == null) {
            remoteRetentionPolicy = null;
        }
    }

    protected void mergeBackupStorage(Map<String, String> params) {
        List<String> oldBackupStorageUuids = this.backupStorageUuids;
        String oldRemoteBackupStorageUuid = this.remoteBackupStorageUuid;

        // set new value if present
        if (params.containsKey("backupStorageUuids")) {
            String value = params.get("backupStorageUuids");
            this.backupStorageUuids = Strings.isBlank(value) ? Collections.emptyList() :
                    Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        }

        if (params.containsKey("remoteBackupStorageUuid")) {
            String value = params.get("remoteBackupStorageUuid");
            this.remoteBackupStorageUuid = Strings.isBlank(value) ? null : value;
        }

        // skip validation if no change
        boolean noChange = Objects.equals(oldBackupStorageUuids, this.backupStorageUuids) &&
                Objects.equals(oldRemoteBackupStorageUuid, this.remoteBackupStorageUuid);
        if (noChange) {
            return;
        }

        // validate new value
        if (backupStorageUuids == null) {
            throw new OperationFailureException(Platform.operr("missing 'backupStorageUuids' in job parameters"));
        }

        if (backupStorageUuids.isEmpty()) {
            throw new OperationFailureException(Platform.operr("job parameter 'backupStorageUuids' is empty"));
        }

        for (String bsUuid: backupStorageUuids) {
            if (!Q.New(BackupStorageVO.class).eq(BackupStorageVO_.uuid, bsUuid).isExists()) {
                throw new OperationFailureException(Platform.operr("unexpected backup storage uuid: %s", bsUuid));
            }
        }
    }



    protected void mergeRetentionPolicy(Map<String, String> params) {
        BackupRetentionPolicy oldRetentionPolicy = this.retentionPolicy;
        BackupRetentionPolicy oldRemoteRetentionPolicy = this.remoteRetentionPolicy;

        // set new value if present
        if (params.containsKey("retentionType") || params.containsKey("retentionValue")) {
            this.retentionPolicy = BackupRetentionPolicy.valueOf(
                    params.getOrDefault("retentionType",
                            this.retentionPolicy == null ? null : this.retentionPolicy.getRetentionType().toString()),
                    params.getOrDefault("retentionValue",
                            this.retentionPolicy == null ? null : String.valueOf(this.retentionPolicy.getRetentionValue()))
            );
            if (this.retentionPolicy != null && oldRetentionPolicy != null) {
                this.retentionPolicy.setFullBackupRetentionValue(oldRetentionPolicy.getFullBackupRetentionValue());
            }
        }

        if (params.containsKey("remoteRetentionType") || params.containsKey("remoteRetentionValue")) {
            this.remoteRetentionPolicy = BackupRetentionPolicy.valueOf(
                    params.getOrDefault("remoteRetentionType",
                            this.remoteRetentionPolicy == null ? null : this.remoteRetentionPolicy.getRetentionType().toString()),
                    params.getOrDefault("remoteRetentionValue",
                            this.remoteRetentionPolicy == null ? null : String.valueOf(this.remoteRetentionPolicy.getRetentionValue()))
            );
            if (this.remoteRetentionPolicy != null && oldRemoteRetentionPolicy != null) {
                this.remoteRetentionPolicy.setFullBackupRetentionValue(oldRemoteRetentionPolicy.getFullBackupRetentionValue());
            }
        }

        // skip validation if no change
        boolean noChange = Objects.equals(oldRetentionPolicy, this.retentionPolicy) &&
                Objects.equals(oldRemoteRetentionPolicy, this.remoteRetentionPolicy);
        if (noChange) {
            return;
        }

        // validate new value
        if (this.retentionPolicy == null) {
            throw new OperationFailureException(Platform.operr("missing 'retentionType' and 'retentionValue' in job parameters"));
        } else {
            if (retentionPolicy.getRetentionType() == null) {
                throw new OperationFailureException(Platform.operr("missing 'retentionType' in job parameters"));
            }

            if (retentionPolicy.getRetentionValue() == 0) {
                throw new OperationFailureException(Platform.operr("missing 'retentionValue' in job parameters"));
            }
        }

        if (remoteRetentionPolicy != null) {
            if (remoteRetentionPolicy.getRetentionType() == null) {
                throw new OperationFailureException(Platform.operr("missing 'remoteRetentionType' in job parameters"));
            }

            if (remoteRetentionPolicy.getRetentionValue() == 0) {
                throw new OperationFailureException(Platform.operr("missing 'remoteRetentionValue' in job parameters"));
            }
        }
    }

    protected void mergeQosStruct(Map<String, String> params) {
        // set new value if present
        if (params.keySet().stream().noneMatch(QOS_FIELD_NAMES::contains)) {
            return;
        }

        if (backupQosStruct == null) {
            backupQosStruct = new BackupQosStruct();
        }

        QOS_FIELDS.forEach(field -> {
            String value = params.get(field.getName());
            if (value != null) {
                field.setAccessible(true);
                Long qos = StringUtils.isBlank(value) ? null : validateBandWidth(Long.parseLong(value));
                try {
                    field.set(backupQosStruct, qos);
                } catch (IllegalAccessException e) {
                    throw new CloudRuntimeException(e);
                }
            }
        });
    }


    private long validateBandWidth(long value) {
        if (value <= 0) {
            throw new OperationFailureException(argerr("bandWidth must be a positive number"));
        }

        if (value < 1024L) {
            logger.warn(String .format("bandWidth[%s] cannot be lower than 1024 Bps, set it to 1024", value));
            return 1024L;
        }
        return value;
    }

    public BackupRetentionPolicy getRetentionPolicy() {
        return retentionPolicy;
    }

    public void setRetentionPolicy(BackupRetentionPolicy retentionPolicy) {
        this.retentionPolicy = retentionPolicy;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public String getRemoteBackupStorageUuid() {
        return remoteBackupStorageUuid;
    }

    public void setRemoteBackupStorageUuid(String remoteBackupStorageUuid) {
        this.remoteBackupStorageUuid = remoteBackupStorageUuid;
    }

    public BackupRetentionPolicy getRemoteRetentionPolicy() {
        return remoteRetentionPolicy;
    }

    public void setRemoteRetentionPolicy(BackupRetentionPolicy remoteRetentionPolicy) {
        this.remoteRetentionPolicy = remoteRetentionPolicy;
    }

    public BackupQosStruct getBackupQosStruct() {
        return backupQosStruct;
    }

    public void setBackupQosStruct(BackupQosStruct backupQosStruct) {
        this.backupQosStruct = backupQosStruct;
    }
}
