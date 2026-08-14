
## Name

```
vm.host.file.sync.concurrency(主机侧虚拟机文件同步并发度)
```

### Description

```
The concurrency level for syncing VM host files from KVM hosts
```

### 含义

```
KVM 主机侧虚拟机文件同时并发个数
```

### Type

```
java.lang.Integer
```

### Category

```
kvm
```

### 取值范围

```
[1, 30]
```

### 取值范围补充说明

```
最小 1，最大 30
```

### DefaultValue

```
5
```

### 默认值补充说明

```
默认并发 5 个同步操作
```

### 支持的资源级配置



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
不暴露
```

### CLI手册暴露

```
暴露
```

## 注意事项

```
无
```
