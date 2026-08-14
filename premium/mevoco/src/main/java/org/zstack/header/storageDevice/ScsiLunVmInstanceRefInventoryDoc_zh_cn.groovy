package org.zstack.header.storageDevice

import java.sql.Timestamp
import java.sql.Timestamp

doc {

    title "SCSI Lun与虚拟机关联关系"

    field {
        name "scsiLunUuid"
        desc "SCSI Lun UUID"
        type "String"
        since "3.1.0"
    }
    field {
        name "vmInstanceUuid"
        desc "云主机UUID"
        type "String"
        since "3.1.0"
    }
    field {
        name "createDate"
        desc "创建时间"
        type "Timestamp"
        since "3.1.0"
    }
    field {
        name "lastOpDate"
        desc "最后一次修改时间"
        type "Timestamp"
        since "3.1.0"
    }
}
