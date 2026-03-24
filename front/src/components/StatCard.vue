<script setup lang="ts">
defineProps<{
  title: string
  value: string | number
  subtitle: string
  icon: string
  color?: string
}>()
</script>

<template>
  <v-card class="soft-panel">
    <v-card-text class="d-flex align-center justify-space-between">
      <div>
        <div class="text-overline text-medium-emphasis">{{ title }}</div>
        <div class="text-h4 font-weight-bold mt-2">{{ value }}</div>
        <div class="text-body-2 text-medium-emphasis mt-2">{{ subtitle }}</div>
      </div>
      <v-avatar size="54" :color="color || 'primary'" variant="tonal">
        <v-icon :icon="icon" size="28" />
      </v-avatar>
    </v-card-text>
  </v-card>
</template>
