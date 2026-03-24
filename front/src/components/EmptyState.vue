<script setup lang="ts">
defineProps<{
  title: string
  description: string
  icon?: string
}>()
</script>

<template>
  <v-sheet class="soft-panel rounded-xl pa-8 text-center">
    <v-icon :icon="icon || 'mdi-book-open-page-variant-outline'" size="42" color="primary" />
    <div class="text-h6 mt-4">{{ title }}</div>
    <div class="text-body-2 text-medium-emphasis mt-2">{{ description }}</div>
    <div class="mt-5">
      <slot />
    </div>
  </v-sheet>
</template>
