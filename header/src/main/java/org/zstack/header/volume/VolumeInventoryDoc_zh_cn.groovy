package org.zstack.header.volume

import java.lang.Long
import java.lang.Integer
import java.sql.Timestamp
import java.lang.Boolean

doc {

	title "硬盘清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "primaryStorageUuid"
		desc "主存储UUID"
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "diskOfferingUuid"
		desc "云盘规格UUID"
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "rootImageUuid"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "installPath"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "format"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "size"
		desc ""
		type "Long"
		since "zsv 3.17.13"
	}
	field {
		name "actualSize"
		desc ""
		type "Long"
		since "zsv 3.17.13"
	}
	field {
		name "deviceId"
		desc ""
		type "Integer"
		since "zsv 3.17.13"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 3.17.13"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 3.17.13"
	}
	field {
		name "isShareable"
		desc ""
		type "Boolean"
		since "zsv 3.17.13"
	}
	field {
		name "volumeQos"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "lastDetachDate"
		desc ""
		type "Timestamp"
		since "zsv 3.17.13"
	}
	field {
		name "lastVmInstanceUuid"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
}
