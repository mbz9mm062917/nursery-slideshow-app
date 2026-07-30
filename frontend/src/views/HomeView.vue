<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '../stores/projectStore'

const router = useRouter()
const projectStore = useProjectStore()
const isCreating = ref(false)
const errorMessage = ref('')

async function handleCreate() {
  isCreating.value = true
  errorMessage.value = ''
  try {
    const project = await projectStore.createProject()
    router.push({ name: 'photo-upload', params: { projectId: project.id } })
  } catch (error) {
    errorMessage.value = '作成に失敗しました。時間をおいて再度お試しください。'
  } finally {
    isCreating.value = false
  }
}
</script>

<template>
  <main class="home">
    <h1>保育園スライドショー</h1>
    <button class="primary" :disabled="isCreating" @click="handleCreate">
      {{ isCreating ? '準備中...' : '＋ 新しいスライドショーを作る' }}
    </button>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
  </main>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: 16px;
  text-align: center;
}

.primary {
  font-size: 18px;
  padding: 16px 32px;
  border-radius: 10px;
  background: var(--accent);
  color: #fff;
  border: none;
}
</style>
