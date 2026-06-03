
## Name

```
migrate.tls.enable(热迁移TLS加密开关)
```

### Description

```
Enable libvirt/QEMU native TLS for KVM live migration.
```

### 含义

```
控制 KVM 热迁移是否启用 libvirt/QEMU 原生 TLS 加密通道。开启后，在满足证书与策略条件时，迁移流量经 TLS 传输。
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
true 表示启用热迁移 TLS；false 表示不启用。
```

### DefaultValue

```
false
```

### 默认值补充说明

```
默认关闭，便于灰度上线；生产环境在证书就绪后可按需开启。
```

### 支持的资源级配置



### 资源粒度说明

```
全局
```

### 背景信息

```
KVM 热迁移 TLS 加密能力引入，用于保护迁移过程中的内存与状态数据。
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
开启后需确保源、目标计算节点 migration 用途证书已安装且有效；strict 策略下证书未就绪将拒绝迁移。
```
