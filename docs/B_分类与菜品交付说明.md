# B 分类与菜品模块交付说明

## 交付范围

B 模块负责分类与菜品：B 端 11 个管理接口、C 端 2 个浏览接口，B 端「分类管理」「菜品管理」两个页面，以及 H5 首页的分类导航与菜品列表组件。

按 A 已定的工程结构继续开发，接口契约已登记到 `docs/api.md` 的「B 端分类模块」「B 端菜品模块」「C 端分类与菜品浏览」三节。

## 后端文件

- `entity/Category`、`entity/Dish`、`entity/DishFlavor`：三张表的实体，时间与操作人靠 `MyMetaObjectHandler` 自动填充
- `dto/CategoryDTO`、`dto/DishDTO`：新增和修改的入参，带 `javax.validation` 校验
- `dto/CategoryPageQueryDTO`、`dto/DishPageQueryDTO`：分页查询入参
- `vo/DishVO`：菜品列表和详情返回，比实体多 `categoryName` 与 `flavors`
- `mapper/CategoryMapper`、`mapper/DishMapper`、`mapper/DishFlavorMapper`：继承 `BaseMapper`，跨模块的引用校验用 `@Select` 直接查 `setmeal`、`setmeal_dish`，不引 C 的 Mapper
- `service/impl/CategoryServiceImpl`：分类增删改查，删除前校验分类下有无菜品和套餐
- `service/impl/DishServiceImpl`：菜品增删改查、起售停售、C 端列表；口味「先删后插」；删除和停售前校验起售状态与套餐引用
- `controller/CategoryController`、`controller/DishController`：`/admin/category`、`/admin/dish`
- `controller/UserCategoryController`、`controller/UserDishController`：`/user/category`、`/user/dish`

## 前端页面

管理后台：

- `/category`：分类分页、按名称和类型筛选、新增、修改、删除
- `/dish`：菜品分页、按名称/分类/状态筛选、新增、修改、批量删除、起售停售、图片上传、口味动态行

客户 H5：

- `components/CategoryNav.vue`：左侧分类导航
- `components/DishList.vue`：菜品卡片 + 口味规格弹窗，选中后 `$emit('add-cart', { dishId, dishFlavor })`

## 数据库

三张表 `category`、`dish`、`dish_flavor` 在 `sql/init.sql` 中已存在，本模块**未改动表结构**。

`sql/b_seed.sql` 是演示数据（5 个分类、7 道菜、5 条口味），只在需要看效果时执行。`price` 存的是分。

## 改动的已有文件

Vue 的路由入口和 Java 的拦截器配置都只有一处，没法靠新增文件生效，所以下面几个文件需要在原文件里加行（不是重写）。改动量合计不到 40 行。

- `admin-web/src/router/index.js`：`children` 增加 `/category`、`/dish`
- `admin-web/src/views/layout/index.vue`：侧边栏增加两个入口，并把写死的 `nav-item active` 改为按 `$route.path` 判断
- `server/sky-server/.../config/WebMvcConfig.java`：C 端拦截器放行浏览类接口
- `docs/api.md`：登记本模块 13 个接口

### 1. router/index.js

`children` 里 `/employee` 那一项后面补逗号，再插入两条：

```js
      {
        path: 'category',
        name: 'Category',
        component: () => import('../views/category/index.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'dish',
        name: 'Dish',
        component: () => import('../views/dish/index.vue'),
        meta: { title: '菜品管理' }
      },
```

### 2. layout/index.vue

把写死高亮的那句换成按路由判断，否则三个菜单会同时高亮：

```vue
<router-link :class="['nav-item', { active: $route.path === '/employee' }]" to="/employee">
```

在它对应的 `</router-link>` 之后补两个入口：

```vue
<router-link :class="['nav-item', { active: $route.path === '/category' }]" to="/category">
  <i class="el-icon-menu" />
  <span>分类管理</span>
</router-link>
<router-link :class="['nav-item', { active: $route.path === '/dish' }]" to="/dish">
  <i class="el-icon-dish" />
  <span>菜品管理</span>
</router-link>
```

### 3. WebMvcConfig.java

把 `.excludePathPatterns("/user/user/login")` 换成：

```java
.excludePathPatterns(
        "/user/user/login",
        "/user/category/list",
        "/user/dish/list",
        "/user/setmeal/list",
        "/user/setmeal/dish/**"
);
```

这一条属于公共基座，按分工该由 E 改。不改的话客户未登录打开 H5 会被 401 踢去登录页。

## 自测清单

- Knife4j 出现「B端分类接口」「B端菜品接口」「C端分类接口」「C端菜品接口」四组
- 新增同名同类型分类返回「同类型下分类名称已存在」，同名不同类型可以共存
- 菜品带两组口味新增后，详情 `flavors` 长度为 2
- 编辑菜品删一组加一组后，口味数量仍为 2（先删后插生效）
- 起售中的菜品删除被拒绝
- 被起售套餐关联的菜品停售被拒绝
- 分类下还有菜品时删除分类被拒绝
- 菜品图片上传后能在表单预览，地址为 MinIO 返回的 url
- 未登录访问 `/user/dish/list` 能返回数据（依赖 C 端拦截器放行）
- 后台侧边栏两个菜单可切换，高亮正确

## 接口约定提醒

- `price` 单位是分，前端按元填写、提交时乘 100。全项目统一，报表和订单金额都依赖这条。
- 菜品删除是批量接口，`ids` 逗号分隔。
- 菜品分页返回 `categoryName` 是新增字段，C 的套餐页和 D 的订单页可以直接用。
- C 端浏览接口免登录；加购、下单仍然要 token。
- 加购接口 `POST /user/shoppingCart` 不属于本模块：`DishList.vue` 只抛出 `add-cart` 事件，由首页调用。

## 测试补齐（2026-11-21）

新增接口级与单元测试，`mvn test` 总数从 81 涨到 114，全绿。

- `CategoryApiIntegrationTest`（11 条）：新增、分页筛选、按类型查列表、修改名称与排序、**分类类型不可改**、删除空分类、**删除有菜品的分类被拒**、**删除有套餐的分类被拒**、分类不存在、无 token 401
- `DishApiIntegrationTest`（11 条）：新增带口味并查详情、**修改时口味整体覆盖（2 组改 1 组后仍是 1 组）**、同分类重名被拒、分页按分类与状态筛选并返回 `categoryName`、**批量删除**、**起售状态删除被拒**、**被套餐关联删除被拒**、**起停售**、**被起售套餐关联停售被拒**、C 端只返回起售且免登录、非法状态值被拒

为了让集成测试能跑起来，测试环境改了两处：

- `schema-test.sql`：补 `dish_flavor` 表（原来没有，菜品口味相关用例无法执行）
- `application-test.yml`：H2 连接串加 `NON_KEYWORDS=VALUE`（H2 2.x 把 `value` 当保留字，`dish_flavor.value` 会建表失败）

顺带修掉一个真实缺陷：`DishServiceImpl.getById()` 之前把 `categoryName` 传成 `null`，导致菜品详情接口不返回分类名称，与 `docs/api.md` 的约定不符。现在详情和列表行为一致，集成测试里已断言。

## 演示图片（2026-11-21）

`b_seed.sql` 里 7 个菜品的 `image` 原本全是 `NULL`，后台和 H5 都显示“无图”。现在改成仓库自带的占位图：

- `admin-web/public/dishes/*.svg`（7 张）
- `customer-h5/public/dishes/*.svg`（7 张，内容相同）

库里存的是相对路径 `/dishes/yuxiang-rousi.svg`，两个前端各自从 `public` 目录取。这样**不依赖 MinIO 也能看到图片**，演示时少一个故障点。要换真实照片，在「菜品管理」页上传后覆盖 `image` 字段即可，前端不用改。

## 图片存储开关（MinIO 降级，2026-11-21）

对应待补清单「MinIO 无降级」。实现取自王在 `推送文件/` 里的版本（`MinioProperties.isEnabled()` + `MinioStorageService` 内部走本地目录 + `WebMvcConfig` 的 `/uploads/**` 映射），B 这边只补了测试，没有另写一套。

| 配置 | 行为 | 返回的 URL |
| --- | --- | --- |
| `MINIO_ENABLED=false`（默认） | 落本地磁盘 | `MINIO_LOCAL_BASE_URL` + `/images/yyyy-MM-dd/<uuid>.<ext>` |
| `MINIO_ENABLED=true` | 走 MinIO | MinIO 的 `public-url` + `bucket/对象路径` |

相关配置（`application.yml` 的 `sky.minio` 段）：

- `enabled`：默认 `false`
- `local-dir`：默认 `./uploads`
- `local-base-url`：默认 `http://127.0.0.1:8080/uploads`

> 演示环境后端跑 18080 时必须覆盖，否则图片地址指向 8080：
> `$env:MINIO_LOCAL_BASE_URL='http://127.0.0.1:18080/uploads'`

测试覆盖：

- `MinioStorageServiceLocalModeTest` 8 条：落盘位置、URL 拼装、扩展名、同名不覆盖、空文件、尾斜杠、无扩展名、日期目录自动创建
- `LocalUploadIntegrationTest` 1 条：关掉开关后真实上传 → 文件落盘 → 通过 `/uploads/**` 取回且内容一致
- `DishImageFlowTest` 2 条：上传返回的地址能存进菜品，后台分页与 H5 列表都回显；无 token 上传 401



