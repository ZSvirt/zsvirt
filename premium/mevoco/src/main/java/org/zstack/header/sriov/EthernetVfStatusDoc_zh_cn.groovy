package org.zstack.header.sriov



doc {

	title "网卡VF使用状态"

	field {
		name "Available"
		desc "可用"
		type "EthernetVfStatus"
		since "3.18.0"
	}
	field {
		name "Reserved"
		desc "预分配"
		type "EthernetVfStatus"
		since "3.18.0"
	}
	field {
		name "Attached"
		desc "已使用"
		type "EthernetVfStatus"
		since "3.18.0"
	}
	field {
		name "Releasing"
		desc "释放中"
		type "EthernetVfStatus"
		since "3.18.0"
	}
}
