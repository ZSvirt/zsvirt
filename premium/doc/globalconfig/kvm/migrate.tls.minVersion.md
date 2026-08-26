
## Name

```
migrate.tls.minVersion(热迁移TLS最低版本)
```

### Description

```
Minimum TLS version for KVM live migration.
```

### 含义

```
指定 KVM 热迁移 TLS 连接允许的最低 TLS 协议版本，用于约束 libvirt/QEMU 迁移通道的加密协议下限。
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
[-9223372036854775808, 9223372036854775807]
```

### 取值范围补充说明

```
常见取值为 TLSv1.2、TLSv1.3 等 libvirt/QEMU 支持的 TLS 版本标识；具体可用版本取决于计算节点 libvirt/QEMU 能力。
```

### DefaultValue

```
TLSv1.2
```

### 默认值补充说明

```
默认要求 TLS 1.2 及以上，兼顾安全性与兼容性。
```

### 支持的资源级配置



### 资源粒度说明

```
全局
```

### 背景信息

```
KVM 热迁移 TLS 加密能力引入，用于统一迁移通道的最低加密协议要求。
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
若设置的版本高于计算节点 libvirt/QEMU 实际支持能力，strict 策略下可能导致 TLS 迁移失败。
```
