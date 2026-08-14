package org.zstack.header.vm.devices

doc {

	title "虚拟机资源元数据归档清单"

	field {
		name "id"
		desc ""
		type "long"
		since "zsv 4.10.16"
	}
	field {
		name "resourceUuid"
		desc "资源 UUID"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "vmInstanceUuid"
		desc "虚拟机 UUID"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "deviceAddress"
		desc "设备地址"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "addressGroupUuid"
		desc ""
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "metadata"
		desc ""
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "metadataClass"
		desc ""
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 4.10.16"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 4.10.16"
	}
}
