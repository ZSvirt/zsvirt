package org.zstack.header.tpm.entity

import java.sql.Timestamp
import org.zstack.header.vm.additions.VmHostFileInventory

doc {

	title "TPM 详情"

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
		name "fileRefs"
		path "org.zstack.header.tpm.entity.TpmCapabilityView.fileRefs"
		desc "TPM 相关的主机侧文件或目录数据列表"
		type "List"
		since "zsv 5.0.0"
		clz VmHostFileInventory.class
	}
	field {
		name "edkVersion"
		desc "EDK 套件版本"
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "swtpmVersion"
		desc "SWTPM 版本"
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "resetTpmAfterVmCloneConfig"
		desc "是否在虚拟机克隆后重置 TPM 状态的配置"
		type "boolean"
		since "zsv 5.0.0"
	}
}
