package org.zstack.network.service.lb

import org.zstack.network.service.lb.APICreateLoadBalancerListenerEvent

doc {
	title "创建负载均衡监听器(CreateLoadBalancerListener)"

	category "负载均衡"

	desc """创建负载均衡监听器"""

	rest {
		request {
			url "POST /v1/load-balancers/{loadBalancerUuid}/listeners"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateLoadBalancerListenerMsg.class

			desc """"""

			params {

				column {
					name "loadBalancerUuid"
					enclosedIn "params"
					desc "负载均衡器UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "instancePort"
					enclosedIn "params"
					desc "云主机端口"
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "loadBalancerPort"
					enclosedIn "params"
					desc "负载均衡器端口"
					location "body"
					type "int"
					optional false
					since "0.6"
				}
				column {
					name "protocol"
					enclosedIn "params"
					desc "协议"
					location "body"
					type "String"
					optional true
					since "3.0"
					values ("udp","tcp","http","https")
				}
				column {
					name "certificateUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "healthCheckProtocol"
					enclosedIn "params"
					desc "健康检查协议"
					location "body"
					type "String"
					optional true
					since "3.9"
					values ("tcp","udp","http")
				}
				column {
					name "healthCheckMethod"
					enclosedIn "params"
					desc "健康检查方法"
					location "body"
					type "String"
					optional true
					since "3.9"
					values ("GET","HEAD")
				}
				column {
					name "healthCheckURI"
					enclosedIn "params"
					desc "健康检查的URI"
					location "body"
					type "String"
					optional true
					since "3.9"
				}
				column {
					name "healthCheckHttpCode"
					enclosedIn "params"
					desc "健康检查期望的返回码"
					location "body"
					type "String"
					optional true
					since "3.9"
				}
				column {
					name "aclStatus"
					enclosedIn "params"
					desc "访问控制策略状态"
					location "body"
					type "String"
					optional true
					since "3.9"
					values ("enable","disable")
				}
				column {
					name "aclUuids"
					enclosedIn "params"
					desc "访问控制策略组"
					location "body"
					type "List"
					optional true
					since "3.9"
				}
				column {
					name "aclType"
					enclosedIn "params"
					desc "访问控制策略类型"
					location "body"
					type "String"
					optional true
					since "3.9"
					values ("white","black")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
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
				column {
					name "securityPolicyType"
					enclosedIn "params"
					desc "TLS安全策略"
					location "body"
					type "String"
					optional true
					since "3.11"
					values ("tls_cipher_policy_default","tls_cipher_policy_1_0","tls_cipher_policy_1_1","tls_cipher_policy_1_2","tls_cipher_policy_1_2_strict","tls_cipher_policy_1_2_strict_with_1_3")
				}
			}
		}

		response {
			clz APICreateLoadBalancerListenerEvent.class
		}
	}
}