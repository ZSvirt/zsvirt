
## Name

```
vm.cpuMode(CPU模式)
```

### Description

```
the cpu mode option, which could be used to enable nested virtualization, options are [none, host-model, host-passthrough, Haswell, Haswell-noTSX, Broadwell, Broadwell-noTSX, SandyBridge, IvyBridge, Conroe, Penryn, Nehalem, Westmere, Opteron_G1, Opteron_G2, Opteron_G3, Opteron_G4]. none: not use nested virtualization; host-model/host-passthrough will enable nested virtualization. When using host-passthrough, VM will see same CPU model in Host /proc/cpuinfo. When using host-model or host-passthrough, VM migration might be failed, due to mismatched CPU model. To use nested virtualization, user need to do some pre-configuration. Firstly, the /sys/module/kvm_intel/parameters/nested should be set as 'Y'; Secondly, the /usr/libexec/qemu-kvm binary should support nested feature as well. 
```

### 含义

```
配置云主机 CPU 模式
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
{none, host-model, host-passthrough, Dhyana, EPYC, EPYC-IBPB, Haswell, Haswell-noTSX, Broadwell, Broadwell-noTSX, SandyBridge, IvyBridge, Conroe, Penryn, Nehalem, Westmere, Opteron_G1, Opteron_G2, Opteron_G3, Opteron_G4, pentium, pentium2, pentium3, Kunpeng-920, FT-2000+, Tengyun-S2500}
```

### 取值范围补充说明

```
在前端，会额外暴露2个抽象的值，为 "使用集群设置" 和 "使用全局设置"
```

### DefaultValue

```
none
```

### 默认值补充说明

```
默认值为none，此时云主机 CPU 型号与物理机 CPU 型号不一致
```

### 支持的资源级配置

||
|---|
|org.zstack.header.vm.VmInstanceVO
|org.zstack.header.cluster.ClusterVO

### 资源粒度说明

```
全局颗粒度，集群颗粒度，VM 级别颗粒度
```

### 背景信息

```
无
```

### UI暴露

```
需要在UI暴露
```

### CLI手册暴露

```
需要在CLI手册暴露
```

## 注意事项

```
无
```
