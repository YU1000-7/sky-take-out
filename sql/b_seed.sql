-- ============================================================
-- 演示数据（分类 / 菜品 / 口味 / 套餐）
-- 用法：建库建表之后（sql/init.sql 已含三张表结构），再执行本文件
-- 注意：price 单位是「分」，600 表示 6.00 元
-- 如果重复执行会插入重复数据，先清空再跑：
--   DELETE FROM dish_flavor; DELETE FROM dish; DELETE FROM category;
-- ============================================================

USE sky_take_out;

INSERT INTO category (type, name, sort, status, create_time, update_time) VALUES
(1, '酒水饮料', 1, 1, NOW(), NOW()),
(1, '传统主食', 2, 1, NOW(), NOW()),
(1, '川湘菜',   3, 1, NOW(), NOW()),
(2, '人气套餐', 1, 1, NOW(), NOW()),
(2, '商务套餐', 2, 1, NOW(), NOW());

INSERT INTO dish (name, category_id, price, image, description, status, create_time, update_time) VALUES
('冰镇可乐', (SELECT id FROM category WHERE name = '酒水饮料' AND type = 1), 600,  '/dishes/bingzhen-kele.svg', '冰爽解渴', 1, NOW(), NOW()),
('鲜榨橙汁', (SELECT id FROM category WHERE name = '酒水饮料' AND type = 1), 1200, '/dishes/xianzha-chengzhi.svg', '当日鲜榨', 1, NOW(), NOW()),
('扬州炒饭', (SELECT id FROM category WHERE name = '传统主食' AND type = 1), 1800, '/dishes/yangzhou-chaofan.svg', '粒粒分明', 1, NOW(), NOW()),
('葱油拌面', (SELECT id FROM category WHERE name = '传统主食' AND type = 1), 1500, '/dishes/congyou-banmian.svg', '葱香浓郁', 1, NOW(), NOW()),
('鱼香肉丝', (SELECT id FROM category WHERE name = '川湘菜'   AND type = 1), 3800, '/dishes/yuxiang-rousi.svg', '经典川菜', 1, NOW(), NOW()),
('水煮牛肉', (SELECT id FROM category WHERE name = '川湘菜'   AND type = 1), 5800, '/dishes/shuizhu-niurou.svg', '麻辣鲜香', 1, NOW(), NOW()),
('麻婆豆腐', (SELECT id FROM category WHERE name = '川湘菜'   AND type = 1), 2800, '/dishes/mapo-doufu.svg', '停售状态示例', 0, NOW(), NOW());

INSERT INTO dish_flavor (dish_id, name, value)
SELECT id, '温度', '常温,冰镇' FROM dish WHERE name = '冰镇可乐'
UNION ALL SELECT id, '温度', '常温,冰镇' FROM dish WHERE name = '鲜榨橙汁'
UNION ALL SELECT id, '辣度', '微辣,中辣,特辣' FROM dish WHERE name = '鱼香肉丝'
UNION ALL SELECT id, '忌口', '不要香菜,不要葱' FROM dish WHERE name = '鱼香肉丝'
UNION ALL SELECT id, '辣度', '微辣,中辣,特辣' FROM dish WHERE name = '水煮牛肉';

-- 套餐演示数据：H5 首页可直接展示并加入购物车
INSERT INTO setmeal (category_id, name, price, status, description, image, create_time, update_time)
SELECT id, '川味双人套餐', 6800, 1, '鱼香肉丝 + 水煮牛肉', NULL, NOW(), NOW()
FROM category
WHERE name = '人气套餐' AND type = 2
LIMIT 1;

INSERT INTO setmeal_dish (setmeal_id, dish_id, name, price, copies)
SELECT s.id, d.id, d.name, d.price, 1
FROM setmeal s
JOIN dish d ON d.name IN ('鱼香肉丝', '水煮牛肉')
WHERE s.name = '川味双人套餐';

-- 菜品图片用仓库内自带的占位图：admin-web/public/dishes/*.svg 与 customer-h5/public/dishes/*.svg
-- 存的是相对路径（/dishes/xxx.svg），两个前端各自从 public 目录取，不依赖 MinIO 也能显示
-- 想换成真实照片时，在「菜品管理」页用 /admin/common/upload 上传后覆盖 image 字段即可
