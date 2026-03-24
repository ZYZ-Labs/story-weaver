<script setup lang="ts">
defineProps<{
  modelValue: boolean
  title: string
  suggestions: string[]
  loading?: boolean
  sourceLabel?: string
  emptyText?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  refresh: []
  select: [string]
}>()
</script>

<template>
  <v-dialog
    :model-value="modelValue"
    max-width="680"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <v-card>
      <v-card-title>{{ title }}</v-card-title>
      <v-card-text class="pt-3">
        <v-alert v-if="sourceLabel" type="info" variant="tonal" class="mb-4">
          {{ sourceLabel }}
        </v-alert>

        <div v-if="loading" class="py-8 text-center text-medium-emphasis">
          正在生成候选名称...
        </div>

        <v-list v-else-if="suggestions.length" lines="two">
          <v-list-item
            v-for="item in suggestions"
            :key="item"
            :title="item"
            subtitle="点击即可填入表单"
            @click="emit('select', item)"
          />
        </v-list>

        <div v-else class="py-8 text-center text-medium-emphasis">
          {{ emptyText || '暂时没有可用候选，请重新生成。' }}
        </div>
      </v-card-text>
      <v-card-actions class="justify-space-between">
        <v-btn variant="text" @click="emit('update:modelValue', false)">关闭</v-btn>
        <v-btn color="primary" :loading="loading" @click="emit('refresh')">重新生成</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
