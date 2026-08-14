package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APIUpdatePortGroupEvent

doc {
	title "更新端口组(UpdatePortGroup)"

	category "network.l2"

	desc """更新端口组"""

	rest {
		request {
			url "PUT /v1/l3-networks/port-group/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdatePortGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updatePortGroup"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "4.2.0"
				}
				column {
					name "name"
					enclosedIn "updatePortGroup"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "4.2.0"
				}
				column {
					name "description"
					enclosedIn "updatePortGroup"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.2.0"
				}
				column {
					name "dnsDomain"
					enclosedIn "updatePortGroup"
					desc "DNS域"
					location "body"
					type "String"
					optional true
					since "4.2.0"
				}
				column {
					name "category"
					enclosedIn "updatePortGroup"
					desc "网络类型，需要与system标签搭配使用，system为true时可设置为Public、Private"
					location "body"
					type "String"
					optional true
					since "4.2.0"
					values ("Public","Private","System")
				}
				column {
					name "system"
					enclosedIn "updatePortGroup"
					desc "是否用于系统云主机"
					location "body"
					type "Boolean"
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
			clz APIUpdatePortGroupEvent.class
		}
	}
}