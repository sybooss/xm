<template>
  <section class="login-page">
    <div class="login-shell">
      <div class="login-copy">
        <span class="login-mark">退</span>
        <h1>电商退换货智能客服系统</h1>
        <p>登录后进入咨询工作台，演示 AI 增强、多轮追问、处理轨迹和人工工单闭环。</p>
      </div>

      <el-form class="login-form" label-position="top" @submit.prevent="submit">
        <h2>管理员登录</h2>
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="admin" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" placeholder="123456" size="large" type="password" show-password @keydown.enter="submit" />
        </el-form-item>
        <el-button class="login-button" type="primary" size="large" :loading="authStore.loading" @click="submit">
          登录
        </el-button>
        <p class="login-tip">演示账号：admin / 123456</p>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()
const form = reactive({ username: 'admin', password: '123456' })

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  await authStore.login(form.username, form.password)
  ElMessage.success('登录成功')
  await router.replace('/chat')
}
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background:
    linear-gradient(120deg, rgb(37 99 235 / 10%), transparent 38%),
    #f4f6f8;
}

.login-shell {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) 360px;
  gap: 28px;
  width: min(920px, 100%);
  align-items: center;
}

.login-copy {
  padding: 10px;
}

.login-mark {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 8px;
  background: var(--brand);
  color: white;
  font-size: 24px;
  font-weight: 800;
}

h1 {
  margin: 18px 0 10px;
  color: var(--text);
  font-size: 34px;
  line-height: 1.2;
}

.login-copy p {
  max-width: 520px;
  margin: 0;
  color: var(--text-muted);
  font-size: 15px;
  line-height: 1.8;
}

.login-form {
  padding: 24px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  background: white;
}

h2 {
  margin: 0 0 18px;
  font-size: 20px;
}

.login-button {
  width: 100%;
}

.login-tip {
  margin: 12px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  text-align: center;
}

@media (max-width: 760px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  h1 {
    font-size: 28px;
  }
}
</style>
