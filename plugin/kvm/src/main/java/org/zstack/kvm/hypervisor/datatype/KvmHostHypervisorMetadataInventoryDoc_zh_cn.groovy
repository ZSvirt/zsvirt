package org.zstack.kvm.hypervisor.datatype

import java.sql.Timestamp

doc {

	title "支持的主机元数据"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该条目"
		type "String"
		since "3.16.21"
	}
	field {
		name "categoryUuid"
		desc "支持的主机类型条目 UUID"
		type "String"
		since "3.16.21"
	}
	field {
		name "managementNodeUuid"
		desc "管理节点 UUID"
		type "String"
		since "3.16.21"
	}
	field {
		name "hypervisor"
		desc "虚拟化软件名称，比如 \"qemu-kvm\""
		type "String"
		since "3.16.21"
	}
	field {
		name "version"
		desc "版本号，比如 \"4.2.0-632.g6a6222b.el7\""
		type "String"
		since "3.16.21"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.16.21"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.16.21"
	}
}
