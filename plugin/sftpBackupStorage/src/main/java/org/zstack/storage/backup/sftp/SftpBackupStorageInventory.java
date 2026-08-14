package org.zstack.storage.backup.sftp;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.storage.backup.BackupStorageInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = SftpBackupStorageVO.class, collectionValueOfMethod="valueOf1",
        parent = {@Parent(inventoryClass = BackupStorageInventory.class, type = SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE)})
@PythonClassInventory
public class SftpBackupStorageInventory extends BackupStorageInventory {
    public String hostname;
    public String username;
    public Integer sshPort;

    protected SftpBackupStorageInventory(SftpBackupStorageVO vo) {
        super(vo);
        hostname = vo.getHostname();
        username = vo.getUsername();
        sshPort = vo.getSshPort();
    }

    public SftpBackupStorageInventory() {
    }

    public static SftpBackupStorageInventory valueOf(SftpBackupStorageVO vo) {
        SftpBackupStorageInventory inv = new SftpBackupStorageInventory(vo);
        return inv;
    }

    public static List<SftpBackupStorageInventory> valueOf1(Collection<SftpBackupStorageVO> vos) {
        List<SftpBackupStorageInventory> invs = new ArrayList<SftpBackupStorageInventory>(vos.size());
        for (SftpBackupStorageVO vo : vos) {
            SftpBackupStorageInventory inv = SftpBackupStorageInventory.valueOf(vo);
            invs.add(inv);
        }
        return invs;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {

        this.username = username;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }
}
