package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APICreateL2PortGroupEvent

doc {
	title "创建端口组L2(CreateL2PortGroup)"

	category "network.l2"

	desc """创建端口组L2"""

	rest {
		request {
			url "POST /v1/l2-networks/port-group"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateL2PortGroupMsg.class

			desc """"""

			params {

				column {
					name "vSwitchUuid"
					enclosedIn "params"
					desc "虚拟交换机UUID"
					location "body"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "vlanMode"
					enclosedIn "params"
					desc "vlan模式"
					location "body"
					type "String"
					optional true
					since "3.17.0"
					values ("ACCESS","TRUNK","PVLAN")
				}
				column {
					name "vlan"
					enclosedIn "params"
					desc "Vlan号"
					location "body"
					type "Integer"
					optional false
					since "3.17.0"
				}
				column {
					name "vlanRanges"
					enclosedIn "params"
					desc "vlan范围"
					location "body"
					type "String"
					optional true
					since "3.17.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.17.0"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "physicalInterface"
					enclosedIn "params"
					desc "物理网卡"
					location "body"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "二层网络类型"
					location "body"
					type "String"
					optional true
					since "3.17.0"
				}
				column {
					name "vSwitchType"
					enclosedIn "params"
					desc "虚拟交换机类型"
					location "body"
					type "String"
					optional true
					since "3.17.0"
					values ("LinuxBridge","OvsDpdk","MacVlan")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.17.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
			}
		}

		response {
			clz APICreateL2PortGroupEvent.class
		}
	}
}