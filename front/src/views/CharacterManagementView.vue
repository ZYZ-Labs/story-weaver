<template>
  <v-card>
    <v-card-title>角色管理</v-card-title>
    <v-card-text>
      <v-chip-group>
        <v-chip v-for="character in characters" :key="character" color="secondary" class="mr-2">{{ character }}</v-chip>
      </v-chip-group>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
const characters = ['主角', '导师', '反派', '伙伴']
</script>
