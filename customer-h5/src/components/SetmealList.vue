<template>
  <div class="setmeal-list">
    <div v-if="loading" class="state">
      <van-loading size="20px">加载中…</van-loading>
    </div>

    <div v-else-if="!list.length" class="empty">该分类暂无在售套餐</div>

    <template v-else>
      <article v-for="item in list" :key="item.id" class="setmeal-card">
        <div class="thumb">
          <img v-if="item.image" :src="item.image" :alt="item.name">
          <div v-else class="thumb-placeholder">暂无图片</div>
        </div>
        <div class="info">
          <div class="name">{{ item.name }}</div>
          <div class="desc">{{ item.description || '门店精选组合' }}</div>
          <div class="bottom">
            <span class="price">￥{{ formatPrice(item.price) }}</span>
            <div class="actions">
              <van-button size="mini" plain type="info" round @click="openDishes(item)">菜品</van-button>
              <van-button size="mini" type="danger" round icon="plus" @click="add(item)">加入</van-button>
            </div>
          </div>
        </div>
      </article>
    </template>

    <van-popup v-model="show" position="bottom" round closeable>
      <div class="detail-panel">
        <div class="detail-head">
          <strong>{{ current.name }}</strong>
          <span>￥{{ formatPrice(current.price) }}</span>
        </div>

        <div class="detail-body">
          <div v-if="detailLoading" class="state">
            <van-loading size="18px">加载套餐菜品…</van-loading>
          </div>
          <div v-else-if="!dishes.length" class="empty">套餐暂无菜品</div>
          <div v-for="dish in dishes" :key="dish.id" class="detail-dish">
            <div class="detail-thumb">
              <img v-if="dish.image" :src="dish.image" :alt="dish.name">
            </div>
            <div class="detail-info">
              <strong>{{ dish.name }}</strong>
              <small>￥{{ formatPrice(dish.price) }} × {{ dish.copies }} 份</small>
            </div>
            <van-tag v-if="dish.dishStatus === 0" type="danger" plain>已停售</van-tag>
          </div>
        </div>

        <div class="detail-foot">
          <van-button type="danger" block round @click="add(current)">加入购物车</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script>
import { getSetmealDishes, getSetmealList } from '../api/setmeal'

export default {
  name: 'SetmealList',
  props: {
    categoryId: {
      type: [Number, String],
      default: null
    }
  },
  data() {
    return {
      list: [],
      loading: false,
      show: false,
      detailLoading: false,
      dishes: [],
      current: {}
    }
  },
  watch: {
    categoryId: {
      immediate: true,
      handler() {
        this.loadList()
      }
    }
  },
  methods: {
    formatPrice(price) {
      return ((price || 0) / 100).toFixed(2)
    },
    async loadList() {
      this.loading = true
      try {
        this.list = (await getSetmealList(this.categoryId)) || []
      } catch (e) {
        this.list = []
        this.$toast(e.message || '套餐加载失败')
      } finally {
        this.loading = false
      }
    },
    async openDishes(setmeal) {
      this.current = setmeal
      this.dishes = []
      this.show = true
      this.detailLoading = true
      try {
        this.dishes = (await getSetmealDishes(setmeal.id)) || []
      } catch (e) {
        this.$toast(e.message || '套餐菜品加载失败')
      } finally {
        this.detailLoading = false
      }
    },
    add(setmeal) {
      if (!setmeal || !setmeal.id) {
        return
      }
      this.$emit('add-cart', { setmealId: setmeal.id, setmealName: setmeal.name })
      this.show = false
    }
  }
}
</script>

<style scoped>
.setmeal-list {
  flex: 1;
  min-width: 0;
  padding: 8px 12px 80px;
  overflow-y: auto;
}

.state,
.empty {
  padding: 48px 0;
  color: #969799;
  font-size: 13px;
  text-align: center;
}

.setmeal-card {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f2f3f5;
}

.thumb {
  flex: 0 0 88px;
  width: 88px;
  height: 88px;
  margin-right: 12px;
  overflow: hidden;
  border-radius: 8px;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #c8c9cc;
  font-size: 12px;
  background: #f7f8fa;
}

.info {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  justify-content: space-between;
}

.name {
  font-size: 15px;
  font-weight: 500;
  color: #323233;
}

.desc {
  margin-top: 2px;
  color: #969799;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price {
  color: #ee0a24;
  font-size: 16px;
  font-weight: 500;
}

.actions {
  display: flex;
  gap: 6px;
}

.detail-panel {
  display: flex;
  flex-direction: column;
  max-height: 68vh;
  padding: 20px 18px 18px;
}

.detail-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-bottom: 14px;
  border-bottom: 1px solid #f2f3f5;
}

.detail-head strong {
  font-size: 17px;
}

.detail-head span {
  color: #ee0a24;
  font-size: 16px;
  font-weight: 600;
}

.detail-body {
  flex: 1;
  overflow-y: auto;
  padding: 6px 0;
}

.detail-dish {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed #f2f3f5;
}

.detail-thumb {
  flex: 0 0 46px;
  width: 46px;
  height: 46px;
  margin-right: 10px;
  overflow: hidden;
  border-radius: 6px;
  background: #f7f8fa;
}

.detail-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-info {
  flex: 1;
  min-width: 0;
}

.detail-info strong {
  display: block;
  font-size: 14px;
  color: #323233;
}

.detail-info small {
  color: #969799;
  font-size: 12px;
}

.detail-foot {
  padding-top: 12px;
}
</style>
