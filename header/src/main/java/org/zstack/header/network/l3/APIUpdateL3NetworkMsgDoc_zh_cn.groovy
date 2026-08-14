package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIUpdateL3NetworkEvent

doc {
	title "更新三层网络(UpdateL3Network)"

	category "三层网络"

	desc """更新三层网络"""

	rest {
		request {
			url "PUT /v1/l3-networks/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateL3NetworkMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateL3Network"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "updateL3Network"
					desc "三层网络的名称"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "updateL3Network"
					desc "三层网络的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "system"
					enclosedIn "updateL3Network"
					desc "是否用于系统云主机"
					location "body"
					type "Boolean"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "category"
					enclosedIn "updateL3Network"
					desc "网络类型，需要与system标签搭配使用，system为true时可设置为Public、Private"
					location "body"
					type "String"
					optional true
					since "zsv 2.2"
					values ("Public","Private","System")
				}
				column {
					name "dnsDomain"
					enclosedIn "updateL3Network"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 2.6.0"
				}
			}
		}

		response {
			clz APIUpdateL3NetworkEvent.class
		}
	}
}