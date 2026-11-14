<template>
  <div class="edit-page">
    <van-nav-bar
      :title="isEdit ? '编辑地址' : '新增地址'"
      left-arrow
      @click-left="$router.back()"
    />

    <main>
      <van-form @submit="save">
        <van-cell-group inset>
          <van-field
            v-model.trim="form.consignee"
            label="收货人"
            placeholder="请输入收货人姓名"
            maxlength="32"
            :rules="[{ required: true, message: '请输入收货人' }]"
          />
          <van-field
            v-model.trim="form.phone"
            label="手机号"
            type="tel"
            maxlength="11"
            placeholder="请输入手机号"
            :rules="[
              { required: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '手机号格式错误' }
            ]"
          />
          <van-field v-model.trim="form.provinceCode" label="省份编码" placeholder="如 110000" />
          <van-field v-model.trim="form.provinceName" label="省份" placeholder="如 北京市" />
          <van-field v-model.trim="form.cityCode" label="城市编码" placeholder="如 110100" />
          <van-field v-model.trim="form.cityName" label="城市" placeholder="如 北京市" />
          <van-field v-model.trim="form.districtCode" label="区县编码" placeholder="如 110101" />
          <van-field v-model.trim="form.districtName" label="区县" placeholder="如 东城区" />
          <van-field
            v-model.trim="form.detail"
            label="详细地址"
            type="textarea"
            rows="2"
            autosize
            maxlength="255"
            show-word-limit
            placeholder="街道、小区、楼牌号等"
            :rules="[{ required: true, message: '请输入详细地址' }]"
          />
          <van-field v-model.trim="form.label" label="标签" placeholder="家 / 公司" />
          <van-cell center title="设为默认地址">
            <van-switch slot="right-icon" v-model="makeDefault" size="22px" />
          </van-cell>
        </van-cell-group>

        <div class="submit">
          <van-button block round type="info" native-type="submit" :loading="saving">
            保存地址
          </van-button>
        </div>
      </van-form>
    </main>
  </div>
</template>

<script>
import { Toast } from 'vant'
import {
  getAddress,
  createAddress,
  updateAddress
} from '../api/address'

function emptyForm() {
  return {
    id: null,
    consignee: '',
    sex: '',
    phone: '',
    provinceCode: '110000',
    provinceName: '北京市',
    cityCode: '110100',
    cityName: '北京市',
    districtCode: '110101',
    districtName: '东城区',
    detail: '',
    label: '家',
    isDefault: 0
  }
}

export default {
  name: 'AddressEdit',
  data() {
    return {
      form: emptyForm(),
      makeDefault: false,
      saving: false
    }
  },
  computed: {
    isEdit() {
      return Boolean(this.$route.params.id)
    }
  },
  created() {
    if (this.isEdit) {
      this.load()
    }
  },
  methods: {
    async load() {
      try {
        const data = await getAddress(this.$route.params.id)
        this.form = { ...emptyForm(), ...data }
        this.makeDefault = data.isDefault === 1
      } catch (error) {
        Toast(error.message || '地址加载失败')
      }
    },
    async save() {
      this.saving = true
      try {
        const payload = {
          ...this.form,
          isDefault: this.makeDefault ? 1 : 0
        }
        if (this.isEdit) {
          await updateAddress(payload)
        } else {
          delete payload.id
          await createAddress(payload)
        }
        Toast.success('保存成功')
        this.$router.replace('/address')
      } catch (error) {
        Toast(error.message || '保存失败')
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.edit-page {
  min-height: 100vh;
}

.edit-page main {
  padding: 20px 0 36px;
}

.submit {
  padding: 28px 18px 0;
}
</style>
