package org.zstack.header.vm

import org.zstack.header.vm.VirtualizerInfo

doc {

	title "资源的虚拟化软件信息"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.16.21"
	}
	field {
		name "resourceType"
		desc "资源类型"
		type "String"
		since "3.16.21"
	}
	ref {
		name "infoList"
		path "org.zstack.header.vm.VirtualizerInfoInventory.infoList"
		desc "资源的虚拟化软件信息详情列表"
		type "List"
		since "3.16.21"
		clz VirtualizerInfo.class
	}
	field {
		name "error"
		desc "出现的查询错误，可能为 null"
		type "List"
		since "3.16.21"
	}
}
