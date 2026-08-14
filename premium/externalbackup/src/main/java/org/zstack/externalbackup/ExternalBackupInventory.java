package org.zstack.externalbackup;

import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2019/12/3.
 */
@Inventory(mappingVOClass = ExternalBackupVO.class)
public class ExternalBackupInventory {
    protected String uuid;
    protected String name;
    protected String description;
    protected ExternalBackupState state;
    protected String installPath;
    protected long totalSize;
    protected String version;
    protected String type;
    protected Timestamp createDate;
    protected Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getInstallPath() {
        return installPath;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ExternalBackupState getState() {
        return state;
    }

    public void setState(ExternalBackupState state) {
        this.state = state;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ExternalBackupInventory() {
    }

    public ExternalBackupInventory(ExternalBackupVO vo) {
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        this.totalSize = vo.getTotalSize();
        this.version = vo.getVersion();
        this.type = vo.getType();
        this.state = vo.getState();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static ExternalBackupInventory valueOf(ExternalBackupVO vo) {
        return new ExternalBackupInventory(vo);
    }

    public static List<ExternalBackupInventory> valueOf(Collection<ExternalBackupVO> vos) {
        return vos.stream().map(ExternalBackupInventory::valueOf).collect(Collectors.toList());
    }

    public static ExternalBackupInventory __example__() {
        ExternalBackupInventory inv = new ExternalBackupInventory();
        inv.uuid = DocUtils.createFixedUuid(ExternalBackupVO.class);
        inv.name = "mybackup";
        inv.state = ExternalBackupState.Ready;
        inv.installPath = "/var/zbox-129ed716d2/zstack-backup/mybackup-3.9.0-6ecb68135490414793fc7d1233254a18";
        inv.totalSize = SizeUnit.GIGABYTE.toByte(500);
        inv.type = "zbox";
        inv.version = "3.9.0";
        return inv;
    }
}
