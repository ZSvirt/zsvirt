package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIAddStorageProtocolEvent

doc {
	title "AddStorageProtocol"

	category "storage.primary"

	desc """添加存储协议"""

	rest {
		request {
			url "POST /v1/primary-storage/protocols"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddStorageProtocolMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "outputProtocol"
					enclosedIn "params"
					desc "输出协议"
					location "body"
					type "String"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
			}
		}

		response {
			clz APIAddStorageProtocolEvent.class
		}
	}
}