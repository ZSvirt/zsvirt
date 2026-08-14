package org.zstack.header.vm

doc {

	title "从虚拟机模板创建虚拟机的结果"

	field {
		name "numberOfClonedVm"
		desc ""
		type "int"
		since "zsv 4.2.6"
	}
	ref {
		name "inventories"
		path "org.zstack.header.vm.CreateVmInstanceFromTemplatedVmInstanceResults.inventories"
		desc "null"
		type "List"
		since "zsv 4.2.6"
		clz CloneVmInstanceInventory.class
	}
}
