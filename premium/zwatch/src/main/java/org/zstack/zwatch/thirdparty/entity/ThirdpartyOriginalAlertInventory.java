package org.zstack.zwatch.thirdparty.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;
import org.zstack.zwatch.ZWatchConstants;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = ThirdpartyOriginalAlertVO.class, collectionValueOfMethod = "valueOf1")
public class ThirdpartyOriginalAlertInventory implements Serializable {
    private String uuid;
    private String thirdpartyPlatformUuid;
    private String product;
    private String service;
    private String metric;
    private String alertLevel;
    private Timestamp alertTime;
    private String dimensions;
    private String message;
    private String dataSource;
    private String sourceText;
    private String readStatus;
    private Timestamp createDate;

    protected ThirdpartyOriginalAlertInventory(ThirdpartyOriginalAlertVO vo) {
        this.setUuid(vo.getUuid());
        this.setThirdpartyPlatformUuid(vo.getThirdpartyPlatformUuid());
        this.setProduct(vo.getProduct());
        this.setService(vo.getService());
        this.setMetric(vo.getMetric());
        this.setAlertLevel(vo.getAlertLevel());
        this.setAlertTime(vo.getAlertTime());
        this.setDimensions(vo.getDimensions());
        this.setMessage(vo.getMessage());
        this.setDataSource(vo.getDataSource());
        this.setSourceText(vo.getSourceText());
        this.setReadStatus(vo.getReadStatus());
        this.setCreateDate(vo.getCreateDate());
    }

    public static ThirdpartyOriginalAlertInventory valueOf(ThirdpartyOriginalAlertVO vo) {
        return new ThirdpartyOriginalAlertInventory(vo);
    }

    public static List<ThirdpartyOriginalAlertInventory> valueOf1(Collection<ThirdpartyOriginalAlertVO> vos) {
        List<ThirdpartyOriginalAlertInventory> invs = new ArrayList<ThirdpartyOriginalAlertInventory>(vos.size());
        for (ThirdpartyOriginalAlertVO vo : vos) {
            invs.add(ThirdpartyOriginalAlertInventory.valueOf(vo));
        }
        return invs;
    }

    public ThirdpartyOriginalAlertInventory() {
    }

    public static ThirdpartyOriginalAlertInventory __example__() {
        ThirdpartyOriginalAlertInventory inventory = new ThirdpartyOriginalAlertInventory();
        inventory.uuid = "08fc895bae9d412b9dbbfcbd24ded0b3";
        inventory.thirdpartyPlatformUuid = "06f51d66b43f4b32b41d42b764be5230";
        inventory.product = "XSKY";
        inventory.service = "XSKY";
        inventory.metric = "osd::osd-status";
        inventory.alertLevel = "Emergent";
        inventory.alertTime = DocUtils.timestamp();
        inventory.dimensions = "{'resource_name':'osd.0'}";
        inventory.message = "osd[osd.0] osd-status reonline";
        inventory.dataSource = "xsky-172.20.196.185";
        inventory.sourceText = "{\"ack_time\":\"0001-01-01T00:00:00Z\",\"acked\":false,\"alarm_id\":\"01-02-01-0008\",\"alert_value\":\"reonline\",\"create\":\"2020-07-16T03:27:40.561937Z\",\"data\":{\"action_status\":\"active\",\"create\":\"2020-07-06T12:49:16.870963Z\",\"data_dir\":\"/var/lib/ceph/osd/ceph-0\",\"disk\":{\"action_status\":\"active\",\"bytes\":1.099511627776E12,\"cache_create\":\"0001-01-01T00:00:00Z\",\"channel_id\":\"\",\"create\":\"2020-07-06T12:40:46.578222Z\",\"device\":\"sda\",\"disk_type\":\"HDD\",\"driver_type\":\"Unknown\",\"enclosure_id\":\"\",\"host\":{\"action_status\":\"active\",\"admin_ip\":\"172.20.193.210\",\"clock_diff\":-3137.0,\"cores\":8.0,\"cpu_model\":\"Intel(R) Xeon(R) CPU E5-2630 v3 @ 2.40GHz\",\"create\":\"2020-07-06T12:33:02.425485Z\",\"description\":\"\",\"disk_num\":0.0,\"gateway_ips\":\"10.0.0.179\",\"id\":3.0,\"is_master_db\":false,\"memory_kbyte\":1.62661E7,\"model\":\"KVM\",\"name\":\"zstack-3\",\"os\":\"CentOS Linux release 7.6.1810 (Core)\",\"private_ip\":\"10.0.0.179\",\"protection_domain\":{\"create\":\"2020-07-06T11:27:49.696197Z\",\"description\":\"The first protection domain\",\"id\":1,\"name\":\"default\",\"placement_node\":{\"create\":\"0001-01-01T00:00:00Z\",\"id\":1,\"name\":\"\",\"properties\":{\"is_witness\":false},\"type\":\"\",\"update\":\"0001-01-01T00:00:00Z\"},\"status\":\"active\",\"update\":\"2020-07-06T11:27:49.696199Z\"},\"public_ips\":\"10.0.0.179\",\"rack\":\"\",\"roles\":\"admin,monitor,block_storage_gateway\",\"status\":\"active\",\"type\":\"storage_server\",\"up\":true,\"update\":\"2020-07-06T12:33:02.425488Z\",\"vendor\":\"Red Hat\"},\"id\":3.0,\"is_cache\":false,\"is_root\":false,\"lighting_status\":\"disabled\",\"model\":\"QEMU QEMU HARDDISK\",\"partition_num\":0.0,\"power_safe\":false,\"rotation_rate\":\"5400 rpm\",\"rotational\":true,\"serial\":\"\",\"slot_id\":\"\",\"status\":\"active\",\"update\":\"2020-07-06T12:40:46.578226Z\",\"used\":false,\"wwid\":\"wwn-0x000ffe24137458e9\"},\"exit_count\":0.0,\"exit_time\":\"0001-01-01T00:00:00Z\",\"host\":{\"action_status\":\"active\",\"admin_ip\":\"172.20.193.210\",\"clock_diff\":-3137.0,\"cores\":8.0,\"cpu_model\":\"Intel(R) Xeon(R) CPU E5-2630 v3 @ 2.40GHz\",\"create\":\"2020-07-06T12:33:02.425485Z\",\"description\":\"\",\"disk_num\":0.0,\"gateway_ips\":\"10.0.0.179\",\"id\":3.0,\"is_master_db\":false,\"memory_kbyte\":1.62661E7,\"model\":\"KVM\",\"name\":\"zstack-3\",\"os\":\"CentOS Linux release 7.6.1810 (Core)\",\"private_ip\":\"10.0.0.179\",\"protection_domain\":{\"create\":\"2020-07-06T11:27:49.696197Z\",\"description\":\"The first protection domain\",\"id\":1,\"name\":\"default\",\"placement_node\":{\"create\":\"0001-01-01T00:00:00Z\",\"id\":1,\"name\":\"\",\"properties\":{\"is_witness\":false},\"type\":\"\",\"update\":\"0001-01-01T00:00:00Z\"},\"status\":\"active\",\"update\":\"2020-07-06T11:27:49.696199Z\"},\"public_ips\":\"10.0.0.179\",\"rack\":\"\",\"roles\":\"admin,monitor,block_storage_gateway\",\"status\":\"active\",\"type\":\"storage_server\",\"up\":true,\"update\":\"2020-07-06T12:33:02.425488Z\",\"vendor\":\"Red Hat\"},\"id\":1,\"in\":true,\"init_time\":\"2020-07-06T12:49:58.17692Z\",\"last_scrub_time\":\"2020-07-14T16:37:52.435474Z\",\"meta_bytes\":2.4696061952E10,\"name\":\"osd.0\",\"omap_byte\":0.0,\"osd_id\":0.0,\"pool\":{\"action_status\":\"active\",\"block_volume_num\":0.0,\"coding_chunk_num\":0.0,\"create\":\"2020-07-06T12:51:56.106138Z\",\"data_chunk_num\":0.0,\"default_managed_volume_format\":128.0,\"device_type\":\"HDD\",\"device_type_check_disabled\":false,\"failure_domain_type\":\"host\",\"hidden\":false,\"id\":1,\"io_bypass_enabled\":false,\"io_bypass_threshold\":0.0,\"name\":\"pool-1\",\"osd_num\":0.0,\"pool_id\":1.0,\"pool_mode\":\"\",\"pool_name\":\"pool-8811978518ec40bba3b77982fb4baba3\",\"pool_role\":\"data\",\"pool_type\":\"replicated\",\"protection_domain\":{\"create\":\"2020-07-06T11:27:49.696197Z\",\"description\":\"The first protection domain\",\"id\":1,\"name\":\"default\",\"placement_node\":{\"create\":\"0001-01-01T00:00:00Z\",\"id\":1,\"name\":\"\",\"properties\":{\"is_witness\":false},\"type\":\"\",\"update\":\"0001-01-01T00:00:00Z\"},\"status\":\"active\",\"update\":\"2020-07-06T11:27:49.696199Z\"},\"qos\":{\"bandwidth\":1.048576E7,\"bandwidth_max\":0.0,\"client_threshold\":0.0,\"id\":1,\"mode\":1.0,\"recovery_rate_type\":\"low\"},\"replicate_size\":3.0,\"status\":\"active\",\"stretched\":false,\"update\":\"2020-07-06T12:51:56.106139Z\"},\"read_cache_size\":2.68435456E8,\"role\":\"data\",\"status\":\"active\",\"type\":\"HDD\",\"up\":true,\"update\":\"2020-07-06T12:49:16.870966Z\",\"uuid\":\"70eb3d98-c94d-48cd-8fcb-62d468734051\"},\"error_records\":[],\"extra_data\":\"\",\"group\":\"osd-status\",\"host\":{\"admin_ip\":\"172.20.193.210\",\"id\":3.0,\"name\":\"zstack-3\"},\"id\":446.0,\"level\":\"info\",\"related_resources\":[{\"resource_id\":3,\"resource_type\":\"host\"},{\"resource_id\":1,\"resource_type\":\"pool\"},{\"resource_id\":3,\"resource_type\":\"disk\"}],\"resolve_time\":\"2020-07-16T03:27:50.532436Z\",\"resolve_type\":\"auto\",\"resolved\":true,\"resource_id\":1,\"resource_name\":\"osd.0\",\"resource_type\":\"osd\",\"status\":\"\",\"trigger_mode\":\"eq\",\"trigger_period\":0,\"trigger_value\":\"reonline\",\"type\":\"event\"}";
        inventory.readStatus = ZWatchConstants.DATA_READ_STATUS_UNREAD;
        inventory.createDate = DocUtils.timestamp();
        return inventory;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String $paramName) {
        uuid = $paramName;
    }

    public String getThirdpartyPlatformUuid() {
        return thirdpartyPlatformUuid;
    }

    public void setThirdpartyPlatformUuid(String $paramName) {
        thirdpartyPlatformUuid = $paramName;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String $paramName) {
        product = $paramName;
    }

    public String getService() {
        return service;
    }

    public void setService(String $paramName) {
        service = $paramName;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String $paramName) {
        metric = $paramName;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(String $paramName) {
        alertLevel = $paramName;
    }

    public Timestamp getAlertTime() {
        return alertTime;
    }

    public void setAlertTime(Timestamp $paramName) {
        alertTime = $paramName;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String $paramName) {
        dimensions = $paramName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String $paramName) {
        message = $paramName;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String $paramName) {
        dataSource = $paramName;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String $paramName) {
        sourceText = $paramName;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp $paramName) {
        createDate = $paramName;
    }

    public String getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(String readStatus) {
        this.readStatus = readStatus;
    }
}
