package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APICreatePortGroupEvent

doc {
	title "创建端口组(CreatePortGroup)"

	category "network.l2"

	desc """创建端口组"""

	rest {
		request {
			url "POST /v1/l3-networks/port-group"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreatePortGroupMsg.class

			desc """"""

			params {

				column {
					name "vSwitchUuid"
					enclosedIn "params"
					desc "虚拟交换机UUID"
					location "body"
					type "String"
					optional false
					since "4.2.0"
				}
				column {
					name "vlanMode"
					enclosedIn "params"
					desc "vlan模式"
					location "body"
					type "String"
					optional true
					since "4.2.0"
					values ("ACCESS","TRUNK","PVLAN")
				}
				column {
					name "vlan"
					enclosedIn "params"
					desc "Vlan号"
					location "body"
					type "Integer"
					optional false
					since "4.2.0"
				}
				column {
					name "vlanRanges"
					enclosedIn "params"
					desc "vlan范围"
					location "body"
					type "String"
					optional true
					since "4.2.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "4.2.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.2.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "三层网络类型"
					location "body"
					type "String"
					optional true
					since "4.2.0"
				}
				column {
					name "l2NetworkUuid"
					enclosedIn "params"
					desc "二层网络UUID"
					location "body"
					type "String"
					optional false
					since "4.2.0"
				}
				column {
					name "category"
					enclosedIn "params"
					desc "网络类型，需要与system标签搭配使用，system为true时可设置为Public、Private"
					location "body"
					type "String"
					optional true
					since "4.2.0"
					values ("Public","Private","System")
				}
				column {
					name "ipVersion"
					enclosedIn "params"
					desc "ip协议号"
					location "body"
					type "Integer"
					optional true
					since "4.2.0"
					values ("4","6")
				}
				column {
					name "system"
					enclosedIn "params"
					desc "是否用于系统云主机"
					location "body"
					type "boolean"
					optional true
					since "4.2.0"
				}
				column {
					name "dnsDomain"
					enclosedIn "params"
					desc "DNS域"
					location "body"
					type "String"
					optional true
					since "4.2.0"
				}
				column {
					name "enableIPAM"
					enclosedIn "params"
					desc "IP地址管理是否启用"
					location "body"
					type "Boolean"
					optional true
					since "4.2.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "4.2.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "4.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.2.0"
				}
			}
		}

		response {
			clz APICreatePortGroupEvent.class
		}
	}
}