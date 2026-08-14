package org.zstack.storage.device.multipath;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ResourceVO;
import org.zstack.storage.device.iscsi.IscsiLunVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Create by weiwang at 2018/8/1
 */
@Entity
@Table
@AutoDeleteTag
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = IscsiLunVO.class, myField = "uuid", targetField = "multipathDeviceUuid")
        }
)
public class MultipathDeviceVO extends ResourceVO {
    @Column
    private String uuid;

    @Column
    private String vendor;

    @Column
    private String model;

    @Column
    private Long size;

    @Column
    private String features;

    @Column
    private String hardwareHandler;

    @Column
    private String writePermission;
}
