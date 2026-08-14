package org.zstack.header.storage.backup;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VolumeBackupVO.class, myField = "volumeBackupUuid", targetField = "uuid")
        },
        friends = {
                @EntityGraph.Neighbour(type = BackupStorageVO.class, myField = "backupStorageUuid", targetField = "uuid")
        }
)
public class VolumeBackupStorageRefVO implements Serializable {
        @Id
        @Column
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private long id;

        @Column
        @ForeignKey(parentEntityClass = VolumeBackupVO.class, onDeleteAction = ReferenceOption.CASCADE)
        private String volumeBackupUuid;

        @Column
        @ForeignKey(parentEntityClass = BackupStorageEO.class, onDeleteAction = ReferenceOption.CASCADE)
        private String backupStorageUuid;

        @Column
        @Enumerated(EnumType.STRING)
        private VolumeBackupStatus status;

        @Column
        private String installPath;

        @Column
        private Timestamp createDate;

        @Column
        private Timestamp lastOpDate;

        @PreUpdate
        private void preUpdate() {
                lastOpDate = null;
        }

        public long getId() {
                return id;
        }

        public void setId(long id) {
                this.id = id;
        }

        public String getVolumeBackupUuid() {
                return volumeBackupUuid;
        }

        public void setVolumeBackupUuid(String volumeBackupUuid) {
                this.volumeBackupUuid = volumeBackupUuid;
        }

        public String getBackupStorageUuid() {
                return backupStorageUuid;
        }

        public void setBackupStorageUuid(String backupStorageUuid) {
                this.backupStorageUuid = backupStorageUuid;
        }

        public VolumeBackupStatus getStatus() {
                return status;
        }

        public void setStatus(VolumeBackupStatus status) {
                this.status = status;
        }

        public String getInstallPath() {
                return installPath;
        }

        public void setInstallPath(String installPath) {
                this.installPath = installPath;
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
}
