<script setup lang="ts">
import PageContainer from '@/components/PageContainer.vue'
</script>

<template>
  <PageContainer
    title="系统状态"
    description="这里预留操作日志、AI 任务轨迹和健康检查入口，方便后续接入更完整的运维与审计能力。"
  >
    <v-card class="soft-panel">
      <v-table>
        <thead>
          <tr>
            <th>模块</th>
            <th>动作</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>认证</td>
            <td>登录 / 注册</td>
            <td>已接入</td>
          </tr>
          <tr>
            <td>项目</td>
            <td>创建 / 编辑 / 删除</td>
            <td>已接入</td>
          </tr>
          <tr>
            <td>AI 写作</td>
            <td>生成 / 采纳 / 拒绝</td>
            <td>已接入</td>
          </tr>
          <tr>
            <td>知识库</td>
            <td>录入 / 检索 / 重建索引</td>
            <td>已接入</td>
          </tr>
        </tbody>
      </v-table>
    </v-card>
  </PageContainer>
</template>
