package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIUpdateL2NetworkEvent

doc {
	title "更新二层网络(UpdateL2Network)"

	category "二层网络"

	desc """更新二层网络"""

	rest {
		request {
			url "PUT /v1/l2-networks/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateL2NetworkMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateL2Network"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "updateL2Network"
					desc "普通二层网络名称"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "updateL2Network"
					desc "普通二层网络的详细描述"
					location "body"
					type "String"
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
			}
		}

		response {
			clz APIUpdateL2NetworkEvent.class
		}
	}
}