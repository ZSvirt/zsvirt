package org.zstack.header.sshkeypair

import org.zstack.header.sshkeypair.APIUpdateSshKeyPairEvent

doc {
	title "UpdateSshKeyPair"

	category "sshKeyPair"

	desc """更新密钥对"""

	rest {
		request {
			url "PUT /v1/ssh-key-pair/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSshKeyPairMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateSshKeyPair"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "name"
					enclosedIn "updateSshKeyPair"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "description"
					enclosedIn "updateSshKeyPair"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APIUpdateSshKeyPairEvent.class
		}
	}
}