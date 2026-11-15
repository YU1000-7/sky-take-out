<template>
  <section class="setmeal-page">
    <div class="page-heading">
      <div>
        <h2>套餐管理</h2>
        <p>套餐是菜品的组合销售单元。保存时套餐与套餐菜品在同一事务内整体写入，起售要求所含菜品全部起售。</p>
      </div>
      <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增套餐</el-button>
    </div>

    <div class="page-card toolbar">
      <el-input
        v-model.trim="query.name"
        clearable
        placeholder="按套餐名称搜索"
        prefix-icon="el-icon-search"
        @keyup.enter.native="handleSearch"
        @clear="handleSearch"
      />
      <el-select
        v-if="categoryOptions.length"
        v-model="query.categoryId"
        clearable
        placeholder="全部分类"
        @change="handleSearch"
      >
        <el-option
          v-for="item in categoryOptions"
          :key="item.id"
          :label="item.name"
          :value="item.id"
        />
      </el-select>
      <el-select v-model="query.status" clearable placeholder="全部状态" @change="handleSearch">
        <el-option label="起售" :value="1" />
        <el-option label="停售" :value="0" />
      </el-select>
      <el-button type="primary" plain @click="handleSearch">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <div class="page-card table-card">
      <div class="table-actions">
        <el-button size="small" :disabled="!selectedRows.length" @click="removeSelected">
          批量删除
        </el-button>
        <span v-if="selectedRows.length" class="selected-tip">已选 {{ selectedRows.length }} 项</span>
        <span v-if="categoryLoadError" class="warn-tip">
          未取到套餐分类（依赖 B 模块的 /admin/category/list），可直接填写分类 ID。
        </span>
      </div>

      <el-table
        v-loading="loading"
        :data="records"
        row-key="id"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column prop="id" label="ID" width="72" align="center" />
        <el-table-column label="图片" width="88" align="center">
          <template slot-scope="{ row }">
            <el-image
              v-if="row.image"
              :src="row.image"
              :preview-src-list="[row.image]"
              fit="cover"
              class="thumb"
            />
            <span v-else class="thumb empty">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="套餐名称" min-width="150" />
        <el-table-column label="分类" min-width="120">
          <template slot-scope="{ row }">
            {{ row.categoryName || categoryLabel(row.categoryId) }}
          </template>
        </el-table-column>
        <el-table-column label="价格" width="110" align="right">
          <template slot-scope="{ row }">￥{{ formatPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="96" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '起售' : '停售' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" @click="openEdit(row)">编辑</el-button>
            <el-button type="text" @click="toggleStatus(row)">
              {{ row.status === 1 ? '停售' : '起售' }}
            </el-button>
            <el-button type="text" class="danger" @click="removeOne(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="query.page"
          :page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog
      :title="form.id ? '编辑套餐' : '新增套餐'"
      :visible.sync="dialogVisible"
      width="760px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="套餐名称" prop="name">
          <el-input v-model.trim="form.name" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="套餐分类" prop="categoryId">
          <el-select
            v-if="categoryOptions.length"
            v-model="form.categoryId"
            placeholder="请选择套餐分类"
            style="width: 100%"
          >
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-input-number v-else v-model="form.categoryId" :min="1" :controls="false" />
          <span v-if="categoryLoadError" class="form-tip">分类接口不可用，请直接填写套餐分类 ID</span>
        </el-form-item>
        <el-form-item label="价格（元）" prop="priceYuan">
          <el-input-number v-model="form.priceYuan" :min="0.01" :precision="2" :step="1" />
          <span class="form-tip">提交后按分存储，当前为 {{ priceInCent }} 分</span>
        </el-form-item>
        <el-form-item label="套餐图片">
          <el-upload
            class="uploader"
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="handleUpload"
          >
            <img v-if="form.image" :src="form.image" class="uploader-image" alt="套餐图片">
            <div v-else class="uploader-holder">
              <i class="el-icon-plus" />
              <span>上传图片</span>
            </div>
          </el-upload>
          <el-input
            v-model.trim="form.image"
            class="image-input"
            placeholder="也可直接粘贴图片地址"
          />
          <span class="form-tip">统一调用 /admin/common/upload，图片地址由 MinIO 返回</span>
        </el-form-item>
        <el-form-item label="套餐描述">
          <el-input
            v-model.trim="form.description"
            type="textarea"
            :rows="2"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">起售</el-radio>
            <el-radio :label="0">停售</el-radio>
          </el-radio-group>
          <span class="form-tip">起售时校验套餐内菜品是否全部起售</span>
        </el-form-item>
        <el-form-item label="套餐菜品">
          <div class="dish-panel">
            <div class="dish-panel-head">
              <el-button size="small" icon="el-icon-search" @click="openPicker">选择菜品</el-button>
              <span class="form-tip">已选 {{ form.setmealDishes.length }} 个菜品</span>
            </div>
            <el-table :data="form.setmealDishes" size="mini" empty-text="还没有选择菜品">
              <el-table-column prop="dishId" label="菜品ID" width="90" align="center" />
              <el-table-column label="菜品名称" min-width="140">
                <template slot-scope="{ row }">
                  {{ row.dishName || `菜品 ${row.dishId}` }}
                  <el-tag v-if="row.dishStatus === 0" type="warning" size="mini">已停售</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="份数" width="130" align="center">
                <template slot-scope="{ row }">
                  <el-input-number v-model="row.copies" size="mini" :min="1" :max="99" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" align="center">
                <template slot-scope="{ $index }">
                  <el-button type="text" class="danger" @click="form.setmealDishes.splice($index, 1)">
                    移除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="选择菜品"
      :visible.sync="pickerVisible"
      width="720px"
      :close-on-click-modal="false"
    >
      <div class="picker-toolbar">
        <el-input
          v-model.trim="pickerQuery.name"
          clearable
          placeholder="按菜品名称搜索"
          prefix-icon="el-icon-search"
          @keyup.enter.native="loadDishPage"
          @clear="loadDishPage"
        />
        <el-button type="primary" plain @click="loadDishPage">查询</el-button>
      </div>

      <el-alert
        v-if="pickerError"
        :title="pickerError"
        type="warning"
        :closable="false"
        show-icon
      />

      <el-table
        v-loading="pickerLoading"
        :data="pickerRecords"
        row-key="id"
        size="small"
        @selection-change="handlePickerSelection"
      >
        <el-table-column type="selection" width="44" align="center" />
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="图片" width="70" align="center">
          <template slot-scope="{ row }">
            <el-image v-if="row.image" :src="row.image" fit="cover" class="thumb small" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="菜品名称" min-width="140" />
        <el-table-column label="价格" width="100" align="right">
          <template slot-scope="{ row }">￥{{ formatPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
              {{ row.status === 1 ? '起售' : '停售' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="picker-pagination">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="pickerQuery.page"
          :page-size="pickerQuery.pageSize"
          :total="pickerTotal"
          @current-change="handlePickerPageChange"
        />
      </div>

      <el-collapse v-if="pickerError" class="manual-add">
        <el-collapse-item title="菜品接口不可用时，先手动登记菜品" name="manual">
          <div class="manual-row">
            <el-input-number v-model="manualDish.dishId" :min="1" :controls="false" placeholder="菜品ID" />
            <el-input-number v-model="manualDish.copies" :min="1" :max="99" />
            <el-button size="small" @click="addManualDish">加入套餐</el-button>
          </div>
        </el-collapse-item>
      </el-collapse>

      <span slot="footer">
        <el-button @click="pickerVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPicker">加入套餐</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { getCategoryList } from '../../api/category'
import { getDishPage, uploadDishImage } from '../../api/dish'
import {
  changeSetmealStatus,
  createSetmeal,
  deleteSetmeal,
  getSetmealById,
  getSetmealPage,
  updateSetmeal
} from '../../api/setmeal'

function createEmptyForm() {
  return {
    id: null,
    name: '',
    categoryId: null,
    priceYuan: undefined,
    image: '',
    description: '',
    status: 0,
    setmealDishes: []
  }
}

export default {
  name: 'SetmealPage',
  data() {
    return {
      loading: false,
      saving: false,
      dialogVisible: false,
      pickerVisible: false,
      pickerLoading: false,
      pickerError: '',
      categoryOptions: [],
      categoryLoadError: false,
      query: {
        page: 1,
        pageSize: 10,
        name: '',
        categoryId: null,
        status: null
      },
      total: 0,
      records: [],
      selectedRows: [],
      form: createEmptyForm(),
      pickerQuery: {
        page: 1,
        pageSize: 8,
        name: ''
      },
      pickerTotal: 0,
      pickerRecords: [],
      pickerSelection: [],
      manualDish: {
        dishId: null,
        copies: 1
      },
      rules: {
        name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
        categoryId: [{ required: true, message: '请选择套餐分类', trigger: 'change' }],
        priceYuan: [{ required: true, message: '请输入套餐价格', trigger: 'blur' }]
      }
    }
  },
  computed: {
    priceInCent() {
      if (!this.form.priceYuan) {
        return 0
      }
      return Math.round(Number(this.form.priceYuan) * 100)
    }
  },
  created() {
    this.loadCategories()
    this.loadSetmeals()
  },
  methods: {
    formatPrice(price) {
      if (price === null || price === undefined) {
        return '0.00'
      }
      return (Number(price) / 100).toFixed(2)
    },
    categoryLabel(categoryId) {
      const matched = this.categoryOptions.find(item => item.id === categoryId)
      return matched ? matched.name : `分类 ${categoryId || '-'}`
    },
    async loadCategories() {
      try {
        const data = await getCategoryList(2)
        this.categoryOptions = data || []
        this.categoryLoadError = false
      } catch (error) {
        this.categoryOptions = []
        this.categoryLoadError = true
      }
    },
    async loadSetmeals() {
      this.loading = true
      try {
        const data = await getSetmealPage(this.query)
        this.records = data.records || []
        this.total = data.total || 0
      } catch (error) {
        this.$message.error(error.message)
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.query.page = 1
      this.loadSetmeals()
    },
    resetSearch() {
      this.query = {
        page: 1,
        pageSize: this.query.pageSize,
        name: '',
        categoryId: null,
        status: null
      }
      this.loadSetmeals()
    },
    handlePageChange(page) {
      this.query.page = page
      this.loadSetmeals()
    },
    handleSizeChange(pageSize) {
      this.query.pageSize = pageSize
      this.query.page = 1
      this.loadSetmeals()
    },
    handleSelectionChange(rows) {
      this.selectedRows = rows
    },
    openCreate() {
      this.form = createEmptyForm()
      this.dialogVisible = true
    },
    async openEdit(row) {
      try {
        const detail = await getSetmealById(row.id)
        this.form = {
          id: detail.id,
          name: detail.name,
          categoryId: detail.categoryId,
          priceYuan: Number((Number(detail.price) / 100).toFixed(2)),
          image: detail.image || '',
          description: detail.description || '',
          status: detail.status,
          setmealDishes: (detail.setmealDishes || []).map(item => ({
            dishId: item.dishId,
            dishName: item.dishName,
            copies: item.copies || 1,
            dishStatus: item.dishStatus
          }))
        }
        this.dialogVisible = true
      } catch (error) {
        this.$message.error(error.message)
      }
    },
    resetForm() {
      this.form = createEmptyForm()
      if (this.$refs.form) {
        this.$refs.form.clearValidate()
      }
    },
    submitForm() {
      this.$refs.form.validate(async valid => {
        if (!valid || this.saving) {
          return
        }
        if (!this.form.setmealDishes.length) {
          this.$message.warning('请先选择套餐内菜品')
          return
        }

        const payload = {
          id: this.form.id,
          name: this.form.name,
          categoryId: this.form.categoryId,
          price: this.priceInCent,
          image: this.form.image,
          description: this.form.description,
          status: this.form.status,
          setmealDishes: this.form.setmealDishes.map(item => ({
            dishId: item.dishId,
            copies: item.copies || 1
          }))
        }

        this.saving = true
        try {
          if (payload.id) {
            await updateSetmeal(payload)
            this.$message.success('套餐已更新')
          } else {
            await createSetmeal(payload)
            this.$message.success('套餐已新增')
          }
          this.dialogVisible = false
          this.loadSetmeals()
        } catch (error) {
          this.$message.error(error.message)
        } finally {
          this.saving = false
        }
      })
    },
    async toggleStatus(row) {
      const nextStatus = row.status === 1 ? 0 : 1
      const action = nextStatus === 1 ? '起售' : '停售'
      try {
        await this.$confirm(`确定要${action}套餐“${row.name}”吗？`, '确认操作', { type: 'warning' })
        await changeSetmealStatus(row.id, nextStatus)
        this.$message.success(`已${action}`)
        this.loadSetmeals()
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
          this.$message.error(error.message || '操作失败')
        }
      }
    },
    removeOne(row) {
      this.removeByIds([row.id], row.name)
    },
    removeSelected() {
      const ids = this.selectedRows.map(row => row.id)
      this.removeByIds(ids, `${ids.length} 个套餐`)
    },
    async removeByIds(ids, label) {
      if (!ids.length) {
        return
      }
      try {
        await this.$confirm(
          `删除后套餐与菜品关联会一并清理，且不可恢复。确定删除${label}吗？`,
          '确认删除',
          { type: 'warning' }
        )
        await deleteSetmeal(ids.join(','))
        this.$message.success('删除成功')
        this.loadSetmeals()
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
          this.$message.error(error.message || '删除失败')
        }
      }
    },
    openPicker() {
      this.pickerSelection = []
      this.pickerVisible = true
      this.loadDishPage()
    },
    async loadDishPage() {
      this.pickerLoading = true
      this.pickerError = ''
      try {
        const data = await getDishPage({
          page: this.pickerQuery.page,
          pageSize: this.pickerQuery.pageSize,
          name: this.pickerQuery.name,
          status: 1
        })
        this.pickerRecords = data.records || []
        this.pickerTotal = data.total || 0
      } catch (error) {
        this.pickerRecords = []
        this.pickerTotal = 0
        this.pickerError = `菜品查询接口不可用（${error.message}）。该接口由 B 模块提供，可先用下方手动登记菜品完成套餐配置。`
      } finally {
        this.pickerLoading = false
      }
    },
    handlePickerPageChange(page) {
      this.pickerQuery.page = page
      this.loadDishPage()
    },
    handlePickerSelection(rows) {
      this.pickerSelection = rows
    },
    confirmPicker() {
      if (!this.pickerSelection.length) {
        this.$message.warning('请先勾选菜品')
        return
      }
      this.mergeDishes(
        this.pickerSelection.map(row => ({
          dishId: row.id,
          dishName: row.name,
          copies: 1,
          dishStatus: row.status
        }))
      )
      this.pickerVisible = false
    },
    addManualDish() {
      if (!this.manualDish.dishId) {
        this.$message.warning('请填写菜品 ID')
        return
      }
      this.mergeDishes([
        {
          dishId: this.manualDish.dishId,
          dishName: '',
          copies: this.manualDish.copies || 1,
          dishStatus: null
        }
      ])
      this.manualDish = { dishId: null, copies: 1 }
    },
    mergeDishes(dishes) {
      const existing = new Set(this.form.setmealDishes.map(item => item.dishId))
      let added = 0
      dishes.forEach(item => {
        if (existing.has(item.dishId)) {
          return
        }
        existing.add(item.dishId)
        this.form.setmealDishes.push(item)
        added += 1
      })
      if (added) {
        this.$message.success(`已加入 ${added} 个菜品`)
      } else {
        this.$message.info('所选菜品都已在套餐中')
      }
    },
    beforeUpload(file) {
      const isImage = /^image\//.test(file.type)
      if (!isImage) {
        this.$message.error('只能上传图片文件')
        return false
      }
      if (file.size / 1024 / 1024 > 5) {
        this.$message.error('图片大小不能超过 5MB')
        return false
      }
      return true
    },
    async handleUpload(options) {
      try {
        this.form.image = await uploadDishImage(options.file)
        this.$message.success('图片上传成功')
      } catch (error) {
        this.$message.error(error.message || '图片上传失败，请检查 MinIO 是否可用')
      }
    }
  }
}
</script>

<style scoped>
.setmeal-page {
  max-width: 1420px;
  margin: 0 auto;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
}

.page-heading h2 {
  margin: 0;
  font-size: 22px;
}

.page-heading p {
  max-width: 720px;
  margin: 8px 0 0;
  color: #7b8492;
  font-size: 13px;
  line-height: 1.7;
}

.toolbar {
  display: flex;
  gap: 10px;
  padding: 16px 18px;
  margin-bottom: 18px;
}

.toolbar .el-input {
  width: 240px;
}

.toolbar .el-select {
  width: 160px;
}

.table-card {
  padding: 4px 0 18px;
  overflow: hidden;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px 6px;
}

.selected-tip {
  color: #6b7280;
  font-size: 12px;
}

.warn-tip {
  color: #c77700;
  font-size: 12px;
}

.thumb {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  background: #f4f6f8;
}

.thumb.small {
  width: 34px;
  height: 34px;
}

.thumb.empty {
  display: inline-grid;
  place-items: center;
  color: #9ba7b8;
  font-size: 11px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 20px 18px 0;
}

.danger {
  color: #d14343;
}

.form-tip {
  margin-left: 10px;
  color: #8a94a3;
  font-size: 12px;
}

.uploader-holder {
  display: grid;
  place-items: center;
  gap: 6px;
  width: 104px;
  height: 104px;
  color: #8a94a3;
  font-size: 12px;
  border: 1px dashed #d3d8e0;
  border-radius: 10px;
  cursor: pointer;
}

.uploader-image {
  width: 104px;
  height: 104px;
  object-fit: cover;
  border-radius: 10px;
}

.image-input {
  margin-top: 8px;
}

.dish-panel {
  width: 100%;
  padding: 14px;
  background: #f8fafb;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

.dish-panel-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.picker-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.picker-toolbar .el-input {
  width: 260px;
}

.picker-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}

.manual-add {
  margin-top: 14px;
}

.manual-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 700px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar {
    flex-wrap: wrap;
  }
}
</style>
