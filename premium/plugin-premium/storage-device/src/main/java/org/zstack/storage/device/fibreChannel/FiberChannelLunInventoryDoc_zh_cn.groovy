package org.zstack.storage.device.fibreChannel


import org.zstack.header.storageDevice.ScsiLunHostRefInventory
import org.zstack.header.storageDevice.ScsiLunVmInstanceRefInventory

doc {

	title "FC SAN磁盘清淡"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.1.0"
	}
	field {
		name "fiberChannelStorageUuid"
		desc "FC SAN存储UUID"
		type "String"
		since "3.1.0"
	}
	field {
		name "wwid"
		desc "磁盘全局唯一表示"
		type "String"
		since "3.1.0"
	}
	field {
		name "vendor"
		desc "供应商"
		type "String"
		since "3.1.0"
	}
	field {
		name "model"
		desc "型号"
		type "String"
		since "3.1.0"
	}
	field {
		name "wwn"
		desc "磁盘WWN"
		type "String"
		since "3.1.0"
	}
	field {
		name "serial"
		desc "磁盘序列号"
		type "String"
		since "3.1.0"
	}
	field {
		name "type"
		desc "磁盘类型"
		type "String"
		since "3.1.0"
	}
	field {
		name "path"
		desc "磁盘路径"
		type "String"
		since "3.1.0"
	}
	field {
		name "size"
		desc "磁盘大小"
		type "Long"
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
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.1.0"
	}
	field {
		name "state"
		desc "启用状态"
		type "String"
		since "3.1.0"
	}
	ref {
		name "scsiLunHostRefs"
		path "org.zstack.storage.device.fibreChannel.FiberChannelLunInventory.scsiLunHostRefs"
		desc "SCSI Lun与物理机关联关系"
		type "List"
		since "3.1.0"
		clz ScsiLunHostRefInventory.class
	}
	ref {
		name "scsiLunVmInstanceRefs"
		path "org.zstack.storage.device.fibreChannel.FiberChannelLunInventory.scsiLunVmInstanceRefs"
		desc "SCSI Lun与虚拟机关联关系"
		type "List"
		since "3.1.0"
		clz ScsiLunVmInstanceRefInventory.class
	}
}
