<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">知识库</h2>
        <p class="page-subtitle">维护退换货规则、FAQ 和客服话术，并测试知识检索命中。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增文档</el-button>
    </div>

    <section class="panel">
      <div class="panel-header">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="关键词" clearable style="width: 220px" @keyup.enter="loadDocs" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
          <el-select v-model="query.intentCode" placeholder="意图" clearable style="width: 180px">
            <el-option v-for="item in intentOptions" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
          <el-button :icon="Search" @click="loadDocs">查询</el-button>
        </div>
      </div>
      <div class="panel-body">
        <el-table v-loading="loading" :data="docs" height="420">
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="categoryName" label="分类" width="120" />
          <el-table-column prop="docType" label="类型" width="100" />
          <el-table-column prop="intentCode" label="意图" width="150" />
          <el-table-column prop="priority" label="优先级" width="90" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><StatusTag :value="row.status" /></template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :icon="Edit" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          class="pager"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @current-change="loadDocs"
          @size-change="loadDocs"
        />
      </div>
    </section>

    <section class="panel search-panel">
      <div class="panel-header">
        <h3 class="panel-title">检索调试</h3>
      </div>
      <div class="panel-body search-body">
        <el-input v-model="searchText" placeholder="输入用户问题，例如：退货多久到账" clearable />
        <el-button type="primary" :icon="Search" @click="runSearch">检索</el-button>
      </div>
      <div v-if="searchHits.length" class="panel-body hit-grid">
        <div v-for="hit in searchHits" :key="hit.id" class="hit-item">
          <strong>{{ hit.title }}</strong>
          <p>{{ hit.contentPreview || hit.answer || hit.content }}</p>
        </div>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑知识文档' : '新增知识文档'" width="720px">
      <el-form :model="form" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" style="width: 100%">
                <el-option v-for="item in categories" :key="item.id" :label="item.categoryName || item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型">
              <el-select v-model="form.docType" style="width: 100%">
                <el-option label="FAQ" value="FAQ" />
                <el-option label="平台规则" value="POLICY" />
                <el-option label="客服话术" value="SCRIPT" />
                <el-option label="通知说明" value="NOTICE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="意图">
              <el-select v-model="form.intentCode" style="width: 100%">
                <el-option v-for="item in intentOptions" :key="item.code" :label="item.name" :value="item.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级"><el-input-number v-model="form.priority" :min="1" :max="100" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="启用" value="ENABLED" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="常见问法"><el-input v-model="form.question" /></el-form-item>
        <el-form-item label="标准答复"><el-input v-model="form.answer" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="详细内容"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="关键词"><el-input v-model="form.keywords" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import StatusTag from '../components/common/StatusTag.vue'
import {
  createKnowledgeDoc,
  deleteKnowledgeDoc,
  listCategories,
  pageKnowledgeDocs,
  searchKnowledgeDocs,
  updateKnowledgeDoc
} from '../api/knowledgeApi'
import { useSystemStore } from '../stores/systemStore'

const systemStore = useSystemStore()
const docs = ref([])
const total = ref(0)
const categories = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const searchText = ref('退货多久到账')
const searchHits = ref([])

const query = reactive({ page: 1, pageSize: 10, keyword: '', status: '', intentCode: '' })
const form = reactive(defaultForm())
const intentOptions = computed(() => systemStore.enums?.intentCodes || [])

function defaultForm() {
  return {
    categoryId: null,
    title: '',
    docType: 'FAQ',
    intentCode: 'RETURN_APPLY',
    scenario: '',
    question: '',
    answer: '',
    content: '',
    keywords: '',
    priority: 10,
    status: 'ENABLED'
  }
}

function resetForm(row) {
  Object.assign(form, defaultForm(), row || {})
}

async function loadDocs() {
  loading.value = true
  try {
    const data = await pageKnowledgeDocs(query)
    docs.value = data?.rows || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  categories.value = await listCategories({ enabled: 1 })
}

function openCreate() {
  editingId.value = null
  resetForm({ categoryId: categories.value[0]?.id })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  resetForm(row)
  dialogVisible.value = true
}

async function save() {
  if (editingId.value) {
    await updateKnowledgeDoc(editingId.value, form)
  } else {
    await createKnowledgeDoc(form)
  }
  dialogVisible.value = false
  await loadDocs()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除「${row.title}」吗？`, '删除确认', { type: 'warning' })
  await deleteKnowledgeDoc(row.id)
  await loadDocs()
}

async function runSearch() {
  searchHits.value = await searchKnowledgeDocs({ query: searchText.value, limit: 5 })
}

onMounted(async () => {
  await Promise.all([systemStore.loadEnums().catch(() => {}), loadCategories(), loadDocs()])
})
</script>

<style scoped>
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.search-panel {
  margin-top: 14px;
}

.search-body {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.hit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  border-top: 1px solid var(--line-soft);
}

.hit-item {
  padding: 10px;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  background: var(--surface-soft);
}

.hit-item p {
  margin: 6px 0 0;
  color: var(--text-muted);
  line-height: 1.6;
}

@media (max-width: 760px) {
  .search-body,
  .hit-grid {
    grid-template-columns: 1fr;
  }
}
</style>
