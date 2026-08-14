
## Name

```
soundType(声卡类型)
```

### Description

```
sound type for VM. "ich6" supply basic sound typen for VM.  Options:[ich6, ich9, ac97]
```

### 含义

```
开启spice开关后，为云主机配置声卡类型
```

### Type

```
java.lang.String
```

### Category

```
vm
```

### 取值范围

```
{ich6, ich9, ac97}
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
ich6
```

### 默认值补充说明

```
无
```

### 支持的资源级配置

||
|---|
|org.zstack.header.vm.VmInstanceVO
|org.zstack.header.cluster.ClusterVO

### 资源粒度说明

```
全局、集群、云主机
```

### 背景信息

```
某些平台的虚拟化组件并不支持当前的默认值ich6,需要修改其他配置
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
无
```
