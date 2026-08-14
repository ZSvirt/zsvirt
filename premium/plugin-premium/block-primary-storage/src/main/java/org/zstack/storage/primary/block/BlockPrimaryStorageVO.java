package org.zstack.storage.primary.block;

import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;

import javax.persistence.*;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/3/18 16:24
 */

@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
@EO(EOClazz = PrimaryStorageEO.class, needView = false)
@AutoDeleteTag
public class BlockPrimaryStorageVO extends PrimaryStorageVO{
        @Column
        private String vendorName;

        @Column(columnDefinition = "Text")
        @Lob
        private String metadata;

        public BlockPrimaryStorageVO() {
        }

        public BlockPrimaryStorageVO(PrimaryStorageVO other) {
                super(other);
        }

        public BlockPrimaryStorageVO(BlockPrimaryStorageVO other) {
                super(other);
                this.vendorName = other.vendorName;
                this.metadata = other.metadata;
        }

        public void setVendorName(String vendorName) {
                this.vendorName = vendorName;
        }

        public String getVendorName() {
                return vendorName;
        }

        public void setMetadata(String metadata) {
                this.metadata = metadata;
        }

        public String getMetadata() {
                return metadata;
        }
}
