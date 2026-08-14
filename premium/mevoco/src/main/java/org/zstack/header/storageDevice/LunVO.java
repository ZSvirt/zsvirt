package org.zstack.header.storageDevice;

import org.zstack.header.tag.AutoDeleteTag;

import javax.persistence.*;


@Entity
@Table
@AutoDeleteTag
@DiscriminatorColumn(name="source", discriminatorType = DiscriminatorType.STRING)
public class LunVO extends LunAO {
    public LunVO() {
    }
}
