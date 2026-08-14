
## Name

```
vm.cpuid.vendor(云主机cpu厂商)
```

### Description

```
set vm cpuid vendor
```

### 含义

```
设置云主机cpu厂商
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
{None, AuthenticAMD}
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
AuthenticAMD
```

### 默认值补充说明

```
无
```

### 支持的资源级配置

||
|---|
|org.zstack.header.vm.VmInstanceVO

### 资源粒度说明

```
无
```

### 背景信息

```
海光物理机兼容兼容旧版本云主机启动，需要修改cpu厂商
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
目前仅对海光物理机上的云主机生效
```
