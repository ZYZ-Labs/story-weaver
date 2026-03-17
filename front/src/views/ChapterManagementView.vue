<template>
  <v-card>
    <v-card-title>章节管理</v-card-title>
    <v-card-text>
      <v-table>
        <thead>
          <tr>
            <th>章节号</th>
            <th>标题</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="chapter in chapters" :key="chapter.no">
            <td>{{ chapter.no }}</td>
            <td>{{ chapter.title }}</td>
            <td>{{ chapter.status }}</td>
          </tr>
        </tbody>
      </v-table>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
const chapters = [
  { no: 1, title: '开端', status: '草稿' },
  { no: 2, title: '冲突', status: '进行中' },
]
</script>
