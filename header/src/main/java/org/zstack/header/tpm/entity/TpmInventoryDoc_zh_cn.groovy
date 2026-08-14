package org.zstack.header.tpm.entity

import org.zstack.header.vm.additions.VmHostFileInventory

doc {

	title "TPM 信息"

	field {
		name "uuid"
		desc "TPM UUID"
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "name"
		desc "TPM 资源名称"
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "vmInstanceUuid"
		desc "虚拟机 UUID"
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 5.0.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 5.0.0"
	}
	ref {
		name "hostRefs"
		path "org.zstack.header.tpm.entity.TpmInventory.hostRefs"
		desc "TPM 与主机的相关数据列表"
		type "List"
		since "zsv 5.0.0"
		clz VmHostFileInventory.class
	}
}
