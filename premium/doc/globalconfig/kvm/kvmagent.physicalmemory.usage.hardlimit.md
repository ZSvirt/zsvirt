
## Name

```
kvmagent.physicalmemory.usage.hardlimit(kvmagent服务物理内存使用硬限制)
```

### Description

```
The hard limit for the physical memory usage of the kvmagent process, exceeding this value will trigger a kvmagent restart.
```

### 含义

```
kvmagent服务物理内存使用硬限制
```

### Type

```
java.lang.Long
```

### Category

```
kvm
```

### 取值范围

```
[0, 9223372036854775807]
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
10737418240
```

### 默认值补充说明

```
无
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
该全局配置的值不应设置过小，比如小于2G，否则容易触发kvmagent自动重启
```
