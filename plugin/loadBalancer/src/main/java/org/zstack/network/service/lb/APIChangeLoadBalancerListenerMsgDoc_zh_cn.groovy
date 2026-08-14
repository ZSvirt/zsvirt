package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIChangeLoadBalancerListenerEvent

doc {
	title "ChangeLoadBalancerListener"

	category "loadBalancer"

	desc """修改负载均衡监听器参数"""

	rest {
		request {
			url "PUT /v1/load-balancers/listeners/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeLoadBalancerListenerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeLoadBalancerListener"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.4"
				}
				column {
					name "connectionIdleTimeout"
					enclosedIn "changeLoadBalancerListener"
					desc "空闲连接超时"
					location "body"
					type "Integer"
					optional true
					since "3.4"
				}
				column {
					name "maxConnection"
					enclosedIn "changeLoadBalancerListener"
					desc "最大并发连接数"
					location "body"
					type "Integer"
					optional true
					since "3.4"
				}
				column {
					name "balancerAlgorithm"
					enclosedIn "changeLoadBalancerListener"
					desc "负载均衡算法"
					location "body"
					type "String"
					optional true
					since "3.4"
					values ("weightroundrobin","roundrobin","leastconn","source")
				}
				column {
					name "healthCheckTarget"
					enclosedIn "changeLoadBalancerListener"
					desc "健康检查端口"
					location "body"
					type "String"
					optional true
					since "3.4"
				}
				column {
					name "healthyThreshold"
					enclosedIn "changeLoadBalancerListener"
					desc "健康检查阈值"
					location "body"
					type "Integer"
					optional true
					since "3.4"
				}
				column {
					name "unhealthyThreshold"
					enclosedIn "changeLoadBalancerListener"
					desc "非健康检查阈值"
					location "body"
					type "Integer"
					optional true
					since "3.4"
				}
				column {
					name "healthCheckInterval"
					enclosedIn "changeLoadBalancerListener"
					desc "健康检查间隔时间"
					location "body"
					type "Integer"
					optional true
					since "3.4"
				}
				column {
					name "healthCheckProtocol"
					enclosedIn "changeLoadBalancerListener"
					desc "负载均衡健康检查协议"
					location "body"
					type "String"
					optional true
					since "3.9"
					values ("tcp","udp","http")
				}
				column {
					name "healthCheckMethod"
					enclosedIn "changeLoadBalancerListener"
					desc "健康检查方法"
					location "body"
					type "String"
					optional true
					since "3.9"
					values ("GET","HEAD")
				}
				column {
					name "healthCheckURI"
					enclosedIn "changeLoadBalancerListener"
					desc "健康检查的URI"
					location "body"
					type "String"
					optional true
					since "3.9"
				}
				column {
					name "healthCheckHttpCode"
					enclosedIn "changeLoadBalancerListener"
					desc "健康检查期望的成功返回码"
					location "body"
					type "String"
					optional true
					since "3.9"
				}
				column {
					name "aclStatus"
					enclosedIn "changeLoadBalancerListener"
					desc "访问控制状态"
					location "body"
					type "String"
					optional true
					since "3.9"
					values ("enable","disable")
				}
				column {
					name "securityPolicyType"
					enclosedIn "changeLoadBalancerListener"
					desc "TLS安全策略"
					location "body"
					type "String"
					optional true
					since "3.11"
					values ("tls_cipher_policy_default","tls_cipher_policy_1_0","tls_cipher_policy_1_1","tls_cipher_policy_1_2","tls_cipher_policy_1_2_strict","tls_cipher_policy_1_2_strict_with_1_3")
				}
				column {
					name "nbprocess"
					enclosedIn "changeLoadBalancerListener"
					desc "负载均衡进程数量"
					location "body"
					type "Integer"
					optional true
					since "3.11"
				}
				column {
					name "httpMode"
					enclosedIn "changeLoadBalancerListener"
					desc "负载均衡HTTP模式"
					location "body"
					type "String"
					optional true
					since "3.11"
					values ("http-keep-alive","http-server-close","http-tunnel","httpclose","forceclose")
				}
				column {
					name "sessionPersistence"
					enclosedIn "changeLoadBalancerListener"
					desc "会话保持模式"
					location "body"
					type "String"
					optional true
					since "3.16"
					values ("disable","iphash","insert","rewrite")
				}
				column {
					name "sessionIdleTimeout"
					enclosedIn "changeLoadBalancerListener"
					desc "会话保持超时时间"
					location "body"
					type "Integer"
					optional true
					since "3.16"
				}
				column {
					name "cookieName"
					enclosedIn "changeLoadBalancerListener"
					desc "Cookie名称"
					location "body"
					type "String"
					optional true
					since "3.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.4"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.4"
				}
				column {
					name "httpRedirectHttps"
					enclosedIn "changeLoadBalancerListener"
					desc "http重定向https"
					location "body"
					type "String"
					optional true
					since "3.17.21"
					values ("disable","enable")
				}
				column {
					name "redirectPort"
					enclosedIn "changeLoadBalancerListener"
					desc "重定向端口"
					location "body"
					type "Integer"
					optional true
					since "3.17.21"
				}
				column {
					name "statusCode"
					enclosedIn "changeLoadBalancerListener"
					desc "重定向状态码"
					location "body"
					type "Integer"
					optional true
					since "3.17.21"
					values ("301","302","303","307","308")
				}
			}
		}

		response {
			clz APIChangeLoadBalancerListenerEvent.class
		}
	}
}