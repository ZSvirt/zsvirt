package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIAttachL2NetworkToHostEvent

doc {
	title "挂载二层网络到物理机(AttachL2NetworkToHost)"

	category "二层网络"

	desc """挂载二层网络到物理机"""

	rest {
		request {
			url "POST /v1/l2-networks/{l2NetworkUuid}/hosts/{hostUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachL2NetworkToHostMsg.class

			desc """"""

			params {

				column {
					name "l2NetworkUuid"
					enclosedIn "params"
					desc "二层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.18.0"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "主机 UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.18.0"
				}
				column {
					name "l2ProviderType"
					enclosedIn "params"
					desc "二层网络实现类型"
					location "body"
					type "String"
					optional true
					since "zsv 3.18.0"
					values ("LinuxBridge")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.18.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.18.0"
				}
				column {
					name "hostParam"
					enclosedIn "params"
					desc "主机参数"
					location "body"
					type "String"
					optional true
					since "zsv 4.3.0"
				}
			}
		}

		response {
			clz APIAttachL2NetworkToHostEvent.class
		}
	}
}