# C 套餐 + 购物车交付说明

## 交付范围

按《苍穹外卖项目｜五人分工修订版》，C 模块负责：

- 后端：套餐新增、修改、删除、分页、详情、起停售，C 端套餐列表和套餐内菜品查询。
- 后端：购物车添加、减少、列表、清空，同一购物车同时支持菜品与套餐。
- B 端：套餐管理页（列表、表单、选择菜品弹窗、图片上传、规格与状态操作）。
- C 端 H5：首页容器与分类导航、套餐展示组件、购物车页面。

依赖关系：套餐选菜复用 B 的菜品查询接口；购物车字段与 D 的订单提交契约对齐；分类下拉与 H5 分类导航读取 B 的分类接口。

## 后端文件

`sky-pojo`

| 文件 | 说明 |
| --- | --- |
| `entity/Setmeal.java`、`entity/SetmealDish.java`、`entity/ShoppingCart.java` | 三张业务表实体 |
| `dto/SetmealDTO.java`、`dto/SetmealDishDTO.java`、`dto/SetmealPageQueryDTO.java` | 套餐入参，含 Bean Validation |
| `dto/ShoppingCartDTO.java`、`dto/ShoppingCartPageQueryDTO.java` | 购物车入参 |
| `vo/SetmealVO.java`、`vo/SetmealDishVO.java`、`vo/SetmealDishDetailVO.java`、`vo/ShoppingCartVO.java` | 接口出参 |
| `vo/GoodsSnapshotVO.java`、`vo/CategoryNameVO.java` | 内部只读投影，不作为接口出参 |
| `enums/SetmealStatus.java` | 套餐状态枚举，避免业务代码散落魔法数字 |

`sky-server`

| 文件 | 说明 |
| --- | --- |
| `controller/SetmealController.java` | `/admin/setmeal/**` |
| `controller/UserSetmealController.java` | `/user/setmeal/**` |
| `controller/ShoppingCartController.java` | `/user/shoppingCart/**` |
| `service/SetmealService.java`、`service/impl/SetmealServiceImpl.java` | 套餐业务规则与事务 |
| `service/ShoppingCartService.java`、`service/impl/ShoppingCartServiceImpl.java` | 购物车业务规则与事务 |
| `mapper/SetmealMapper.java`、`mapper/SetmealDishMapper.java`、`mapper/ShoppingCartMapper.java` | 本模块表的数据访问 |
| `mapper/GoodsQueryMapper.java` | 跨模块只读查询适配层（见下文） |

### 关于 GoodsQueryMapper

套餐与购物车需要读取 `dish`、`category`、`order_detail`、`orders` 等由 B / D 维护的表。为了不依赖对方尚未提交的实体和 Mapper（同名类会冲突），C 模块把这些只读 SQL 集中在 `GoodsQueryMapper`。

待 B 的 `DishMapper`、`CategoryMapper` 稳定后，可把 `GoodsQueryMapper` 内部实现替换为对 B 服务的调用，方法签名保持不变，C 的业务代码无需改动。

## 业务规则

### 套餐

1. 分类校验：`categoryId` 必须存在且 `category.type = 2`，否则拒绝保存。
2. 菜品校验：`setmealDishes` 至少 1 个、`dishId` 不可重复、菜品必须存在于 `dish` 表。
3. 快照：写入 `setmeal_dish` 时同时保存菜品名称和价格快照。
4. 事务：套餐主表与 `setmeal_dish` 在同一事务内写入；修改为整体替换（先删后插），失败整体回滚，不会留下半更新数据。
5. 起售校验：起售前要求套餐内有菜品且所含菜品全部起售，否则给出具体停售菜品名称。
6. 删除规则：
   - 起售中不可删除，先停售；
   - 被购物车引用不可删除，建议停售；
   - 被有效订单（状态 1/2/3）引用不可删除，建议停售。
7. C 端只返回起售套餐；`/user/setmeal/list` 的 `categoryId` 缺省时返回全部起售套餐，便于分类接口未就绪时降级。

### 购物车

1. `dishId` 与 `setmealId` 必须二选一，同时传或都不传都会返回业务异常。
2. 合并口径：同一客户 + 同一菜品 + 同一口味合并为一条；同一套餐合并为一条。空口味统一存为 `null`，避免 `null` 与空串拆成两条。
3. 加购写入 `name`、`image`、`amount` 快照，商品后续改价不影响购物车已有记录。
4. 加购前校验商品处于起售状态，停售商品不允许加入购物车。
5. 单个商品数量上限 99；减少到 0 时移除记录。
6. 金额口径：`amount` 为单价（分），订单金额按 `number * amount` 计算，与 D 的订单提交契约一致。

## 前端页面

### B 端 `admin-web`

| 页面 | 路径 | 说明 |
| --- | --- | --- |
| 套餐管理 | `/#/setmeal` | 名称/分类/状态筛选、分页、新增、编辑、起停售、单条与批量删除 |
| 套餐表单 | 弹窗 | 分类下拉、价格（元输入按分提交）、图片上传、描述、状态、套餐菜品份数调整 |
| 选择菜品 | 弹窗 | 复用 `GET /admin/dish/page`，多选回填；接口不可用时支持手动登记菜品 ID |

新增文件：`src/api/setmeal.js`、`src/views/setmeal/index.vue`；修改 `src/router/index.js`、`src/views/layout/index.vue`（侧边导航新增“套餐管理”）。

图片上传统一走 `POST /admin/common/upload`，使用 `token` 请求头，与 A、B 的图片上传保持一致。

### C 端 `customer-h5`

| 页面 | 路径 | 说明 |
| --- | --- | --- |
| 首页 | `/#/home` | 分类导航（含套餐分类）、菜品区、套餐区、购物车悬浮条 |
| 购物车 | `/#/cart` | 商品列表、加减、清空、合计、结算入口 |

新增文件：`src/api/setmeal.js`、`src/api/cart.js`、`src/views/Cart.vue`、`src/components/SetmealList.vue`。

**与 B 的集成方式（B 交付后已改为组件化集成，不再使用软接入点）：**

B 已把 H5 首页做成容器（`Home.vue` + `components/CategoryNav.vue` + `components/DishList.vue`），C 沿用这套结构，改动只有三处：

| 文件 | 改动 |
| --- | --- |
| `components/CategoryNav.vue` | 同时查询 `type=1`、`type=2` 两类分类并合并展示，套餐分类加「套餐」角标；`change` 事件追加第二个参数 `category`（含 `type`），只读第一个参数的旧调用方不受影响 |
| `components/SetmealList.vue` | 新增，套餐卡片列表 + 套餐菜品弹层，props `categoryId`，事件 `add-cart` 抛 `{ setmealId, setmealName }`，与 `DishList` 的 `{ dishId, dishFlavor }` 由首页统一收敛 |
| `views/Home.vue` | 按当前分类的 `type` 切换渲染 `DishList` 或 `SetmealList`；`handleAddCart` 由占位提示改为真实调用加购接口；新增购物车悬浮条 |

这样 `DishList` 与 `SetmealList` 的加购事件格式统一，后续若新增其他商品类型，只需在 `Home.vue` 加一个分支。

## 数据库变更

`sql/init.sql` 中为 C 模块的表补充了索引与唯一键（仅对新建库生效）：

```sql
ALTER TABLE setmeal ADD KEY idx_setmeal_status (status);
ALTER TABLE setmeal_dish ADD UNIQUE KEY uk_setmeal_dish (setmeal_id, dish_id);
ALTER TABLE shopping_cart ADD KEY idx_cart_user_goods (user_id, dish_id, setmeal_id);
```

已有数据库需要手动执行上述语句。`setmeal_dish` 的唯一键要求先清理重复关联：

```sql
DELETE sd1 FROM setmeal_dish sd1
JOIN setmeal_dish sd2
  ON sd1.setmeal_id = sd2.setmeal_id
 AND sd1.dish_id = sd2.dish_id
 AND sd1.id > sd2.id;
```

## 自测与验证结果

后端：

- `mvn test` 共 32 项通过（原 7 项 + 本次新增 25 项）。
- 新增单元测试：`SetmealServiceImplTest`（9 项）、`ShoppingCartServiceImplTest`（7 项）。
- 新增接口集成测试：`SetmealApiIntegrationTest`（7 项）、`ShoppingCartApiIntegrationTest`（4 项），使用 H2 + MockMvc，覆盖登录鉴权、事务回滚、删除规则、数量合并与减少归零。
- `mvn -DskipTests package` 可产出可执行 JAR。

前端：

- `admin-web` 执行 `npm run build` 成功。
- `customer-h5` 执行 `npm run build` 成功。

页面自测清单（联调环境）：

- [ ] B 端新增套餐后列表可查，编辑回显菜品与份数
- [ ] 修改套餐整体替换菜品，旧菜品不残留
- [ ] 停售套餐可删除，起售套餐删除被拒并提示原因
- [ ] 起售含停售菜品的套餐被拒并提示具体菜品
- [ ] H5 分类导航切换后套餐区跟随变化
- [ ] 套餐加入购物车后悬浮条数量与金额正确
- [ ] 购物车加减数量、减到 0 移除、清空正常
- [ ] 同一菜品不同口味在购物车中分成两条

## 联调依赖

| 依赖 | 提供方 | 状态 | 说明 |
| --- | --- | --- | --- |
| `GET /admin/category/list?type=2` | B | **已集成** | 套餐表单分类下拉直接调用 B 的 `api/category.js` |
| `GET /admin/dish/page` | B | **已集成** | 选择菜品弹窗直接调用 B 的 `api/dish.js` |
| `POST /admin/common/upload` | B/A | **已集成** | 图片上传复用 B 的 `uploadDishImage`，统一走 request 拦截器带 token |
| `GET /user/category/list?type=1\|2` | B | **已集成** | 首页分类导航合并展示菜品与套餐分类 |
| `GET /user/dish/list` | B | **已集成** | 菜品区沿用 B 的 `DishList` 组件 |
| 客户登录与 token | E | 待交付 | 浏览类接口 B 已在拦截器中放行；加购与购物车需要客户 token |
| 订单提交与结算页 | D | 待交付 | 购物车“去结算”进入购物车页，提交订单由 D 接入 |

B 的接口已全部就位并通过联调验证，C 不再保留任何降级分支。剩余两个外部依赖由 D、E 补齐后可做端到端联调。

## 提交建议

```text
feat: add setmeal management and shopping cart
test: cover setmeal transaction and cart merge rules
docs: document setmeal and cart contracts
```
