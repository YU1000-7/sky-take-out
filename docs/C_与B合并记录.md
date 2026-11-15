# C 模块与 B 分支合并记录

> 合并基线：B 交付的 `sky-take-out-dev.zip`（含 A 员工模块 + B 分类/菜品模块）
> 结果：后端 34 项测试通过，两个前端构建通过，页面联调截图确认

## 1. 后端：直接并入，零冲突

C 的 37 个新增文件（实体、DTO、VO、枚举、Mapper、Service、Controller、测试）与 B 的代码没有重名和依赖交叉，全部原样并入。

合并后测试构成：

| 来源 | 用例数 |
| --- | --- |
| A · 员工 | 5（4 单元 + 1 集成） |
| B · 分类/菜品 | 2 |
| C · 套餐/购物车 | 27 |
| 公共 | 2（JWT） |
| **合计** | **34，全部通过** |

B 已经在 `WebMvcConfig` 里为 `/user/setmeal/list`、`/user/setmeal/dish/**` 预留了免登录放行，C 的浏览接口开箱可用，未改一行配置。购物车接口仍需登录，符合预期。

## 2. 前端：改为组件化集成，不再整体覆盖首页

B 把 H5 首页做成了容器（`Home.vue` + `CategoryNav` + `DishList`），C 按这套结构集成，只动三处：

### 2.1 `components/CategoryNav.vue`（改）

- 原来只查 `type=1`，改为同时查 `type=1` 和 `type=2`，合并为一个列表，套餐分类加「套餐」角标。
- `change` 事件由 `(categoryId)` 扩展为 `(categoryId, category)`，第二个参数带 `type`。只读第一个参数的调用方行为不变，向后兼容。
- 两个分类接口任一失败时只丢对应部分，不会整块空白。

### 2.2 `components/SetmealList.vue`（新增）

套餐卡片列表 + 套餐菜品弹层，与 `DishList` 保持同一套视觉与交互：

| 项 | 约定 |
| --- | --- |
| props | `categoryId` |
| 事件 | `add-cart`，参数 `{ setmealId, setmealName }` |
| 依赖 | `GET /user/setmeal/list`、`GET /user/setmeal/dish/{id}` |

### 2.3 `views/Home.vue`（改）

- 按当前分类的 `type` 切换渲染 `DishList` 或 `SetmealList`。
- `handleAddCart` 由 B 的占位 toast 改为真实调用 `POST /user/shoppingCart`，菜品与套餐两个来源统一收敛到这里。
- 新增购物车悬浮条（数量 + 合计 + 去结算），数据来自 `GET /user/shoppingCart/list`，未登录时静默隐藏。

B 的 hero、tabbar、`DishList` 组件均未改动。

## 3. 代码清理：消除与 B 的重复实现

| 位置 | 处理 |
| --- | --- |
| `admin-web/src/api/setmeal.js` | 删掉 `getSetmealCategoryOptions`（改用 B 的 `getCategoryList`）、`getDishPage`（改用 B 的 `getDishPage`） |
| `admin-web/src/views/setmeal/index.vue` | 分类下拉改调 `getCategoryList(2)`，选菜弹窗改调 B 的 `getDishPage` |
| 图片上传 | 由 `el-upload` 直传改为 `uploadDishImage`，与 B 的菜品上传同一条链路，token 由拦截器统一注入，不再手工拼 header |
| `customer-h5/src/api/setmeal.js` | 删掉重复的 `getCategoryList`，分类统一用 B 的 `api/menu.js` |

## 4. 文档调整

`docs/api.md` 按 B 的章节体系重排：

```
基础约定 → B 端员工 → B 端分类 → B 端菜品 → B 端套餐
→ C 端分类与菜品浏览 → C 端套餐查询 → C 端购物车 → 公共接口 → 其他模块接口
```

同时删除了原先「C 端分类查询（B 模块提供，C 端依赖）」这节过渡说明——B 的「C 端分类与菜品浏览」已经覆盖，保留会造成同一接口两份文档。

## 5. 自测结果

| 项 | 结果 |
| --- | --- |
| `mvn -B test` | 34 项通过，BUILD SUCCESS |
| `admin-web npm run build` | 通过 |
| `customer-h5 npm run build` | 通过 |
| H5 首页（菜品分类） | hero + 分类导航（套餐分类带角标）+ 菜品列表 + 购物车悬浮条，正常 |
| H5 首页（套餐分类） | 套餐卡片、价格、菜品/加入按钮，正常 |
| 套餐菜品弹层 | 菜品名称、单价、份数、加入购物车，正常 |
| H5 购物车页 | 菜品/套餐混排、口味标签、加减、合计、清空、结算，正常 |
| B 端套餐管理 | 列表、筛选、状态、操作按钮、侧边导航高亮，正常 |
| 选择菜品弹窗 | 菜品分页、图片、价格、状态、加入套餐，正常 |

## 6. 给 D 的接口口径提醒

购物车列表返回的 `amount` 是**单价（分）**，`number` 是份数，合计需自行计算 `number * amount`。套餐项 `setmealId` 有值、`dishId` 为 `null`；菜品项相反，且 `dishFlavor` 可能为 `null`。
