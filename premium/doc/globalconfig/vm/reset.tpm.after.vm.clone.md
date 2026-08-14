
## Name

```
reset.tpm.after.vm.clone(克隆后重置 TPM 状态)
```

### Description

```
whether reset TPM state after VM clone
```

### 含义

```
该虚拟机克隆（或从快照中创建新虚拟机）后是否重置 TPM 状态
```

### Type

```
java.lang.Boolean
```

### Category

```
vm
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
默认重置 TPM 状态
```

### 支持的资源级配置

||
|---|
|org.zstack.header.vm.VmInstanceVO
|org.zstack.header.cluster.ClusterVO

### 资源粒度说明

```
无
```

### 背景信息

```
无
```

### UI暴露

```
暴露
```

### CLI手册暴露

```
暴露
```

## 注意事项

```
注意重置 TPM 状态后，如果做了磁盘加密等操作的虚拟机可能无法启动，
注意在快照或克隆前，虚拟机需要执行解密磁盘等操作
```
