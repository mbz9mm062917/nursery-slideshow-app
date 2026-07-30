<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useVideoJobStore } from '../stores/videoJobStore'
import { resolveApiUrl } from '../utils/resolveApiUrl'

defineProps<{ projectId: string }>()
const router = useRouter()
const videoJobStore = useVideoJobStore()

const downloadUrl = computed(() => {
  const path = videoJobStore.current?.downloadUrl
  return path ? resolveApiUrl(path) : null
})

function handleCreateNew() {
  router.push({ name: 'home' })
}
</script>

<template>
  <div class="download">
    <h1>動画が完成しました</h1>
    <video v-if="downloadUrl" :src="downloadUrl" controls></video>
    <a v-if="downloadUrl" class="primary" :href="downloadUrl" download>ダウンロード</a>
    <button type="button" @click="handleCreateNew">新しいスライドショーを作る</button>
  </div>
</template>

<style scoped>
.download {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  gap: 16px;
  text-align: center;
  padding: 20px;
}

video {
  max-width: 100%;
  width: 480px;
  border-radius: 8px;
}

.primary {
  display: inline-block;
  font-size: 18px;
  padding: 14px 32px;
  border-radius: 10px;
  background: var(--accent);
  color: #fff;
  text-decoration: none;
}
</style>
