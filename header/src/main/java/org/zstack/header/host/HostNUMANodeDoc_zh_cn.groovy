package org.zstack.header.host

import java.lang.Long
import java.lang.Long

doc {

	title "主机 NUMA Node"

	field {
		name "distance"
		desc "NUMA node 距离"
		type "List"
		since "zsv 3.13.12"
	}
	field {
		name "cpus"
		desc "NUMA node 所有的 CPU 列表"
		type "List"
		since "zsv 3.13.12"
	}
	field {
		name "free"
		desc "NUMA node 可用内存大小，单位 Byte"
		type "Long"
		since "zsv 3.13.12"
	}
	field {
		name "size"
		desc "NUMA node内存大小，单位 Byte"
		type "Long"
		since "zsv 3.13.12"
	}
	field {
		name "nodeID"
		desc "NUMA node ID"
		type "String"
		since "zsv 3.13.12"
	}
	field {
		name "VMsUuid"
		desc "关联 NUMA node 的虚拟机 UUID 列表"
		type "List"
		since "zsv 3.13.12"
	}
}
