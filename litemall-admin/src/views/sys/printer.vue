<template>
  <div class="app-container">

    <!-- 查询和其他操作 -->
    <div class="filter-container">
      <el-input v-model="listQuery.sn" clearable class="filter-item" style="width: 200px;" :placeholder="$t('sys_printer.placeholder.filter_sn')" />
      <el-input v-model="listQuery.name" clearable class="filter-item" style="width: 200px;" :placeholder="$t('sys_printer.placeholder.filter_name')" />
      <el-button v-permission="['GET /admin/printer/list']" class="filter-item" type="primary" icon="el-icon-search" @click="handleFilter">{{ $t('app.button.search') }}</el-button>
      <el-button v-permission="['POST /admin/printer/create']" class="filter-item" type="primary" icon="el-icon-edit" @click="handleCreate">{{ $t('app.button.create') }}</el-button>
    </div>

    <!-- 查询结果 -->
    <el-table v-loading="listLoading" :data="list" :element-loading-text="$t('app.message.list_loading')" border fit highlight-current-row>

      <el-table-column align="center" label="ID" prop="id" sortable width="80" />

      <el-table-column align="center" :label="$t('sys_printer.table.sn')" prop="sn" />

      <el-table-column align="center" :label="$t('sys_printer.form.pkey')" prop="pkey" />

      <el-table-column align="center" :label="$t('sys_printer.table.name')" prop="name" />

      <el-table-column align="center" :label="$t('sys_printer.table.online')" prop="online">
        <template slot-scope="scope">
          <el-tag :type="scope.row.online === 1 ? 'success' : 'danger'">
            {{ scope.row.online === 1 ? '在线' : '离线' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column align="center" :label="$t('sys_printer.table.status')" prop="status">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 0 ? 'success' : 'danger'">
            {{ scope.row.status === 0 ? '正常' : '异常' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column align="center" label="添加时间" prop="addTime" width="160" />

      <el-table-column align="center" label="更新时间" prop="updateTime" width="160" />

      <el-table-column align="center" :label="$t('sys_printer.table.actions')" width="420" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button v-permission="['POST /admin/printer/print-test']" type="warning" size="mini" @click="handlePrintTest(scope.row)">打印测试</el-button>
          <el-button v-permission="['POST /admin/printer/print']" type="success" size="mini" @click="handlePrint(scope.row)">打印</el-button>
          <el-button v-permission="['POST /admin/printer/clean-queue']" type="info" size="mini" @click="handleCleanQueue(scope.row)">清空</el-button>
          <el-button v-permission="['POST /admin/printer/update']" type="primary" size="mini" @click="handleUpdate(scope.row)">{{ $t('app.button.edit') }}</el-button>
          <el-button v-permission="['POST /admin/printer/delete']" type="danger" size="mini" @click="handleDelete(scope.row)">{{ $t('app.button.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="listQuery.page" :limit.sync="listQuery.limit" @pagination="getList" />

    <!-- 添加或修改对话框 -->
    <el-dialog :visible.sync="dialogFormVisible" :title="textMap[dialogStatus]">
      <el-form ref="dataForm" :rules="rules" :model="dataForm" status-icon label-position="left" label-width="120px" style="width: 400px; margin-left:50px;">
        <el-form-item :label="$t('sys_printer.form.sn')" prop="sn">
          <el-input v-model="dataForm.sn" :disabled="dialogStatus === 'update'" placeholder="请输入打印机编号" />
        </el-form-item>
        <el-form-item v-if="dialogStatus === 'create'" :label="$t('sys_printer.form.pkey')" prop="pkey">
          <el-input v-model="dataForm.pkey" placeholder="请输入打印机密钥Key" />
        </el-form-item>
        <el-form-item :label="$t('sys_printer.form.name')" prop="name">
          <el-input v-model="dataForm.name" placeholder="请输入自定义名称" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">{{ $t('app.button.cancel') }}</el-button>
        <el-button v-if="dialogStatus=='create'" type="primary" @click="createData">{{ $t('app.button.confirm') }}</el-button>
        <el-button v-else type="primary" @click="updateData">{{ $t('app.button.confirm') }}</el-button>
      </div>
    </el-dialog>

    <!-- 自定义打印内容对话框 -->
    <el-dialog :visible.sync="printDialogVisible" title="自定义打印内容">
      <el-form ref="printForm" :rules="printRules" :model="printForm" status-icon label-position="left" label-width="120px" style="width: 500px; margin-left:50px;">
        <el-form-item label="打印次数" prop="times">
          <el-input-number v-model="printForm.times" :min="1" :max="5" label="打印次数" />
        </el-form-item>
        <el-form-item label="打印内容" prop="content">
          <el-input v-model="printForm.content" type="textarea" :rows="15" placeholder="请输入排版标记内容" style="width: 100%; font-family: monospace;" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="printDialogVisible = false">{{ $t('app.button.cancel') }}</el-button>
        <el-button type="primary" @click="submitPrint">立即提交</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import { listPrinter, createPrinter, updatePrinter, deletePrinter, printTest, print, cleanQueue } from '@/api/printer'
import Pagination from '@/components/Pagination' // Secondary package based on el-pagination

export default {
  name: 'Printer',
  components: { Pagination },
  data() {
    return {
      list: null,
      total: 0,
      listLoading: true,
      listQuery: {
        page: 1,
        limit: 20,
        sn: undefined,
        name: undefined,
        sort: 'add_time',
        order: 'desc'
      },
      dialogFormVisible: false,
      dialogStatus: '',
      textMap: {
        update: '编辑打印机设备',
        create: '添加打印机设备'
      },
      dataForm: {
        id: undefined,
        sn: '',
        pkey: '',
        name: ''
      },
      rules: {
        sn: [{ required: true, message: '设备编号不能为空', trigger: 'blur' }],
        pkey: [{ required: true, message: '设备密钥不能为空', trigger: 'blur' }],
        name: [{ required: true, message: '设备名称不能为空', trigger: 'blur' }]
      },
      printDialogVisible: false,
      printForm: {
        id: undefined,
        times: 1,
        content: ''
      },
      printRules: {
        content: [{ required: true, message: '打印内容不能为空', trigger: 'blur' }]
      },
      defaultTemplate: `<C><L2>小满食光</L2></C>
<C>-- 结账单 --</C>
<BR>
下单时间：2026-07-17 23:15:00
订单编号：XM1234567890
<BR>
--------------------------------
商品名称      数量     单价
肉松沙拉三明治  1      ¥18.00
美式咖啡(冰)   1      ¥15.00
--------------------------------
合计：¥33.00
<BR>
备注：三明治不加洋葱，咖啡少冰。
<BR>
<C><QRCODE>https://www.litemall.com</QRCODE></C>
<C>扫描二维码关注我们</C>
<BR>
<BR>
<CUT>
`
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.listLoading = true
      listPrinter(this.listQuery).then(response => {
        this.list = response.data.data.list
        this.total = response.data.data.total
        this.listLoading = false
      }).catch(() => {
        this.list = []
        this.total = 0
        this.listLoading = false
      })
    },
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    resetForm() {
      this.dataForm = {
        id: undefined,
        sn: '',
        pkey: '',
        name: ''
      }
    },
    handleCreate() {
      this.resetForm()
      this.dialogStatus = 'create'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    createData() {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          createPrinter(this.dataForm).then(response => {
            this.list.unshift(response.data.data)
            this.dialogFormVisible = false
            this.$notify.success({
              title: '成功',
              message: '添加打印机成功'
            })
            this.getList() // Refresh list to get latest sync status
          }).catch(response => {
            this.$notify.error({
              title: '添加失败',
              message: response.data.errmsg || '操作失败'
            })
          })
        }
      })
    },
    handleUpdate(row) {
      this.dataForm = Object.assign({}, row)
      this.dialogStatus = 'update'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    updateData() {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          updatePrinter(this.dataForm).then(() => {
            for (const v of this.list) {
              if (v.id === this.dataForm.id) {
                const index = this.list.indexOf(v)
                this.list.splice(index, 1, this.dataForm)
                break
              }
            }
            this.dialogFormVisible = false
            this.$notify.success({
              title: '成功',
              message: '更新打印机成功'
            })
            this.getList() // Refresh list to get latest sync status
          }).catch(response => {
            this.$notify.error({
              title: '更新失败',
              message: response.data.errmsg || '操作失败'
            })
          })
        }
      })
    },
    handleDelete(row) {
      this.$confirm('此操作将永久删除该打印机设备, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deletePrinter(row).then(() => {
          this.$notify.success({
            title: '成功',
            message: '删除成功'
          })
          this.getList()
        }).catch(response => {
          this.$notify.error({
            title: '删除失败',
            message: response.data.errmsg || '操作失败'
          })
        })
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消删除'
        })
      })
    },
    handlePrintTest(row) {
      this.listLoading = true
      printTest(row).then(response => {
        this.listLoading = false
        this.$message({
          type: 'success',
          message: response.data.data || '测试打印指令已下发'
        })
      }).catch(response => {
        this.listLoading = false
        this.$message.error(response.data.errmsg || '测试打印发送失败')
      })
    },
    handlePrint(row) {
      this.printForm = {
        id: row.id,
        times: 1,
        content: this.defaultTemplate
      }
      this.printDialogVisible = true
      this.$nextTick(() => {
        this.$refs['printForm'].clearValidate()
      })
    },
    submitPrint() {
      this.$refs['printForm'].validate((valid) => {
        if (valid) {
          this.listLoading = true
          print(this.printForm).then(response => {
            this.listLoading = false
            this.printDialogVisible = false
            this.$message({
              type: 'success',
              message: response.data.data || '打印指令发送成功'
            })
          }).catch(response => {
            this.listLoading = false
            this.$message.error(response.data.errmsg || '打印发送失败')
          })
        }
      })
    },
    handleCleanQueue(row) {
      this.$confirm('确定要清空该打印机的所有待打印订单吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.listLoading = true
        cleanQueue(row).then(response => {
          this.listLoading = false
          this.$message({
            type: 'success',
            message: `清空待打印订单成功，共清空 ${response.data.data} 个订单`
          })
        }).catch(response => {
          this.listLoading = false
          this.$message.error(response.data.errmsg || '清空失败')
        })
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消清空'
        })
      })
    }
  }
}
</script>

<style scoped>
</style>
