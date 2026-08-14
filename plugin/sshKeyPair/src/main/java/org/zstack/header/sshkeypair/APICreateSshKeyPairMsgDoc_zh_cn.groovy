package org.zstack.header.sshkeypair

import org.zstack.header.sshkeypair.APICreateSshKeyPairEvent

doc {
	title "CreateSshKeyPair"

	category "sshKeyPair"

	desc """创建密钥对"""

	rest {
		request {
			url "POST /v1/ssh-key-pair"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSshKeyPairMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "publicKey"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
			clz APICreateSshKeyPairEvent.class
		}
	}
}