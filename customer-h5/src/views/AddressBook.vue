<template>
  <div class="address-page">
    <van-nav-bar
      title="地址簿"
      left-arrow
      @click-left="$router.push('/home')"
      @click-right="$router.push('/address/edit')"
    >
      <span slot="right" class="nav-action">新增</span>
    </van-nav-bar>

    <div v-if="loading" class="state">
      <van-loading>加载中...</van-loading>
    </div>

    <van-empty v-else-if="addresses.length === 0" description="还没有收货地址">
      <van-button round type="info" @click="$router.push('/address/edit')">
        新增地址
      </van-button>
    </van-empty>

    <main v-else class="address-list">
      <article v-for="item in addresses" :key="item.id" class="address-card">
        <header>
          <div class="person">
            <strong>{{ item.consignee }}</strong>
            <span>{{ item.phone }}</span>
            <van-tag v-if="item.isDefault === 1" type="warning" plain>
              默认
            </van-tag>
          </div>
          <van-icon
            name="edit"
            size="18"
            color="#7b8492"
            @click="$router.push(`/address/edit/${item.id}`)"
          />
        </header>
        <p class="address-text">{{ formatAddress(item) }}</p>
        <footer>
          <van-button
            v-if="item.isDefault !== 1"
            size="mini"
            plain
            type="info"
            @click="makeDefault(item)"
          >
            设为默认
          </van-button>
          <van-button size="mini" plain type="danger" @click="remove(item)">
            删除
          </van-button>
        </footer>
      </article>
    </main>
  </div>
</template>

<script>
import { Toast } from 'vant'
import {
  listAddresses,
  deleteAddress,
  setDefaultAddress
} from '../api/address'

export default {
  name: 'AddressBook',
  data() {
    return {
      addresses: [],
      loading: false
    }
  },
  created() {
    this.load()
  },
  methods: {
    async load() {
      this.loading = true
      try {
        this.addresses = (await listAddresses()) || []
      } catch (error) {
        Toast(error.message || '地址加载失败')
      } finally {
        this.loading = false
      }
    },
    formatAddress(item) {
      return [
        item.provinceName,
        item.cityName,
        item.districtName,
        item.detail
      ].filter(Boolean).join(' ')
    },
    async makeDefault(item) {
      await setDefaultAddress(item.id)
      Toast.success('已设为默认地址')
      this.load()
    },
    async remove(item) {
      await this.$dialog.confirm({
        title: '删除地址',
        message: `确定删除“${item.consignee}”的地址吗？`
      })
      await deleteAddress(item.id)
      Toast.success('删除成功')
      this.load()
    }
  }
}
</script>

<style scoped>
.address-page {
  min-height: 100vh;
}

.nav-action {
  color: var(--sky-accent);
  font-size: 14px;
}

.state {
  padding-top: 34vh;
  text-align: center;
}

.address-list {
  padding: 18px 16px 32px;
}

.address-card {
  margin-bottom: 14px;
  padding: 18px;
  border: 1px solid var(--sky-line);
  border-radius: 14px;
  background: #fff;
}

.address-card header,
.person,
.address-card footer {
  display: flex;
  align-items: center;
}

.address-card header {
  justify-content: space-between;
}

.person {
  gap: 10px;
}

.person strong {
  font-size: 16px;
}

.person span {
  color: var(--sky-muted);
  font-size: 13px;
}

.address-text {
  margin: 13px 0;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.7;
}

.address-card footer {
  gap: 8px;
  justify-content: flex-end;
}
</style>
