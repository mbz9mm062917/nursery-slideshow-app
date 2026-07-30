<script setup lang="ts">
import { ref } from 'vue'
import { projectApi } from '../../api/projectApi'

const isSubmitting = ref(false)
const errorMessage = ref('')

const emit = defineEmits<{ created: [] }>()

async function handleCreate() {
  isSubmitting.value = true
  errorMessage.value = ''
  try {
    await projectApi.create()
    emit('created')
  } catch (error) {
    errorMessage.value = '作成に失敗しました'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div>
    <button :disabled="isSubmitting" @click="handleCreate">
      {{ isSubmitting ? '作成中...' : '新規プロジェクトを作成' }}
    </button>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
  </div>
</template>

<style scoped>
.error {
  color: #c0392b;
}
</style>
