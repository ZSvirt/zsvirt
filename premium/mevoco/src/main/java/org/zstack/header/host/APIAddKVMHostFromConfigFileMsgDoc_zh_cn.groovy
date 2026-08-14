package org.zstack.header.host

import org.zstack.header.host.APIAddHostFromConfigFileEvent

doc {
	title "AddKVMHostFromConfigFile"

	category "host"

	desc """通过文件添加物理机"""

	rest {
		request {
			url "POST /v1/hosts/kvm/from-file"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddKVMHostFromConfigFileMsg.class

			desc """"""

			params {

				column {
					name "hostInfo"
					enclosedIn "params"
					desc "经过base64编码的物理机信息"
					location "body"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIAddHostFromConfigFileEvent.class
		}
	}
}