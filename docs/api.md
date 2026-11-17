# 苍穹外卖 API 接口文档

项目采用 B 端管理后台和 C 端浏览器 H5 共用一套后端。接口变更必须同步更新本文件，并通知全体成员。

## 基础约定

- 后端地址：`http://localhost:8080`
- B 端前缀：`/admin`
- C 端前缀：`/user`
- 请求头：`token: <JWT>`
- 时间格式：`yyyy-MM-dd HH:mm:ss`；纯日期字段为 `yyyy-MM-dd`
- 金额单位：分

`LocalDateTime`、`LocalDate` 的 JSON 格式由 `com.sky.config.JacksonConfig` 统一注册，
前后端都按接口文档的格式收发，不需要各自再做时间格式化。

`LocalDateTime`、`LocalDate` 的 JSON 格式由 `com.sky.config.JacksonConfig` 统一注册，
前后端都按接口文档的格式收发，不需要各自再做时间格式化。

统一返回：

```json
{
  "code": 1,
  "msg": "成功",
  "data": {}
}
```

| code | 含义 |
| --- | --- |
| 1 | 成功 |
| 0 | 业务异常 |
| 401 | 未登录或 Token 失效 |

## B 端员工模块

### 1. 员工登录

`POST /admin/employee/login`

请求体：

```json
{
  "username": "admin",
  "password": "123456"
}
```

响应 `data`：

```json
{
  "id": 1,
  "username": "admin",
  "name": "管理员",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### 2. 员工登出

`POST /admin/employee/logout`

请求头：`token`

### 3. 员工分页查询

`GET /admin/employee/page`

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| page | 否 | 页码，默认 1 |
| pageSize | 否 | 每页条数，默认 10 |
| name | 否 | 姓名模糊查询 |

### 4. 新增员工

`POST /admin/employee`

```json
{
  "username": "zhangsan",
  "name": "张三",
  "phone": "13800138001",
  "sex": "男",
  "idNumber": "110101199001011234"
}
```

新员工默认密码为 `123456`。

### 5. 修改员工

`PUT /admin/employee`

请求体在新增字段基础上携带 `id`。

### 6. 根据 ID 查询员工

`GET /admin/employee/{id}`

### 7. 启用或禁用员工

`PUT /admin/employee/status/{status}?id={id}`

`status`：`1` 启用，`0` 禁用。

## B 端分类模块

### 1. 新增分类

`POST /admin/category`

请求体：

```json
{
  "type": 1,
  "name": "热菜",
  "sort": 1
}
```

`type`：`1` 菜品分类，`2` 套餐分类。同一类型下分类名不能重复。

### 2. 修改分类

`PUT /admin/category`

请求体在新增字段基础上携带 `id`。分类类型不允许修改。

### 3. 删除分类

`DELETE /admin/category?id={id}`

分类下还有菜品或套餐时不允许删除。

### 4. 分类分页查询

`GET /admin/category/page?page=1&pageSize=10&name=&type=`

响应 `data` 为分页对象，`records` 元素字段：`id`、`type`、`name`、`sort`、`status`、`updateTime`。

### 5. 按类型查询分类

`GET /admin/category/list?type=1`

响应 `data` 为分类数组，按 `sort` 升序。分类管理页的下拉框和 H5 导航使用。

## B 端菜品模块

### 1. 新增菜品

`POST /admin/dish`

请求体：

```json
{
  "name": "鱼香肉丝",
  "categoryId": 3,
  "price": 3800,
  "image": "http://localhost:9000/sky-take-out/dish/xxx.jpg",
  "description": "经典川菜",
  "status": 1,
  "flavors": [
    { "name": "辣度", "value": "微辣,中辣,特辣" }
  ]
}
```

`price` 单位分。`image` 取公共上传接口返回的地址。

### 2. 修改菜品

`PUT /admin/dish`

请求体在新增字段基础上携带 `id`。`flavors` 为整体覆盖，传什么就是什么，不传等于清空。
修改 `status` 时与专用起售/停售接口执行相同校验；被起售中套餐关联的菜品不能停售。

### 3. 批量删除菜品

`DELETE /admin/dish?ids=1,2,3`

`ids` 逗号分隔。起售中的菜品、已被套餐关联的菜品不允许删除。

### 4. 菜品分页查询

`GET /admin/dish/page?page=1&pageSize=10&name=&categoryId=&status=`

响应 `data` 为分页对象，`records` 元素为菜品视图对象，字段：`id`、`name`、`categoryId`、`categoryName`、`price`、`image`、`description`、`status`、`updateTime`。

### 5. 根据 ID 查询菜品

`GET /admin/dish/{id}`

响应 `data` 比列表多一个 `flavors` 数组：

```json
{
  "flavors": [
    { "name": "辣度", "value": "微辣,中辣,特辣" }
  ]
}
```

### 6. 菜品起售停售

`PUT /admin/dish/status/{status}?id={id}`

`status`：`1` 起售，`0` 停售。若该菜品被起售中的套餐关联，不允许停售。


## B 端订单模块

订单状态：`1` 待接单、`2` 已接单、`3` 派送中、`4` 已完成、`5` 已取消。
合法流转只有 `1→2`、`2→3`、`3→4`（商家）和 `1→5`（客户取消），其余流转一律返回 `code=0` 并提示当前状态。

### 1. 后台订单分页查询

`GET /admin/order/page`

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| page | 否 | 默认 1 |
| pageSize | 否 | 默认 10，最大 100 |
| number | 否 | 订单号模糊查询 |
| status | 否 | 订单状态 |
| beginTime | 否 | 下单时间起，`yyyy-MM-dd HH:mm:ss` |
| endTime | 否 | 下单时间止，`yyyy-MM-dd HH:mm:ss` |

响应 `data.records` 每项为订单视图对象：

```json
{
  "id": 1,
  "number": "20261104123456",
  "status": 1,
  "userId": 1,
  "addressBookId": 1,
  "amount": 14400,
  "remark": "不要香菜",
  "consignee": "张三",
  "phone": "13800138000",
  "address": "江苏省苏州市姑苏区观前街1号",
  "payMethod": 1,
  "payStatus": 1,
  "orderTime": "2026-11-04 12:00:00",
  "checkoutTime": "2026-11-04 12:00:00",
  "estimateDeliveryTime": "2026-11-04 12:30:00",
  "deliveryTime": null,
  "cancelReason": null
}
```

订单列表不返回明细，需要明细时调用详情接口。

### 2. 查看订单详情

`GET /admin/order/details/{id}`

响应 `data`：

```json
{
  "order": { "id": 1, "number": "20261104123456", "status": 2, "amount": 14400 },
  "orderDetailList": [
    { "name": "鱼香肉丝", "dishId": 1, "setmealId": null, "dishFlavor": "微辣", "number": 2, "amount": 3800 },
    { "name": "双人特惠餐", "dishId": null, "setmealId": 1, "dishFlavor": null, "number": 1, "amount": 6800 }
  ]
}
```

明细同时兼容菜品与套餐：菜品行 `dishId` 有值、套餐行 `setmealId` 有值。名称、图片、单价都是下单时的快照。

### 3. 商家接单

`PUT /admin/order/confirm?id={id}`

仅 `1` 待接单可接单，响应 `{"code":1,"msg":"接单成功","data":null}`。

### 4. 商家派送

`PUT /admin/order/delivery?id={id}`

仅 `2` 已接单可派送，同时写入派送时间，响应 `{"code":1,"msg":"派送成功","data":null}`。

### 5. 商家完成订单

`PUT /admin/order/complete?id={id}`

仅 `3` 派送中可完成，响应 `{"code":1,"msg":"订单完成","data":null}`。

### 6. 统一状态流转（扩展接口）

`PUT /admin/order/status`

请求体：

```json
{ "id": 1, "status": 2 }
```

`status` 仅支持 `2`、`3`、`4`，分别等价于接单、派送、完成；流转到 `1`/`5` 会返回业务异常。
这个接口是接单、派送、完成三个语义化接口的统一入口，后端复用同一套状态机校验。

## B 端报表统计模块

统计口径：

- 营业额、销量 TOP10 只统计已完成订单（状态 `4`）。
- 订单统计的「订单总数」统计区间内全部订单（含已取消），「有效订单数」统计已完成订单。
- 区间内没有订单的日期补 `0`，图表不会断点。
- `begin`、`end` 可缺省，缺省为最近 7 天；区间最长 90 天，开始日期晚于结束日期会返回业务异常。

### 1. 营业额统计

`GET /admin/report/turnover?begin=2026-11-01&end=2026-11-07`

响应 `data`：

```json
[
  { "date": "2026-11-01", "turnover": 12800 },
  { "date": "2026-11-02", "turnover": 0 }
]
```

`turnover` 单位为分。

### 2. 订单统计

`GET /admin/report/orderStatistics?begin=&end=`

响应 `data`：

```json
{
  "orderCountList": [{ "date": "2026-11-04", "orderCount": 12, "validOrderCount": null }],
  "validOrderCountList": [{ "date": "2026-11-04", "orderCount": null, "validOrderCount": 10 }]
}
```

两个列表按日期升序一一对应：`orderCountList` 只填 `orderCount`，`validOrderCountList` 只填 `validOrderCount`。

### 3. 销量 TOP10

`GET /admin/report/top10?begin=&end=`

响应 `data`：

```json
[{ "name": "鱼香肉丝", "sales": 32 }]
```

按商品名称汇总已完成订单的销量，降序取前 10 条。菜品与套餐同名时合并为一条。


## B 端订单模块

订单状态：`1` 待接单、`2` 已接单、`3` 派送中、`4` 已完成、`5` 已取消。
合法流转只有 `1→2`、`2→3`、`3→4`（商家）和 `1→5`（客户取消），其余流转一律返回 `code=0` 并提示当前状态。

### 1. 后台订单分页查询

`GET /admin/order/page`

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| page | 否 | 默认 1 |
| pageSize | 否 | 默认 10，最大 100 |
| number | 否 | 订单号模糊查询 |
| status | 否 | 订单状态 |
| beginTime | 否 | 下单时间起，`yyyy-MM-dd HH:mm:ss` |
| endTime | 否 | 下单时间止，`yyyy-MM-dd HH:mm:ss` |

响应 `data.records` 每项为订单视图对象：

```json
{
  "id": 1,
  "number": "20261104123456",
  "status": 1,
  "userId": 1,
  "addressBookId": 1,
  "amount": 14400,
  "remark": "不要香菜",
  "consignee": "张三",
  "phone": "13800138000",
  "address": "江苏省苏州市姑苏区观前街1号",
  "payMethod": 1,
  "payStatus": 1,
  "orderTime": "2026-11-04 12:00:00",
  "checkoutTime": "2026-11-04 12:00:00",
  "estimateDeliveryTime": "2026-11-04 12:30:00",
  "deliveryTime": null,
  "cancelReason": null
}
```

订单列表不返回明细，需要明细时调用详情接口。

### 2. 查看订单详情

`GET /admin/order/details/{id}`

响应 `data`：

```json
{
  "order": { "id": 1, "number": "20261104123456", "status": 2, "amount": 14400 },
  "orderDetailList": [
    { "name": "鱼香肉丝", "dishId": 1, "setmealId": null, "dishFlavor": "微辣", "number": 2, "amount": 3800 },
    { "name": "双人特惠餐", "dishId": null, "setmealId": 1, "dishFlavor": null, "number": 1, "amount": 6800 }
  ]
}
```

明细同时兼容菜品与套餐：菜品行 `dishId` 有值、套餐行 `setmealId` 有值。名称、图片、单价都是下单时的快照。

### 3. 商家接单

`PUT /admin/order/confirm?id={id}`

仅 `1` 待接单可接单，响应 `{"code":1,"msg":"接单成功","data":null}`。

### 4. 商家派送

`PUT /admin/order/delivery?id={id}`

仅 `2` 已接单可派送，同时写入派送时间，响应 `{"code":1,"msg":"派送成功","data":null}`。

### 5. 商家完成订单

`PUT /admin/order/complete?id={id}`

仅 `3` 派送中可完成，响应 `{"code":1,"msg":"订单完成","data":null}`。

### 6. 统一状态流转（扩展接口）

`PUT /admin/order/status`

请求体：

```json
{ "id": 1, "status": 2 }
```

`status` 仅支持 `2`、`3`、`4`，分别等价于接单、派送、完成；流转到 `1`/`5` 会返回业务异常。
这个接口是接单、派送、完成三个语义化接口的统一入口，后端复用同一套状态机校验。

## B 端报表统计模块

统计口径：

- 营业额、销量 TOP10 只统计已完成订单（状态 `4`）。
- 订单统计的「订单总数」统计区间内全部订单（含已取消），「有效订单数」统计已完成订单。
- 区间内没有订单的日期补 `0`，图表不会断点。
- `begin`、`end` 可缺省，缺省为最近 7 天；区间最长 90 天，开始日期晚于结束日期会返回业务异常。

### 1. 营业额统计

`GET /admin/report/turnover?begin=2026-11-01&end=2026-11-07`

响应 `data`：

```json
[
  { "date": "2026-11-01", "turnover": 12800 },
  { "date": "2026-11-02", "turnover": 0 }
]
```

`turnover` 单位为分。

### 2. 订单统计

`GET /admin/report/orderStatistics?begin=&end=`

响应 `data`：

```json
{
  "orderCountList": [{ "date": "2026-11-04", "orderCount": 12, "validOrderCount": null }],
  "validOrderCountList": [{ "date": "2026-11-04", "orderCount": null, "validOrderCount": 10 }]
}
```

两个列表按日期升序一一对应：`orderCountList` 只填 `orderCount`，`validOrderCountList` 只填 `validOrderCount`。

### 3. 销量 TOP10

`GET /admin/report/top10?begin=&end=`

响应 `data`：

```json
[{ "name": "鱼香肉丝", "sales": 32 }]
```

按商品名称汇总已完成订单的销量，降序取前 10 条。菜品与套餐同名时合并为一条。

## C 端分类与菜品浏览

这两个接口免登录，客户端未登录也能浏览菜单。

### 1. 查询分类列表

`GET /user/category/list?type=1`

`type` 默认 `1`，只返回启用的分类，按 `sort` 升序。H5 首页左侧导航使用；套餐分类传 `type=2`。

### 2. 根据分类查询菜品

`GET /user/dish/list?categoryId={id}`

只返回起售菜品，元素含 `flavors`，供 H5 规格选择弹窗使用。
## C 端用户与地址簿

C 端接口统一使用客户 JWT，请求头为 `token`。登录接口除外。

### 客户手机号登录

`POST /user/user/login`

请求体：

```json
{
  "phone": "13800138000"
}
```

响应 `data`：

```json
{
  "id": 1,
  "phone": "13800138000",
  "name": "用户8000",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

手机号不存在时会自动创建客户记录。

### 新增地址

`POST /user/addressBook`

```json
{
  "consignee": "张三",
  "sex": "男",
  "phone": "13800138000",
  "provinceCode": "110000",
  "provinceName": "北京市",
  "cityCode": "110100",
  "cityName": "北京市",
  "districtCode": "110101",
  "districtName": "东城区",
  "detail": "示例小区 1 号楼 101",
  "label": "家",
  "isDefault": 1
}
```

首个地址自动设为默认地址。

### 修改地址

`PUT /user/addressBook`

请求体在新增字段基础上携带 `id`。

### 删除地址

`DELETE /user/addressBook?id={id}`

### 查询当前用户地址列表

`GET /user/addressBook/list`

### 根据 ID 查询地址

`GET /user/addressBook/{id}`

### 设置默认地址

`PUT /user/addressBook/default?id={id}`

所有地址操作均校验 `userId`，禁止跨客户读取、修改或删除。


## C 端订单模块

下单在同一个事务内完成：校验购物车与收货地址 → 写入订单主表 → 写入订单明细 → 清空购物车。
购物车为空、地址不属于当前客户、商品已停售都会返回业务异常并整体回滚。
订单金额按购物车 `number * amount` 汇总，单位分；收货人、电话、地址写入订单快照，后续客户改地址或商品改价不影响历史订单。

### 1. 提交订单

`POST /user/order/submit`

```json
{
  "addressBookId": 1,
  "payMethod": 1,
  "remark": "不要香菜"
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| addressBookId | 是 | 必须是当前客户自己的收货地址 |
| payMethod | 是 | `1` 微信支付（模拟） |
| remark | 否 | 最长 255 字符 |

响应 `data`：

```json
{
  "orderId": 1,
  "number": "20261104123456",
  "amount": 14400,
  "orderTime": "2026-11-04 12:00:00"
}
```

下单后订单状态为 `1` 待接单，`payStatus` 直接置为 `1`（模拟支付成功），`estimateDeliveryTime` 为下单后 30 分钟。

### 2. 用户订单分页

`GET /user/order/list?page=1&pageSize=10&status=`

只返回当前客户的订单，按 `orderTime` 倒序。`status` 可选，元素字段与后台订单分页一致。

### 3. 查询订单详情

`GET /user/order/{id}`

响应结构与后台详情一致（`order` + `orderDetailList`），非本人订单返回 `code=0`「无权查看该订单」。

### 4. 用户取消订单

`PUT /user/order/cancel?id={id}`

仅 `1` 待接单可取消，写入 `cancelReason`（固定为「用户取消订单」）与 `cancelTime`。
已接单、派送中、已完成的订单取消会返回业务异常。

响应：`{"code":1,"msg":"取消订单成功","data":null}`


## C 端订单模块

下单在同一个事务内完成：校验购物车与收货地址 → 写入订单主表 → 写入订单明细 → 清空购物车。
购物车为空、地址不属于当前客户、商品已停售都会返回业务异常并整体回滚。
订单金额按购物车 `number * amount` 汇总，单位分；收货人、电话、地址写入订单快照，后续客户改地址或商品改价不影响历史订单。

### 1. 提交订单

`POST /user/order/submit`

```json
{
  "addressBookId": 1,
  "payMethod": 1,
  "remark": "不要香菜"
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| addressBookId | 是 | 必须是当前客户自己的收货地址 |
| payMethod | 是 | `1` 微信支付（模拟） |
| remark | 否 | 最长 255 字符 |

响应 `data`：

```json
{
  "orderId": 1,
  "number": "20261104123456",
  "amount": 14400,
  "orderTime": "2026-11-04 12:00:00"
}
```

下单后订单状态为 `1` 待接单，`payStatus` 直接置为 `1`（模拟支付成功），`estimateDeliveryTime` 为下单后 30 分钟。

### 2. 用户订单分页

`GET /user/order/list?page=1&pageSize=10&status=`

只返回当前客户的订单，按 `orderTime` 倒序。`status` 可选，元素字段与后台订单分页一致。

### 3. 查询订单详情

`GET /user/order/{id}`

响应结构与后台详情一致（`order` + `orderDetailList`），非本人订单返回 `code=0`「无权查看该订单」。

### 4. 用户取消订单

`PUT /user/order/cancel?id={id}`

仅 `1` 待接单可取消，写入 `cancelReason`（固定为「用户取消订单」）与 `cancelTime`。
已接单、派送中、已完成的订单取消会返回业务异常。

响应：`{"code":1,"msg":"取消订单成功","data":null}`

## 公共接口

### 公共文件上传

`POST /admin/common/upload`

请求头：`token`

请求体：`multipart/form-data`，文件字段为 `file`

响应 `data` 为可公开访问的文件地址。

## 其他模块接口

完整接口以原始《api接口文档》为准。当前已实现的模块：

- B 端：员工、分类、菜品、套餐、订单、报表、公共上传
- C 端：用户登录、地址簿、分类与菜品浏览、套餐查询、购物车、订单

尚未实现的只有「再来一单」等增强功能，接入前需要先在本文件登记接口与字段。
未实现的接口不得由前端先行假定字段。需要增减字段时先在本文件登记。
