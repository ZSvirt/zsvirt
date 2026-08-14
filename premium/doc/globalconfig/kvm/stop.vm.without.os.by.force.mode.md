
## Name

```
stop.vm.without.os.by.force.mode(对没有操作系统VM关闭使用force模式)
```

### Description

```
use 'force' mode when stop VM without operating system
```

### 含义

```
对没有操作系统VM关闭使用force模式
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
当这个配置设置为 true 时，关闭无操作系统的 VM 将忽略关闭 VM 的 API 的 type 字段，
强制使用 force 模式（等同于关闭电源方式）关闭 VM。

在此之前，用 grace 模式关闭无操作系统的 VM 将耗费至少 1 分钟。
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
VM 在关闭时，如果没有操作系统，在 grace 模式下没有组件应答 QEMU 发出的 shutdown 指令，
会导致关闭异常缓慢，直至超时。默认情况下，关闭 VM 的超时时间为 1 分钟。

当配置设为 true 时，KVM Agent 通过判断内存的最近更新时间来判断操作系统是否存在。
如果 VM 没有操作系统，那就不会有组件更新 VM 的内存，查询到的内存的最近更新时间
就应该为 0（表示不存在）。当 KVM Agent 发现 VM 不存在操作系统，将强制使用 force
模式关闭 VM，这样能快速关闭和返回。
```

### UI暴露

```
不需要
```

### CLI手册暴露

```
需要
```

## 注意事项

```
无
```
