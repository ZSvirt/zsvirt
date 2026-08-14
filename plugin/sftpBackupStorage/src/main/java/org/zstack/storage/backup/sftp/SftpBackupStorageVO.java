package org.zstack.storage.backup.sftp;

import org.zstack.core.convert.PasswordConverter;
import org.zstack.header.storage.backup.BackupStorageEO;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Convert;

@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = BackupStorageEO.class, needView = false)
@AutoDeleteTag
public class SftpBackupStorageVO extends BackupStorageVO {

    @Column
    private String hostname;
    @Column
    private String username;
    @Column
    @Convert(converter = PasswordConverter.class)
    private String password;
    @Column
    private int sshPort;

    public SftpBackupStorageVO() {
    }
    
    public SftpBackupStorageVO(BackupStorageVO vo) {
        super(vo);
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

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public int getSshPort() {
        return sshPort;
    }
    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }
}
