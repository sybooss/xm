<template>
  <section class="login-page">
    <div class="login-shell">
      <div class="login-copy">
        <span class="login-mark">退</span>
        <h1>电商退换货智能客服系统</h1>
        <p>登录后进入咨询工作台，演示 AI 增强、多轮追问、处理轨迹和人工工单闭环。</p>
      </div>

      <el-form class="login-form" label-position="top" @submit.prevent="submit">
        <h2>{{ mode === 'register' ? '客户注册' : form.username === 'admin' ? '管理员登录' : '客户登录' }}</h2>
        <div class="mode-switch">
          <el-button :type="mode === 'login' ? 'primary' : 'default'" @click="switchMode('login')">登录</el-button>
          <el-button :type="mode === 'register' ? 'primary' : 'default'" @click="switchMode('register')">注册</el-button>
        </div>
        <div v-if="mode === 'login'" class="role-switch">
          <el-button :type="form.username === 'admin' ? 'primary' : 'default'" @click="useAdmin">管理员</el-button>
          <el-button :type="form.username === 'demo_customer' ? 'primary' : 'default'" @click="useCustomer">客户</el-button>
        </div>
        <el-form-item label="账号">
          <el-input v-model="form.username" :placeholder="mode === 'register' ? '4-30位字母、数字或下划线' : 'admin 或 demo_customer'" size="large" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" label="昵称">
          <el-input v-model="form.displayName" placeholder="用于页面展示，可不填" size="large" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" label="手机号">
          <el-input v-model="form.phone" placeholder="可选，11位手机号" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" placeholder="123456" size="large" type="password" show-password @keydown.enter="submit" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" label="确认密码">
          <el-input v-model="form.confirmPassword" placeholder="再次输入密码" size="large" type="password" show-password @keydown.enter="submit" />
        </el-form-item>
        <el-button class="login-button" type="primary" size="large" :loading="authStore.loading" @click="submit">
          {{ mode === 'register' ? '注册并登录' : '登录' }}
        </el-button>
        <p class="login-tip">{{ mode === 'register' ? '注册后默认拥有客户权限，可进入咨询工作台。' : '管理员：admin / 123456；客户：demo_customer / 123456' }}</p>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()
const mode = ref('login')
const form = reactive({
  username: 'admin',
  password: '123456',
  confirmPassword: '',
  displayName: '',
  phone: ''
})

function switchMode(nextMode) {
  mode.value = nextMode
  if (nextMode === 'login') {
    useAdmin()
  } else {
    form.username = ''
    form.password = ''
    form.confirmPassword = ''
    form.displayName = ''
    form.phone = ''
  }
}

function useAdmin() {
  form.username = 'admin'
  form.password = '123456'
  form.confirmPassword = ''
}

function useCustomer() {
  form.username = 'demo_customer'
  form.password = '123456'
  form.confirmPassword = ''
}

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  if (mode.value === 'register') {
    await authStore.register({
      username: form.username,
      password: form.password,
      confirmPassword: form.confirmPassword,
      displayName: form.displayName,
      phone: form.phone
    })
    ElMessage.success('注册成功')
  } else {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
  }
  await router.replace(authStore.isAdmin ? '/showcase' : '/chat')
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

.mode-switch,
.role-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 16px;
}

.mode-switch {
  margin-bottom: 10px;
}

.mode-switch .el-button,
.role-switch .el-button {
  width: 100%;
  margin: 0;
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
