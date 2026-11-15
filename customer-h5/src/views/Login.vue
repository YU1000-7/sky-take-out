<template>
  <div class="login-page">
    <van-nav-bar title="客户登录" />

    <section class="hero">
      <p class="eyebrow">WELCOME TO SKY</p>
      <h1>好好吃饭，<br>从登录开始。</h1>
      <p>演示环境使用手机号直接登录，无需短信验证码。</p>
    </section>

    <section class="form-card">
      <van-form @submit="handleLogin">
        <van-field
          v-model.trim="phone"
          name="phone"
          type="tel"
          maxlength="11"
          clearable
          label="手机号"
          placeholder="请输入 11 位手机号"
          :rules="[
            { required: true, message: '请输入手机号' },
            { pattern: /^1[3-9]\d{9}$/, message: '手机号格式错误' }
          ]"
        />
        <div class="form-action">
          <van-button
            block
            round
            type="info"
            native-type="submit"
            :loading="loading"
          >
            登录 / 自动注册
          </van-button>
        </div>
      </van-form>
    </section>

    <p class="tips">登录即表示同意用户服务协议与隐私政策。</p>
  </div>
</template>

<script>
import { Toast } from 'vant'
import { login } from '../api/user'

export default {
  name: 'CustomerLogin',
  data() {
    return {
      phone: '13800138000',
      loading: false
    }
  },
  methods: {
    async handleLogin() {
      this.loading = true
      try {
        const data = await login(this.phone)
        localStorage.setItem('sky_customer_token', data.token)
        localStorage.setItem('sky_customer_profile', JSON.stringify(data))
        Toast.success('登录成功')
        this.$router.replace(this.$route.query.redirect || '/home')
      } catch (error) {
        Toast(error.message || '登录失败')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  padding-bottom: 40px;
}

.hero {
  padding: 58px 28px 50px;
  color: #fff;
  background:
    radial-gradient(circle at 86% 4%, rgba(242, 138, 46, 0.48), transparent 34%),
    #202938;
}

.eyebrow {
  margin: 0 0 18px;
  color: #c6ced9;
  font-size: 10px;
  letter-spacing: 0.22em;
}

.hero h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.35;
}

.hero p:last-child {
  margin: 18px 0 0;
  color: #b7c0cc;
  font-size: 13px;
  line-height: 1.7;
}

.form-card {
  margin: -24px 20px 0;
  padding: 18px 8px 10px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 16px 36px rgba(20, 28, 38, 0.1);
}

.form-action {
  padding: 22px 16px 10px;
}

.tips {
  margin: 24px 30px;
  color: var(--sky-muted);
  font-size: 11px;
  line-height: 1.7;
  text-align: center;
}
</style>
