# TCP 协议消息格式

> 本文用真实生成的字节（非手工编写）说明客户端与服务端之间收发消息长什么样。
> 字节由 `protobuf-java 3.25.5` 实际编码产出。

---

## 一、先明确：TCP 上跑的是二进制，不是 JSON

实际传输的是 **Protobuf 二进制**。本文出现的 JSON 只是 **Protobuf 官方 JSON 映射**下的等价表示，
目的是让你直观看出「字段是什么」，**线上并不会传 JSON**。

一帧数据的结构：

```
┌──────────────┬────────────────────────────────────────┐
│ 4 字节长度    │ Protobuf 编码的 BaseMessage              │
│ （大端 int）  │                                        │
└──────────────┴────────────────────────────────────────┘
   ↑ 只用于在字节流里切出完整一帧；TCP 没有消息边界，必须有它

拆帧由 LengthFieldBasedFrameDecoder 完成，之后交给 ProtobufDecoder 解成对象。
```

---

## 二、信封：所有消息都套这一层

```protobuf
message BaseMessage {
    optional int32 code = 1;   // 消息码
    optional bytes body = 2;   // 业务消息序列化后的字节
}
```

用 JSON 表示就是：

```json
{
  "code": 268435456,
  "body": "<业务消息的字节，Base64>"
}
```

**关键点：`code` 就是「这是什么消息」，`body` 是「消息内容」。**
业务消息的具体类型不由 `body` 携带，而是由 `code` 决定——服务端拿 `code` 查出处理方法，
用该方法第一个参数的类型去反序列化 `body`。

---

## 三、四个真实例子

### ① 网络诊断请求（客户端 → 服务端）

**业务消息定义**

```protobuf
// C_S_PING_REQUEST = 0x10000000
message PingRequest {
    optional int64 pingTime = 1; // 请求时间
}
```

**JSON 表示**

```json
// 业务消息
{ "pingTime": "1234567890123" }

// 套上信封后（实际传输的内容）
{ "code": 268435456, "body": "CMuJ7I/3Iw==" }
```

> 注意 `pingTime` 是字符串形式——这是 Protobuf JSON 映射对 `int64` 的规定
> （64 位整数超出 JavaScript 安全整数范围，用字符串避免精度丢失）。

**实际字节（逐字节注释）**

```
00 00 00 0F              ← 帧头：4 字节长度 = 15（后面 BaseMessage 的字节数）

08                       ← 信封.字段1(code)，0x08 = 字段号1 | wireType 0(varint)
80 80 80 80 01           ← 268435456 的 varint 编码（0x10000000）

12                       ← 信封.字段2(body)，0x12 = 字段号2 | wireType 2(长度前缀)
07                       ← body 长度 = 7 字节

   ── 以下 7 字节就是 PingRequest 的编码 ──
   08                    ← 字段1(pingTime)，varint
   CB 89 EC 8F F7 23     ← 1234567890123 的 varint 编码
```

**总长度 19 字节。**

---

### ② 心跳请求（客户端 → 服务端）

**业务消息定义**

```protobuf
// C_S_HEART_BEAT_REQUEST = 0x10000001
message HeartBeatRequest {
}
```

**JSON 表示**

```json
// 业务消息（空消息）
{ }

// 套上信封后
{ "code": 268435457, "body": "" }
```

**实际字节**

```
00 00 00 08              ← 帧头：长度 = 8
08 81 80 80 80 01        ← code = 268435457
12 00                    ← body，长度 0
```

**总长度 12 字节。** 心跳消息内容为空，开销几乎全在信封上。

---

### ③ 账号注册请求（客户端 → 服务端）

**业务消息定义**

```protobuf
// C_S_COMMON_REGISTER_REQUEST = 0x10000202
message CommonRegisterRequest {
    optional string username = 1; // 用户名
    optional string password = 2; // 密码(32位md5小写密文)
}
```

**JSON 表示**

```json
// 业务消息
{
  "username": "zhangsan",
  "password": "e10adc3949ba59abbe56e057f20f883e"
}

// 套上信封后
{
  "code": 268435970,
  "body": "Cgh6aGFuZ3NhbhIgZTEwYWRjMzk0OWJhNTlhYmJlNTZlMDU3ZjIwZjg4M2U="
}
```

**实际字节**

```
00 00 00 34              ← 帧头：长度 = 52

08 82 84 80 80 01        ← code = 268435970 (0x10000202)
12 2C                    ← body，长度 44

   ── body 内容 ──
   0A 08                  ← 字段1(username)，长度 8
   7A 68 61 6E 67 73 61 6E   ← "zhangsan" 的 UTF-8 字节

   12 20                  ← 字段2(password)，长度 32
   65 31 30 61 64 63 33 39 34 39 62 61 35 39 61 62 62 65 35 36 65 30 35 37 66 32 30 66 38 38 33 65
                          ← "e10adc3949ba59abbe56e057f20f883e" 的 ASCII 字节
```

**总长度 56 字节。**

---

### ④ 服务端下发提示（服务端 → 客户端）

**业务消息定义**

```protobuf
// S_C_HINT_MESSAGE_RESPONSE = 0x20000002
message HintMessageResponse {
    optional string content = 1; // 提示消息内容
    optional int32 level = 2;    // 消息级别 0:提示 1:警告 2:错误
}
```

**JSON 表示**

```json
// 业务消息
{ "content": "金币不足", "level": 2 }

// 套上信封后
{ "code": 536870914, "body": "Cgzph5HluIHkuI3otrMQAg==" }
```

**实际字节**

```
00 00 00 18              ← 帧头：长度 = 24

08 82 80 80 80 02        ← code = 536870914 (0x20000002)
12 10                    ← body，长度 16

   ── body 内容 ──
   0A 0C                  ← 字段1(content)，长度 12
   E9 87 91 E5 B8 81 E4 B8 8D E8 B6 B3
                          ← "金币不足" 的 UTF-8 字节（每个汉字 3 字节）

   10 02                  ← 字段2(level)，varint = 2
```

**总长度 28 字节。** 中文按 UTF-8 编码，一个汉字 3 字节。

---

## 四、请求与响应的对应关系

消息码有明确的编码规律：

```
0x1xxxxxxx   ← 客户端 → 服务端
0x2xxxxxxx   ← 服务端 → 客户端
```

同一个业务的请求与响应**共用低 24 位**，只有最高位不同：

| 业务 | 请求 | 响应 |
|---|---|---|
| 账号注册 | `C_S_COMMON_REGISTER_REQUEST` = `0x1`**0000202** | `S_C_COMMON_REGISTER_RESPONSE` = `0x2`**0000202** |
| 网络诊断 | `C_S_PING_REQUEST` = `0x1`**0000000** | `S_C_PING_RESPONSE` = `0x2`**0000000** |

客户端收到 `0x20000202`，就知道这是 `0x202` 那个业务的回应。

**这跟 HTTP 里「URL 对应响应」是同一个作用，只是用位运算表达的。**

---

## 五、与 Web 的 `{code, data, message}` 有什么不同

「code 和 data 已经有了」——就是信封里的 `code` 和 `body`。

差别在 **`message`（错误描述）** 与 **「响应能不能对应到请求」**：

```
C_S_LOGIN_SUCCESS_RESPONSE   = 0x20000210; // 用户登录成功返回
S_C_WX_LOGIN_REAUTH_RESPONSE = 0x20000206; // 通知前端重新授权
```

**这两条都没有对应的 `C_S_..._REQUEST`** —— 它们是**推送**，服务端主动发的。

Web 的 `{code, data, message}` 隐含一个前提：每个响应都能对应到一个请求。而游戏长连接里：

| 场景 | Web | 游戏 |
|---|---|---|
| 服务端主动推送（别人进房间了、轮子出结果了） | 无此模型 | 常态 |
| 一个请求引发多条响应（进房间 → 房间信息 + 玩家列表 + 我的数据） | 一请求一响应 | 常态 |
| 没有请求却有响应 | 不成立 | 常态 |

**没有请求，就无所谓「这个响应对应哪个请求的 message」。**

失败的情况走 `S_C_HINT_MESSAGE_RESPONSE`（例 ④），客户端收到后弹提示框——
不为每个接口定义单独的 error 结构。

---

## 六、相关的代码位置

| 环节 | 代码 |
|---|---|
| 拆帧 + 解码 | `TcpChannelInitializer`（LengthFieldBasedFrameDecoder + ProtobufDecoder） |
| 收消息 | `TcpServerHandler.channelRead` |
| 按消息码分发 | 路由表由 `AppHandlerRegister` 扫描 `@AppHandler` 装配 |
| 发消息 | `ClientSender.sendMessage(...)` → `TcpNetwork.sendMessageToClient(...)` |
| 信封定义 | `src/main/proto/AppMessage.proto` 的 `BaseMessage` |
