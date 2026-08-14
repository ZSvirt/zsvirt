package org.zstack.header.vm

import org.zstack.header.vm.CloneVmInstanceInventory

doc {

	title "在这里输入结构的名称"

	field {
		name "numberOfClonedVm"
		desc ""
		type "int"
		since "0.6"
	}
	ref {
		name "inventories"
		path "org.zstack.header.vm.CloneVmInstanceResults.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz CloneVmInstanceInventory.class
	}
}
