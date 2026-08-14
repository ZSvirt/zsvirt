package org.zstack.header.vm.additions

doc {

	title "虚拟机在主机侧的相关文件或目录数据"

	field {
		name "uuid"
		desc "相关文件 UUID"
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
		name "hostUuid"
		desc "主机 UUID"
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "type"
		desc "文件类型, 按用途分类, 可能是 NvRam 或者 TpmState"
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "path"
		desc "主机侧相关文件或目录的路径"
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
}
