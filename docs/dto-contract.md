# DTO/VO 与公共类契约

本文件是五人并行开发的模型基线。Day 1 后新增或修改 DTO/VO、枚举和公共类，必须同步修改本文件与 `docs/api.md`。

## 模块位置

| 模块 | 路径 | 内容 |
| --- | --- | --- |
| `sky-common` | `server/sky-common` | Result、PageResult、JWT、上下文、异常、状态枚举 |
| `sky-pojo` | `server/sky-pojo` | DTO、VO、Entity |
| `sky-server` | `server/sky-server` | Controller、Service、Mapper、配置 |

## 公共约定

- ID 使用 `Long`。
- 金额按接口文档使用分，数据库字段类型以 `sql/init.sql` 为准。
- 时间使用 `LocalDateTime`，JSON 格式为 `yyyy-MM-dd HH:mm:ss`。
- Controller 不直接返回 Entity。
- 分页统一使用 `PageResult<T>`。
- B 端 JWT 和 C 端 JWT 使用不同密钥与角色校验。
- `/admin/**` 使用管理员身份，`/user/**` 使用客户身份。

## 模块模型

### A 员工

- DTO：`EmployeeLoginDTO`、`EmployeeDTO`、`EmployeePageQueryDTO`
- VO：`EmployeeLoginVO`、`EmployeeVO`

### B 分类与菜品

- DTO：`CategoryDTO`、`CategoryPageQueryDTO`、`DishDTO`、`DishPageQueryDTO`、`DishFlavorDTO`
- VO：`CategoryVO`、`DishVO`、`DishFlavorVO`

### C 套餐与购物车

- DTO：`SetmealDTO`、`SetmealDishDTO`、`SetmealPageQueryDTO`、`ShoppingCartDTO`、`ShoppingCartPageQueryDTO`
- VO：`SetmealVO`、`SetmealDishVO`、`ShoppingCartVO`
- `ShoppingCartDTO` 中 `dishId` 和 `setmealId` 必须且只能有一个。

### D 订单与报表

- DTO：`OrderSubmitDTO`、`OrderPageQueryDTO`、`OrderStatusDTO`、`ReportQueryDTO`
- VO：`OrderVO`、`OrderDetailVO`、`OrderAdminVO`、`TurnoverVO`、`DateCountVO`、`OrderStatisticsVO`、`SalesTop10VO`
- 订单明细同时支持 `dishId` 和 `setmealId`。

### E 用户与地址

- DTO：`UserLoginDTO`、`AddressBookDTO`、`AddressBookPageQueryDTO`
- VO：`UserLoginVO`、`AddressBookVO`
- Entity：`User`、`AddressBook`

## 状态枚举

| 枚举 | 值 |
| --- | --- |
| `CategoryType` | `DISH=1`、`SETMEAL=2` |
| `DishStatus` | `DISCONTINUED=0`、`ON_SALE=1` |
| `SetmealStatus` | `DISCONTINUED=0`、`ON_SALE=1` |
| `OrderStatus` | 待接单1、已接单2、派送中3、已完成4、已取消5 |

## 变更流程

1. 修改 DTO/VO 或接口。
2. 同步修改 `docs/api.md` 和 `sql/init.sql`。
3. Apifox 和 Knife4j 样例同步更新。
4. PR 描述写明受影响的 A/B/C/D/E 模块。
5. Review 通过后合入 `dev`。
