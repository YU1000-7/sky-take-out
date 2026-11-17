<template>
  <van-sidebar v-if="categories.length" class="category-nav" :active-key="activeIndex" @change="handleChange">
    <van-sidebar-item v-for="item in categories" :key="item.key">
      <template #title>
        <span class="cat-name">{{ item.name }}</span>
        <em v-if="item.type === 2" class="cat-tag">套餐</em>
      </template>
    </van-sidebar-item>
  </van-sidebar>
  <div v-else class="category-nav category-empty">暂无分类</div>
</template>

<script>
import { getCategoryList } from '../api/menu'

export default {
  name: 'CategoryNav',
  data() {
    return {
      categories: [],
      activeIndex: 0
    }
  },
  async created() {
    try {
      // 菜品分类与套餐分类分开查，任一接口失败都不影响另一类展示。
      const [dishCategories, setmealCategories] = await Promise.all([
        getCategoryList(1).catch(() => []),
        getCategoryList(2).catch(() => [])
      ])

      const categories = []
      ;(dishCategories || []).forEach(item => {
        categories.push({ key: `dish-${item.id}`, id: item.id, name: item.name, type: 1 })
      })
      ;(setmealCategories || []).forEach(item => {
        categories.push({ key: `setmeal-${item.id}`, id: item.id, name: item.name, type: 2 })
      })

      this.categories = categories
      if (categories.length) {
        this.applyChange(0)
      }
    } catch (e) {
      this.categories = []
      this.$toast(e.message || '分类加载失败')
    }
  },
  methods: {
    handleChange(index) {
      this.applyChange(index)
    },
    applyChange(index) {
      this.activeIndex = index
      const category = this.categories[index]
      if (category) {
        // 第二个参数带上 type，首页据此切换菜品区与套餐区；只读第一个参数的调用方不受影响。
        this.$emit('change', category.id, category)
      }
    }
  }
}
</script>

<style scoped>
.category-nav {
  width: 92px;
  flex: 0 0 92px;
  height: 100%;
  background: #f7f8fa;
}

.category-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #969799;
  font-size: 12px;
}

.cat-name {
  display: block;
  line-height: 1.4;
}

.cat-tag {
  display: inline-block;
  margin-top: 3px;
  padding: 0 4px;
  color: #ee0a24;
  border: 1px solid #f8b8c1;
  border-radius: 3px;
  font-size: 9px;
  font-style: normal;
  line-height: 13px;
}
</style>
