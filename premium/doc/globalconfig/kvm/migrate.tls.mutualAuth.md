
## Name

```
migrate.tls.mutualAuth(热迁移双向TLS认证开关)
```

### Description

```
Enable mutual TLS for KVM live migration.
```

### 含义

```
控制 KVM 热迁移是否启用双向 TLS（mTLS）。开启后，迁移连接需同时校验源端客户端证书与目标端服务端证书。
```

### Type

```
java.lang.Boolean
```

### Category

```
kvm
```

### 取值范围

```
[-9223372036854775808, 9223372036854775807]
```

### 取值范围补充说明

```
true 表示启用双向 TLS 认证；false 表示不启用双向认证。
```

### DefaultValue

```
true
```

### 默认值补充说明

```
默认开启双向认证，以提升迁移通道的身份校验强度。
```

### 支持的资源级配置



### 资源粒度说明

```
全局
```

### 背景信息

```
KVM 热迁移 TLS 加密能力引入，配合 migration 用途主机证书实现迁移端身份互信。
```

### UI暴露

```
是
```

### CLI手册暴露

```
是
```

## 注意事项

```
仅在 migrate.tls.enable 为 true 且实际发起 TLS 迁移时生效；修改后对新发起的迁移生效。
```
