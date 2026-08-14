package org.zstack.header.allocator.datatypes



doc {

	title "资源的CPU和内存容量信息结构"

	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "zsv 3.9.0"
	}
	field {
		name "totalCpu"
		desc "CPU总数"
		type "long"
		since "zsv 3.9.0"
	}
	field {
		name "availableCpu"
		desc "可用CPU数量"
		type "long"
		since "zsv 3.9.0"
	}
	field {
		name "totalMemory"
		desc "内存总量"
		type "long"
		since "zsv 3.9.0"
	}
	field {
		name "availableMemory"
		desc "可用内存"
		type "long"
		since "zsv 3.9.0"
	}
	field {
		name "managedCpuNum"
		desc "受管理的物理CPU数量"
		type "long"
		since "zsv 3.9.0"
	}
}
