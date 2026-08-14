package org.zstack.header.volume.block

import java.lang.Integer
import java.lang.Long
import java.sql.Timestamp
import java.lang.Boolean

doc {

	title "expon类型块存储卷"
	
	field {
		name "exponStatus"
		desc "华瑞存储上的卷状态"
		type "String"
		since "4.10.16"
	}
	field {
		name "iscsiPath"
		desc "iSCSI"
		type "String"
		since "4.10.16"
	}
	field {
		name "vendor"
		desc "存储厂商"
		type "String"
		since "4.10.16"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.10.16"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "4.10.16"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "4.10.16"
	}
	field {
		name "primaryStorageUuid"
		desc "主存储UUID"
		type "String"
		since "4.10.16"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "4.10.16"
	}
	field {
		name "diskOfferingUuid"
		desc "云盘规格UUID"
		type "String"
		since "4.10.16"
	}
	field {
		name "rootImageUuid"
		desc "根镜像UUID"
		type "String"
		since "4.10.16"
	}
	field {
		name "installPath"
		desc "安装路径"
		type "String"
		since "4.10.16"
	}
	field {
		name "type"
		desc "卷类型"
		type "String"
		since "4.10.16"
	}
	field {
		name "format"
		desc "卷格式"
		type "String"
		since "4.10.16"
	}
	field {
		name "size"
		desc "卷规格大小"
		type "Long"
		since "4.10.16"
	}
	field {
		name "actualSize"
		desc "卷真实大小"
		type "Long"
		since "4.10.16"
	}
	field {
		name "deviceId"
		desc ""
		type "Integer"
		since "4.10.16"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "4.10.16"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "4.10.16"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.10.16"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.10.16"
	}
	field {
		name "isShareable"
		desc ""
		type "Boolean"
		since "4.10.16"
	}
	field {
		name "volumeQos"
		desc ""
		type "String"
		since "4.10.16"
	}
	field {
		name "lastDetachDate"
		desc ""
		type "Timestamp"
		since "4.10.16"
	}
	field {
		name "lastVmInstanceUuid"
		desc ""
		type "String"
		since "4.10.16"
	}
}
