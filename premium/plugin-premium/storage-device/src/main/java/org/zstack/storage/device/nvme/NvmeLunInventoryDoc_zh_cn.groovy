package org.zstack.storage.device.nvme

import org.zstack.storage.device.nvme.NvmeLunHostRefInventory
import java.lang.Long
import java.sql.Timestamp

doc {

	title "NVMe磁盘"

	field {
		name "nvmeTargetUuid"
		desc "所属NVMe设备UUID"
		type "String"
		since "3.16.21"
	}
	ref {
		name "nvmeLunHostRefs"
		path "org.zstack.storage.device.nvme.NvmeLunInventory.nvmeLunHostRefs"
		desc "null"
		type "List"
		since "3.16.21"
		clz NvmeLunHostRefInventory.class
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.16.21"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.16.21"
	}
	field {
		name "wwid"
		desc "全局唯一标识"
		type "String"
		since "3.16.21"
	}
	field {
		name "vendor"
		desc "供应商"
		type "String"
		since "3.16.21"
	}
	field {
		name "model"
		desc "型号"
		type "String"
		since "3.16.21"
	}
	field {
		name "wwn"
		desc ""
		type "String"
		since "3.16.21"
	}
	field {
		name "serial"
		desc "序列号"
		type "String"
		since "3.16.21"
	}
	field {
		name "type"
		desc "类型"
		type "String"
		since "3.16.21"
	}
	field {
		name "hctl"
		desc ""
		type "String"
		since "3.16.21"
	}
	field {
		name "path"
		desc "路径"
		type "String"
		since "3.16.21"
	}
	field {
		name "state"
		desc "状态"
		type "String"
		since "3.16.21"
	}
	field {
		name "size"
		desc "大小"
		type "Long"
		since "3.16.21"
	}
	field {
		name "multipathDeviceUuid"
		desc "多路径设备UUID"
		type "String"
		since "3.16.21"
	}
	field {
		name "source"
		desc "磁盘来源，NVMe"
		type "String"
		since "3.16.21"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.16.21"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.16.21"
	}
}
