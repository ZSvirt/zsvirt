package org.zstack.header.network.l3;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = IpRangeEO.class)
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@AutoDeleteTag
public class AddressPoolVO extends IpRangeVO {
}
