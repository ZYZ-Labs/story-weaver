<script setup lang="ts">
defineProps<{
  modelValue: boolean
  title: string
  text: string
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  confirm: []
}>()
</script>

<template>
  <v-dialog
    :model-value="modelValue"
    max-width="420"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <v-card>
      <v-card-title>{{ title }}</v-card-title>
      <v-card-text>{{ text }}</v-card-text>
      <v-card-actions class="justify-end">
        <v-btn variant="text" @click="emit('update:modelValue', false)">取消</v-btn>
        <v-btn color="error" :loading="loading" @click="emit('confirm')">确认</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
