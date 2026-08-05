<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ErrorBanner from '../components/common/ErrorBanner.vue'
import { useVideoJobStore } from '../stores/videoJobStore'
import { resolveApiUrl } from '../utils/resolveApiUrl'
import { extractErrorMessage } from '../utils/errorMessage'

const props = defineProps<{ projectId: string }>()
const router = useRouter()
const videoJobStore = useVideoJobStore()

const errorMessage = ref('')

const downloadUrl = computed(() => {
  const path = videoJobStore.current?.downloadUrl
  return path ? resolveApiUrl(path) : null
})

function handleCreateNew() {
  router.push({ name: 'home' })
}

onMounted(async () => {
  if (videoJobStore.current) {
    return
  }
  try {
    const job = await videoJobStore.restoreLatest(props.projectId)
    if (job.status !== 'COMPLETED') {
      router.push({ name: 'video-generating', params: { projectId: props.projectId } })
    }
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, '動画生成ジョブが見つかりません。')
  }
})
</script>

<template>
  <div class="download">
    <template v-if="errorMessage">
      <ErrorBanner :message="errorMessage" />
      <button type="button" @click="handleCreateNew">新しいスライドショーを作る</button>
    </template>
    <template v-else>
      <h1>動画が完成しました</h1>
      <div v-if="downloadUrl" class="video-card card">
        <video :src="downloadUrl" controls></video>
        <a class="btn-pill" :href="downloadUrl" download>ダウンロード</a>
      </div>
      <button type="button" class="btn-pill ghost" @click="handleCreateNew">新しいスライドショーを作る</button>
    </template>
  </div>
</template>

<style scoped>
.download {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  gap: 18px;
  text-align: center;
  padding: 20px;
}

.video-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}

video {
  max-width: 100%;
  width: 480px;
  border-radius: var(--radius-md);
}

.btn-pill {
  font-size: 16px;
}
</style>
