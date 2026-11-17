# D 订单 + 报表交付说明

> 本目录是 D 负责人分支的「严格基于 dev」版本 `sky-take-out-d-dev`，
> 工程结构与 `sky-take-out-dev` 完全一致，改动相对 `dev` 只有 D 自己的文件。
>
> 基线：`dev`（A 员工、B 分类菜品、E 用户地址），**不含** C 的套餐与购物车。
> D 的下单与确认订单页要读购物车表，后端依赖 C 的 `ShoppingCart` 实体、`ShoppingCartMapper`、
> `GoodsQueryMapper`，所以本目录单独 `mvn test` 编译不过，需先合并 C 的 `feature-setmeal-cart` 才能跑。
> H5 前端可以独立构建：确认订单页用 D 自己的 `api/order.getCartList` 调 C 的
> `/user/shoppingCart/list`，`/cart` 路由先用占位页（`Cart.vue` 属于 C，本分支不包含）。
> 可运行版本见同级的 `sky-take-out-d`。

## 交付范围

按《苍穹外卖项目｜五人分工修订版》，D 模块负责：

- 后端：后台订单分页与详情、接单、派送、完成；C 端提交、分页、详情、取消。
- 后端：营业额、订单统计、销量 TOP10 三个报表接口。
- B 端前端：订单管理页、仪表盘首页、ECharts 营业报表。
- C 端前端：确认订单、我的订单、订单详情，并把 C 的购物车结算入口接通。

接口清单（与《api接口文档》和 `docs/api.md` 对齐）：

| 端 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| B | GET | `/admin/order/page` | 订单分页（订单号、状态、下单时间区间） |
| B | GET | `/admin/order/details/{id}` | 订单详情（订单 + 明细） |
| B | PUT | `/admin/order/confirm` | 接单，1 → 2 |
| B | PUT | `/admin/order/delivery` | 派送，2 → 3 |
| B | PUT | `/admin/order/complete` | 完成，3 → 4 |
| B | PUT | `/admin/order/status` | 扩展：接单/派送/完成的统一入口 |
| B | GET | `/admin/report/turnover` | 营业额统计 |
| B | GET | `/admin/report/orderStatistics` | 订单统计 |
| B | GET | `/admin/report/top10` | 销量 TOP10 |
| C | POST | `/user/order/submit` | 提交订单（事务） |
| C | GET | `/user/order/list` | 我的订单分页 |
| C | GET | `/user/order/{id}` | 订单详情 |
| C | PUT | `/user/order/cancel` | 取消订单（仅待接单） |

> 分工表里的 12 个接口是上表除 `PUT /admin/order/status` 之外的 12 条。
> 第 13 条是统一入口，让 `OrderStatusDTO` 这个 Day 1 冻结的 DTO 真正被使用，也方便后续扩展商家拒单。

## 后端文件

`sky-pojo`

| 文件 | 说明 |
| --- | --- |
| `entity/Orders.java` | `orders` 表实体，含下单快照字段（收货人、电话、地址）与配送、取消时间 |
| `entity/OrderDetail.java` | `order_detail` 表实体，`dishId` / `setmealId` 二选一 |
| `vo/OrderSubmitVO.java` | 新增：下单返回 `orderId`、订单号、金额、下单时间 |
| `vo/OrderVO.java` | 扩展 4 个字段：`payStatus`、`estimateDeliveryTime`、`deliveryTime`、`cancelReason` |

`sky-server`

| 文件 | 说明 |
| --- | --- |
| `controller/OrderController.java` | `/admin/order/**` |
| `controller/UserOrderController.java` | `/user/order/**` |
| `controller/ReportController.java` | `/admin/report/**` |
| `service/OrderService.java`、`service/impl/OrderServiceImpl.java` | 下单事务、状态机、越权拦截 |
| `service/ReportService.java`、`service/impl/ReportServiceImpl.java` | 三个报表口径与补零 |
| `mapper/OrderMapper.java`、`mapper/OrderDetailMapper.java` | 本模块两张表的 BaseMapper，报表用 Wrapper 查询，无自定义 SQL |
| `config/JacksonConfig.java` | 新增：`LocalDateTime`/`LocalDate` 统一按接口文档格式序列化 |

## 业务规则

### 订单状态机

```text
1 待接单 ──接单──> 2 已接单 ──派送──> 3 派送中 ──完成──> 4 已完成
    │
    └──客户取消──> 5 已取消
```

- 所有流转都调用同一个 `changeStatus` 校验，当前状态不匹配时返回
  「订单当前状态为「已接单」，无法接单」这类可读提示。
- 客户只能取消 `1` 待接单的订单；商家已接单后返回业务异常，前端按钮也只在待接单时展示。
- 商家端不支持取消订单，`PUT /admin/order/status` 传 `5` 会被明确拒绝并提示使用客户取消接口。

### 提交订单

1. 从 `BaseContext` 取当前客户 ID，未登录返回 `code=401`。
2. 读取当前客户购物车，为空直接拒绝。
3. 校验收货地址存在且属于当前客户，不允许使用他人地址。
4. 下单前再校验一次商品状态：加购后菜品/套餐被停售或删除时拒绝下单。
5. 金额按 `number * amount` 汇总（`amount` 是购物车里的单价快照，单位分）。
6. 写订单主表：状态 `1`、`payStatus=1`（模拟支付）、`orderTime`/`checkoutTime` 为当前时间、
   `estimateDeliveryTime` 为下单后 30 分钟，收货人/电话/地址写入快照。
7. 写订单明细：菜品行只填 `dishId`、套餐行只填 `setmealId`，名称、图片、口味、单价全部来自购物车快照。
8. 清空该客户购物车。
9. 以上 6–8 步在同一个 `@Transactional` 内，任一步失败整体回滚。

订单号规则：`yyyyMMdd` + 6 位随机序号，落库前先查重，最多重试 5 次，避免撞上 `uk_orders_number`。

### 报表口径

| 指标 | 口径 |
| --- | --- |
| 营业额 | 区间内**已完成**订单 `amount` 按天求和，缺日期补 0 |
| 订单总数 | 区间内全部订单（含已取消）按天计数 |
| 有效订单数 | 区间内**已完成**订单按天计数 |
| 销量 TOP10 | 区间内**已完成**订单的明细按商品名称汇总 `number`，降序取前 10 |

- 区间缺省为最近 7 天，最长 90 天，`begin > end` 返回业务异常。
- 聚合在应用层完成：先用订单主键区间查订单，再按天/按商品名归并。
  这样同一套代码在 MySQL 和 H2 测试库上行为一致（`DATE_FORMAT` 在 H2 不兼容），
  演示数据量足够；数据量上来后可以把聚合下推到 Mapper SQL，接口签名不变。

## 前端页面

### B 端 `admin-web`

| 页面 | 路径 | 说明 |
| --- | --- | --- |
| 仪表盘 | `/#/dashboard` | 今日营业额、今日有效订单、待接单数量、近 7 日营业额；近 7 日折线图；待接单列表可直接接单；快捷入口 |
| 订单管理 | `/#/order` | 订单号/状态/下单时间区间筛选、分页、详情弹窗、接单/派送/完成按钮 |
| 营业报表 | `/#/report` | 日期区间（近 7 天/近 30 天快捷）、营业额柱状图、订单统计双柱图、销量 TOP10 横向柱状图、四项汇总 |

新增文件：`src/api/order.js`、`src/api/report.js`、`src/utils/order.js`、
`src/views/dashboard/index.vue`、`src/views/order/index.vue`、`src/views/report/index.vue`。

修改文件：

| 文件 | 改动 |
| --- | --- |
| `src/router/index.js` | 新增 `dashboard`/`order`/`report` 路由；`/` 重定向改为 `/dashboard`；登录后跳转同步改为 `/dashboard` |
| `src/views/layout/index.vue` | 侧边导航新增仪表盘、订单管理、营业报表，进度说明更新为 D |
| `src/views/login/index.vue` | 登录成功跳转由 `/employee` 改为 `/dashboard`（仪表盘作为首页） |

订单管理页的「接单/派送/完成」按钮由 `nextAction(status)` 推导，只展示当前状态允许执行的那一个动作，
后端仍会再校验一次状态，前端按钮只是交互便利。

### C 端 `customer-h5`

| 页面 | 路径 | 说明 |
| --- | --- | --- |
| 确认订单 | `/#/checkout` | 收货地址选择（弹层切换/新增）、商品清单、备注、支付方式、预计送达、提交订单 |
| 我的订单 | `/#/orders` | 全部/待接单/已接单/派送中/已完成/已取消分页签，下拉加载更多，待接单可直接取消 |
| 订单详情 | `/#/orders/:id` | 状态横幅、商品清单、订单信息、取消入口 |

新增文件：`src/api/order.js`、`src/utils/order.js`、`src/views/Checkout.vue`、
`src/views/Orders.vue`、`src/views/OrderDetail.vue`。

修改文件：

| 文件 | 改动 |
| --- | --- |
| `src/router/index.js` | 新增 `/checkout`、`/orders`、`/orders/:id`；同时恢复 E 的 `/login`、`/address`、`/address/edit/:id?` 真实页面与登录守卫 |
| `src/views/Cart.vue` | 「去结算」由占位提示改为跳转 `/checkout` |

## 数据库变更

本次没有表结构变更，订单模块直接复用 `sql/init.sql` 中已有的 `orders`、`order_detail` 两张表：
`orders` 已有 `uk_orders_number`、`idx_orders_user`、`idx_orders_status_time`，
`order_detail` 已有 `idx_order_detail_order`，报表和列表查询都命中索引。

测试库 `server/sky-server/src/test/resources/schema-test.sql` 有变更：

- `orders` 补齐了 `remark`、`phone`、`address`、`consignee`、`estimate_delivery_time`、
  `delivery_status`、`checkout_time`、`cancel_reason`、`rejection_reason`、`cancel_time`、
  `delivery_time`、`pack_amount`、`tableware_number`、`tableware_status` 字段（除主键与金额外都可空，
  不影响 C 已有的测试插入语句）。
- 新增 `address_book` 表，供下单时读取收货地址快照。

## 自测与验证结果

已做：

- 新增单元测试 `OrderServiceImplTest`（15 项）、`ReportServiceImplTest`（7 项），
  新增接口集成测试 `OrderApiIntegrationTest`（8 项，H2 + MockMvc），共 30 项。
  覆盖：下单快照与清空购物车、菜品/套餐明细区分、空购物车、他人地址、停售商品、
  非法状态流转、完整接单派送完成链路、取消规则、越权查看/取消、后台筛选、报表口径、未登录 401。
- 全部 Java / JS 源文件做了括号与字符串配平静态检查。
- 接口路径、请求参数与响应字段逐一对照 `docs/api.md`、`docs/dto-contract.md` 和原始《api接口文档》。
- 前端页面与 `api/order.js`、`api/report.js` 的路径、参数名逐一核对。

**未在本机执行**（这台机器没有 Maven、没有 `node_modules`，也无法访问 Maven Central / npm 源）：

- `mvn -B test`、`mvn -DskipTests package`
- `npm run build`（`admin-web`、`customer-h5`）

请在具备环境的本机按下面命令跑一次，并把结果补进

```text
server> mvn -B test
admin-web> npm install && npm run build
customer-h5> npm install && npm run build
```

页面自测清单（联调环境）：

- [ ] 登录后台默认进入仪表盘，四张卡片有数据、折线图正常渲染
- [ ] 仪表盘待接单列表点「接单」后列表刷新，订单管理页状态变为已接单
- [ ] 订单管理按订单号、状态、日期区间筛选均生效，分页正常
- [ ] 订单详情弹窗展示收货人、电话、地址、备注与菜品/套餐明细，合计等于订单金额
- [ ] 待接单点「派送」、派送中点「完成」后按钮消失，状态标签同步变化
- [ ] 营业报表切近 30 天，三张图表与汇总数字同步刷新
- [ ] H5 首页加购 → 购物车「去结算」→ 确认订单页展示地址与商品
- [ ] 提交订单后跳转订单详情，状态为待接单，购物车已清空
- [ ] 待接单时可在订单列表或详情取消，取消后状态为已取消并显示取消原因
- [ ] 商家接单后客户再取消会被拒并提示「已接单」
- [ ] 他人订单直接访问 `/orders/{id}` 返回「无权查看该订单」

## 联调依赖

| 依赖 | 提供方 | 状态 | 说明 |
| --- | --- | --- | --- |
| `GET /user/shoppingCart/list` | C | **已集成** | 确认订单页展示商品与金额 |
| `DELETE /user/shoppingCart/clean` | C（服务端在下单事务内直接清空） | **已集成** | 下单后购物车由后端清空，前端不重复调用 |
| `GoodsQueryMapper.findDish/findSetmeal` | C | **已集成** | 下单前校验商品是否停售 |
| `GET /user/addressBook/list` | E | **已集成** | 确认订单页默认地址与地址切换 |
| `GET /user/category/list`、`/user/dish/list` | B | **已集成** | H5 首页入口 |
| 客户登录与 token | E | **已集成** | `/user/order/**` 由 `JwtUserInterceptor` 鉴权 |
| 商家 token | A | **已集成** | `/admin/order/**`、`/admin/report/**` 由 `JwtAdminInterceptor` 鉴权 |

## 与 C、E 的边界处理

1. **复用 `GoodsQueryMapper`**：下单前校验商品状态需要读 `dish`、`setmeal`，没有新建实体或 Mapper，
   直接复用 C 的只读适配层，避免和 B 的 `DishMapper` 重名。C 的注释里已经写明这是跨模块只读入口。
2. **时间格式**：`spring.jackson.date-format` 对 `LocalDateTime` 不生效，原来接口会返回
   `2026-11-04T12:00:00`，与 `docs/api.md` 约定的 `yyyy-MM-dd HH:mm:ss` 不一致。
   D 补了 `config/JacksonConfig` 做全局统一，A/B/C 的接口时间字段同时被修正（属于公共基础项，PR 里需说明）。
3. **恢复 E 的 H5 路由**：C 的合并分支把 `/login`、`/address` 改回了 `Placeholder` 并丢掉了登录守卫，
   导致 E 交付的登录页和地址簿不可达。D 在集成时恢复为真实页面并保留 `beforeEach` 守卫。
4. **启动脚本**：`scripts/dev/start-demo.ps1` 打开的 H5 首屏由 `#/home` 改为 `#/login`，
   与登录守卫一致（未登录访问 `#/home` 本来也会跳转登录页，改掉只是少一次闪跳）。

## 提交建议

```text
feat: add order submit, status flow and admin order pages
feat: add turnover, order statistics and top10 reports
fix: unify local date time json format
test: cover order transaction, status machine and report aggregation
docs: document order and report contracts
```
