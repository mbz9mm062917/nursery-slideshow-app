<script setup lang="ts">
import { ref } from 'vue'
import { projectApi } from '../../api/projectApi'
import { extractErrorMessage } from '../../utils/errorMessage'
import ThemeSelect from './ThemeSelect.vue'
import type { Project } from '../../types/project'

const props = defineProps<{ project: Project }>()
const emit = defineEmits<{ updated: [] }>()

const title = ref(props.project.title ?? '')
const themeCode = ref(props.project.themeCode)
const isSubmitting = ref(false)
const errorMessage = ref('')

async function handleSave() {
  isSubmitting.value = true
  errorMessage.value = ''
  try {
    await projectApi.patch(props.project.id, {
      title: title.value || undefined,
      themeCode: themeCode.value ?? undefined,
    })
    emit('updated')
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, '更新に失敗しました')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <form @submit.prevent="handleSave">
    <input v-model="title" type="text" placeholder="タイトル" />
    <ThemeSelect v-model="themeCode" />
    <button :disabled="isSubmitting" type="submit">保存</button>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
  </form>
</template>

<style scoped>
.error {
  color: #c0392b;
}
</style>
