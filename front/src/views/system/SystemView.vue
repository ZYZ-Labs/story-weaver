<script setup lang="ts">
import PageContainer from '@/components/PageContainer.vue'
</script>

<template>
  <PageContainer
    title="系统日志"
    description="预留操作日志、AI 任务轨迹和健康检查入口，方便后续接后端日志模块。"
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
            <td>登录</td>
            <td>已接入</td>
          </tr>
          <tr>
            <td>项目</td>
            <td>CRUD</td>
            <td>已接入</td>
          </tr>
          <tr>
            <td>AI 写作</td>
            <td>生成 / 采纳 / 拒绝</td>
            <td>已接入</td>
          </tr>
        </tbody>
      </v-table>
    </v-card>
  </PageContainer>
</template>
