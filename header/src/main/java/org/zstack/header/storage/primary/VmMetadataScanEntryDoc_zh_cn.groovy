package org.zstack.header.storage.primary

doc {

	title "虚拟机元数据扫描数据"

	field {
		name "registrationStatus"
		desc "扫描时元数据与平台现有资源的注册关系：UNREGISTERED（未注册）、REGISTERED（已在当前主存储位置注册）、UUID_CONFLICT（存在同 UUID 或存储位置冲突，需要重生成 UUID）。元数据完整性由 incomplete 字段表示，提交注册时仍需重新校验。"
		type "String"
		since "zsv 5.2.0"
	}

	field {
		name "vmUuid"
		desc ""
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "vmName"
		desc ""
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "vmCategory"
		desc ""
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "architecture"
		desc ""
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "schemaVersion"
		desc ""
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "metadataPath"
		desc ""
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "hostUuid"
		desc ""
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "sizeBytes"
		desc ""
		type "Long"
		since "zsv 5.0.0"
	}
	field {
		name "lastUpdateTime"
		desc ""
		type "Long"
		since "zsv 5.0.0"
	}
	field {
		name "incomplete"
		desc ""
		type "boolean"
		since "zsv 5.0.0"
	}
}
