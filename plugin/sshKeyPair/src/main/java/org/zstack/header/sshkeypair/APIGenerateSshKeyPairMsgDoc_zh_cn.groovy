package org.zstack.header.sshkeypair

import org.zstack.header.sshkeypair.APIGenerateSshKeyPairReply

doc {
	title "GenerateSshKeyPair"

	category "sshKeyPair"

	desc """生成密钥对"""

	rest {
		request {
			url "POST /v1/ssh-key-pair/generate"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGenerateSshKeyPairMsg.class

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
			clz APIGenerateSshKeyPairReply.class
		}
	}
}