package org.zstack.header.sshkeypair

import org.zstack.header.sshkeypair.APIAttachSshKeyPairToVmInstanceEvent

doc {
	title "AttachSshKeyPairToVmInstance"

	category "sshKeyPair"

	desc """云主机挂载密钥对"""

	rest {
		request {
			url "POST /v1/ssh-key-pair/{sshKeyPairUuid}/vm-instance/{vmInstanceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachSshKeyPairToVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "sshKeyPairUuid"
					enclosedIn "params"
					desc ""
					location "url"
					type "String"
					optional false
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
			clz APIAttachSshKeyPairToVmInstanceEvent.class
		}
	}
}