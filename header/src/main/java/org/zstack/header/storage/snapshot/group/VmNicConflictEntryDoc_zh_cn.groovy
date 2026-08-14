package org.zstack.header.storage.snapshot.group

import org.zstack.header.vm.VmNicInventory

doc {

	title "网卡冲突信息"

	field {
		name "ip"
		desc "冲突的ip"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "mac"
		desc "冲突的mac"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "vmNicName"
		desc "网卡名称"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "vmInstanceName"
		desc "虚拟机名称"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "vmInstanceUuid"
		desc "虚拟机UUID"
		type "String"
		since "zsv 4.10.16"
	}
}
