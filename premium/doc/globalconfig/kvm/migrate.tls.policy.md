
## Name

```
migrate.tls.policy(热迁移TLS策略)
```

### Description

```
TLS migration policy: strict or permissive.
```

### 含义

```
定义 KVM 热迁移在 TLS 启用场景下的门禁策略：证书或 TLS 条件不满足时的处理方式。
```

### Type

```
java.lang.String
```

### Category

```
kvm
```

### 取值范围

```
{strict, permissive}
```

### 取值范围补充说明

```
strict：证书未就绪、过期、吊销或 SAN 不匹配等异常时拒绝迁移（fail-closed）。
permissive：仅用于灰度验证；证书或 TLS 异常时可能回退明文迁移，生产环境不建议使用。
```

### DefaultValue

```
strict
```

### 默认值补充说明

```
默认严格策略，避免在证书异常时 silently 降级为明文迁移。
```

### 支持的资源级配置



### 资源粒度说明

```
全局
```

### 背景信息

```
KVM 热迁移 TLS 加密能力引入，需与证书生命周期及迁移前校验流程配合使用。
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
生产环境建议使用 strict；permissive 可能导致用户误以为迁移始终处于加密状态。
```
