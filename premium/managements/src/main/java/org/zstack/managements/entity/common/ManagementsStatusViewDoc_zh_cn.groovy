package org.zstack.managements.entity.common

import org.zstack.managements.entity.common.ManagementNodeStatusView

doc {

	title "管理节点相关信息"

	field {
		name "vip"
		desc "VIP 地址"
		type "String"
		since "4.10.20"
	}
	field {
		name "uiHttpPath"
		desc "UI HTTP 路径"
		type "String"
		since "4.10.20"
	}
	ref {
		name "nodes"
		path "org.zstack.managements.entity.common.ManagementsStatusView.nodes"
		desc "管理节点信息列表"
		type "List"
		since "4.10.20"
		clz ManagementNodeStatusView.class
	}
}
