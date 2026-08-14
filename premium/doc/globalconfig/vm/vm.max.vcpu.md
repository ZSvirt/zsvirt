
## Name

```
vm.max.vcpu(云主机最大vcpu数量)
```

### Description

```
vm's maximum vcpu number
```

### 含义

```
设置云主机最大vcpu数量
```

### Type

```
java.lang.Integer
```

### Category

```
vm
```

### 取值范围

```
[-2147483648, 2147483647]
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
128
```

### 默认值补充说明

```
不同的集群架构默认值不同

x86和arm集群为128

mips集群为8

loongarch64集群为32
```

### 支持的资源级配置

||
|---|
|org.zstack.header.vm.VmInstanceVO
|org.zstack.header.cluster.ClusterVO

### 资源粒度说明

```
在开启numa的情况下，如果需要设置云主机cpu拓扑，可能需要修改单台云主机的
 vcpu最大值
```

### 背景信息

```
无
```

### UI暴露

```
不暴露
```

### CLI手册暴露

```
不暴露
```

## 注意事项

```
目前仅对开启云主机cpu/内存热加载的机器生效
```
