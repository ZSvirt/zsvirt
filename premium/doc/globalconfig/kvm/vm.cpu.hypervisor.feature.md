
## Name

```
vm.cpu.hypervisor.feature(是否需要云主机cpu的hypervisor特性)
```

### Description

```
enable or disable hypervisor feature in guest cpuid
```

### 含义

```
云主机的cpuid中会记录特性hypervisor用于表示云主机是否运行在
虚拟化环境，提供一个开关，用于关闭该特性，跳过特定云主机应用
的虚拟化环境检测
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
{true, false}
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
true
```

### 默认值补充说明

```
默认不会屏蔽hypervisor特性标识符
```

### 支持的资源级配置

||
|---|
|org.zstack.header.vm.VmInstanceVO
|org.zstack.header.cluster.ClusterVO

### 资源粒度说明

```
该配置属于特殊场景，目前支持云主机级别修改即可
```

### 背景信息

```
ZSTAC-52579
```

### UI暴露

```
需要在云主机详情页高级设置支持
```

### CLI手册暴露

```
不需要
```

## 注意事项

```
可能存在兼容性以及性能问题，谨慎使用
```
