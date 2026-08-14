package org.zstack.header.vm.devices

doc {

	title "虚拟机资源元数据组清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
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
	ref {
		name "addressList"
		path "org.zstack.header.vm.devices.VmInstanceResourceMetadataGroupInventory.addressList"
		desc "虚拟机地址清单列表"
		type "List"
		since "zsv 4.10.16"
		clz VmInstanceResourceMetadataArchiveInventory.class
	}
}
