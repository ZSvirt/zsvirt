package org.zstack.header.volume.block

import java.lang.Integer
import java.lang.Long
import java.sql.Timestamp
import java.lang.Boolean

doc {

	title "块存储卷"
	
	field {
		name "iscsiPath"
		desc ""
		type "String"
		since "3.17.11"
	}
	field {
		name "vendor"
		desc ""
		type "String"
		since "3.17.11"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.17.11"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.17.11"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.17.11"
	}
	field {
		name "primaryStorageUuid"
		desc "主存储UUID"
		type "String"
		since "3.17.11"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "3.17.11"
	}
	field {
		name "diskOfferingUuid"
		desc "云盘规格UUID"
		type "String"
		since "3.17.11"
	}
	field {
		name "rootImageUuid"
		desc ""
		type "String"
		since "3.17.11"
	}
	field {
		name "installPath"
		desc ""
		type "String"
		since "3.17.11"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "3.17.11"
	}
	field {
		name "format"
		desc ""
		type "String"
		since "3.17.11"
	}
	field {
		name "size"
		desc ""
		type "Long"
		since "3.17.11"
	}
	field {
		name "actualSize"
		desc ""
		type "Long"
		since "3.17.11"
	}
	field {
		name "deviceId"
		desc ""
		type "Integer"
		since "3.17.11"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "3.17.11"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "3.17.11"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.17.11"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.17.11"
	}
	field {
		name "isShareable"
		desc ""
		type "Boolean"
		since "3.17.11"
	}
	field {
		name "volumeQos"
		desc ""
		type "String"
		since "3.17.11"
	}
	field {
		name "lastDetachDate"
		desc ""
		type "Timestamp"
		since "3.17.11"
	}
	field {
		name "lastVmInstanceUuid"
		desc ""
		type "String"
		since "3.17.11"
	}
}
