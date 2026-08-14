
## Name

```
vm.metadata.payload.rejectThreshold(元数据载荷拒绝阈值)
```

### Description

```
Max allowed VM metadata payload size in bytes. VMs exceeding this are skipped.
```

### 含义

```
单个VM元数据JSON载荷的最大允许字节数，超过此阈值的VM刷盘操作将被跳过
```

### Type

```
java.lang.Long
```

### Category

```
vm
```

### 取值范围

```
[0, 9223372036854775807]
```

### 取值范围补充说明

```
单位为字节，正常云主机元数据通常在KB到数百KB级别
```

### DefaultValue

```
33554432
```

### 默认值补充说明

```
默认32MB（33554432字节），预留了大量余量
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
保护性配置，防止因数据异常向主存储写入超大文件
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
被拒绝的VM脏行不会被清除，Poller仍会尝试处理。如确认元数据合法只是偏大可增大此值
```
