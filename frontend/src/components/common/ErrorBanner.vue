<script setup lang="ts">
defineProps<{ message: string }>()
</script>

<template>
  <p class="error-banner">{{ message }}</p>
</template>

<style scoped>
.error-banner {
  background: #fdecea;
  color: var(--error);
  border: 1px solid var(--error);
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 16px;
}
</style>
