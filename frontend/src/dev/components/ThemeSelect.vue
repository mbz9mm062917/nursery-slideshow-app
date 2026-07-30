<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { themeApi } from '../../api/themeApi'
import type { Theme } from '../../types/theme'

defineProps<{ modelValue: string | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const themes = ref<Theme[]>([])
const isLoading = ref(false)
const errorMessage = ref('')

async function loadThemes() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    themes.value = await themeApi.list()
  } catch (error) {
    errorMessage.value = 'テーマの取得に失敗しました'
  } finally {
    isLoading.value = false
  }
}

onMounted(loadThemes)
</script>

<template>
  <span>
    <select
      v-if="!isLoading && !errorMessage"
      :value="modelValue ?? ''"
      @change="emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
    >
      <option value="" disabled>テーマを選択</option>
      <option v-for="theme in themes" :key="theme.id" :value="theme.code">
        {{ theme.name }}
      </option>
    </select>
    <span v-else-if="isLoading">テーマ読み込み中...</span>
    <span v-else class="error">{{ errorMessage }}</span>
  </span>
</template>

<style scoped>
.error {
  color: #c0392b;
}
</style>
