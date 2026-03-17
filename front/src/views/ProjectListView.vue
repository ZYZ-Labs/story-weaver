<template>
  <v-card>
    <v-card-title>项目列表</v-card-title>
    <v-card-text>
      <v-list>
        <v-list-item v-for="project in projects" :key="project.id" :title="project.name" :subtitle="project.description" />
      </v-list>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
const projects = [
  { id: 1, name: '长篇小说 A', description: '奇幻冒险' },
  { id: 2, name: '短篇小说 B', description: '科幻悬疑' },
]
</script>
