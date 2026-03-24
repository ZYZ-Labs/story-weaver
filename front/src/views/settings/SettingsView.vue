<script setup lang="ts">
import PageContainer from '@/components/PageContainer.vue'
</script>

<template>
  <PageContainer
    title="系统设置"
    description="这里先承接用户中心、Prompt 模板与项目参数设置的入口，保持主导航闭环。"
  >
    <v-card class="soft-panel">
      <v-list lines="two">
        <v-list-item title="用户中心" subtitle="头像、昵称、邮箱与登录信息。" />
        <v-list-item title="Prompt 模板" subtitle="后续接入系统级、项目级与任务级 Prompt 管理。" />
        <v-list-item title="项目参数" subtitle="默认模型、RAG 开关与写作策略的统一配置。" />
      </v-list>
    </v-card>
  </PageContainer>
</template>
