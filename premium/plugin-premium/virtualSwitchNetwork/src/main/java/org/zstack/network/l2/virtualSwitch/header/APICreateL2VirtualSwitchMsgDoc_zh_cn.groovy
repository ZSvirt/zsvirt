package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APICreateL2VirtualSwitchEvent

doc {
	title "创建虚拟交换机(CreateL2VirtualSwitch)"

	category "network.l2"

	desc """创建虚拟交换机"""

	rest {
		request {
			url "POST /v1/l2-networks/virtual-switch"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateL2VirtualSwitchMsg.class

			desc """"""

			params {

				column {
					name "isDistributed"
					enclosedIn "params"
					desc "是否为分布式"
					location "body"
					type "Boolean"
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
			clz APICreateL2VirtualSwitchEvent.class
		}
	}
}