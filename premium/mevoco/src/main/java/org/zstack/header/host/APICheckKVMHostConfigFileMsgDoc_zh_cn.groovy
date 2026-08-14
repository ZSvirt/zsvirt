package org.zstack.header.host

import org.zstack.header.host.APICheckHostConfigFileReply

doc {
	title "CheckKVMHostConfigFile"

	category "host"

	desc """检查添加物理机文件合法性"""

	rest {
		request {
			url "POST /v1/hosts/kvm/from-file/check"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICheckKVMHostConfigFileMsg.class

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
			}
		}

		response {
			clz APICheckHostConfigFileReply.class
		}
	}
}