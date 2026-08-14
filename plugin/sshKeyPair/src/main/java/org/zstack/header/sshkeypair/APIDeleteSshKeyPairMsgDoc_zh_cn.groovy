package org.zstack.header.sshkeypair

import org.zstack.header.sshkeypair.APIDeleteSshKeyPairEvent

doc {
	title "DeleteSshKeyPair"

	category "sshKeyPair"

	desc """删除密钥对"""

	rest {
		request {
			url "DELETE /v1/ssh-key-pair/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteSshKeyPairMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APIDeleteSshKeyPairEvent.class
		}
	}
}