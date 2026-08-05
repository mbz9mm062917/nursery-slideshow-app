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
    <div class="home-blob home-blob-a" aria-hidden="true"></div>
    <div class="home-blob home-blob-b" aria-hidden="true"></div>
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
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
  overflow: hidden;
  padding: 20px;
}

.home-card {
  position: relative;
  z-index: 1;
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

.home-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(50px);
  opacity: 0.35;
}

.home-blob-a {
  width: 240px;
  height: 240px;
  background: var(--accent-yellow);
  top: -70px;
  right: -60px;
}

.home-blob-b {
  width: 180px;
  height: 180px;
  background: var(--accent-mint);
  bottom: -50px;
  left: -60px;
}
</style>
