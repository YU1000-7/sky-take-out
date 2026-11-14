# 苍穹外卖 API 接口文档

项目采用 B 端管理后台和 C 端浏览器 H5 共用一套后端。接口变更必须同步更新本文件，并通知全体成员。

## 基础约定

- 后端地址：`http://localhost:8080`
- B 端前缀：`/admin`
- C 端前缀：`/user`
- 请求头：`token: <JWT>`
- 时间格式：`yyyy-MM-dd HH:mm:ss`
- 金额单位：分

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

## 公共接口

### 公共文件上传

`POST /admin/common/upload`

请求头：`token`

请求体：`multipart/form-data`，文件字段为 `file`

响应 `data` 为可公开访问的文件地址。

## 其他模块接口

完整接口以原始《api接口文档》为准。DTO/VO 已统一预置在 `server/sky-pojo`，其他负责人应在现有模型上继续实现，不得重复创建同义类。

- B 端：套餐、订单、报表
- C 端：购物车、套餐查询、订单
- 跨模块商品明细同时支持 `dishId` 与 `setmealId`

未实现的接口不得由前端先行假定字段。需要增减字段时先在本文件和 `docs/dto-contract.md` 登记。
