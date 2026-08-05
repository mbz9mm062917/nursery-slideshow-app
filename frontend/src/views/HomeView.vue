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
    <div class="home-card card">
      <h1>保育園スライドショー</h1>
      <button class="btn-pill" :disabled="isCreating" @click="handleCreate">
        {{ isCreating ? '準備中...' : '＋ 新しいスライドショーを作る' }}
      </button>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </div>
  </main>
</template>

<style scoped>
.home {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
  padding: 20px;
}

.home-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 44px 36px;
  text-align: center;
}

.home-card h1 {
  margin: 0;
  font-size: 26px;
}

.home-card .btn-pill {
  font-size: 16px;
}
</style>
