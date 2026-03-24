<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="page-grid">
    <div class="d-flex flex-wrap align-center justify-space-between ga-4">
      <div>
        <div class="text-h4 font-weight-bold text-primary">{{ title }}</div>
        <div v-if="description" class="text-body-1 text-medium-emphasis mt-2">
          {{ description }}
        </div>
      </div>
      <slot name="actions" />
    </div>

    <slot />
  </div>
</template>
