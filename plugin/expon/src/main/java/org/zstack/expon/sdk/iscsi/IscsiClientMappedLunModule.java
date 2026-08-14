package org.zstack.expon.sdk.iscsi;

/**
 * @example {
 * "cap_health_status": "health",
 * "cap_health_status_reason": "",
 * "client_id": "bdec2cad-cac1-4c38-8ce9-8e8d78232d15",
 * "create_time": 1705473244611,
 * "data_size": 0,
 * "delete_time": null,
 * "description": "",
 * "id": "41f0edc2-79e1-4e81-add3-2950110b23d8",
 * "io_priority": "default",
 * "is_delete": false,
 * "is_readonly": false,
 * "iscsi_gw_id": "fcdea7d0-5200-4f14-b326-6ee8e2bc53dd",
 * "name": "test",
 * "perf_health_status": "health",
 * "perf_health_status_reason": "",
 * "pool_name": "pool",
 * "qos_status": false,
 * "run_status": "normal",
 * "update_time": 1705567347995,
 * "use_status": "offline",
 * "verify_enabled": false,
 * "volume_name": "volume-687d4349-3c73-4c23-9a0f-c5836b6beb1e",
 * "volume_size": 1073741824,
 * "volume_type": "normal_volume"
 * }
 */
public class IscsiClientMappedLunModule {
    private String id;
    private String name;
    private String description;
    private String volumeName;
    private String poolName;
    private String iscsiGwId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getIscsiGwId() {
        return iscsiGwId;
    }

    public void setIscsiGwId(String iscsiGwId) {
        this.iscsiGwId = iscsiGwId;
    }
}
