package org.zstack.header.vm



doc {

	title "资源的虚拟化软件信息详情"

	field {
		name "hypervisor"
		desc "虚拟化软件，一般为\"qemu-kvm\""
		type "String"
		since "3.16.21"
	}
	field {
		name "currentVersion"
		desc "云主机当前使用的虚拟化软件版本 / 物理机当前安装的虚拟化软件版本"
		type "String"
		since "3.16.21"
	}
	field {
		name "expectVersion"
		desc "期望的虚拟化软件版本。如果待查询的是 VM，则期望的虚拟化软件版本来自物理机；如果待查询的是物理机，则期望的虚拟化软件版本来自管理节点"
		type "String"
		since "3.16.21"
	}
	field {
		name "matchState"
		desc "当前虚拟化软件版本和期望是否匹配。如果当前版本或期望有一方没有查到，则返回 \"Unknown\""
		type "String"
		since "3.16.21"
	}
}
