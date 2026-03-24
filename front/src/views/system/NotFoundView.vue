<template>
  <v-container class="fill-height d-flex align-center justify-center">
    <v-card max-width="520" class="soft-panel text-center">
      <v-card-text class="pa-8">
        <div class="text-overline text-secondary">404</div>
        <div class="text-h4 font-weight-bold mt-2">页面不存在</div>
        <div class="text-body-2 text-medium-emphasis mt-4">
          这个地址没有对应页面，可以返回总览继续工作。
        </div>
        <v-btn class="mt-6" color="primary" to="/dashboard">返回总览</v-btn>
      </v-card-text>
    </v-card>
  </v-container>
</template>
