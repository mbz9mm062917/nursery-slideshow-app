<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ErrorBanner from '../components/common/ErrorBanner.vue'
import { useVideoJobStore } from '../stores/videoJobStore'
import { extractErrorMessage } from '../utils/errorMessage'

const props = defineProps<{ projectId: string }>()
const router = useRouter()
const videoJobStore = useVideoJobStore()

const errorMessage = ref('')
let pollTimer: ReturnType<typeof setInterval> | null = null

function stopPolling() {
  if (pollTimer !== null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function poll() {
  try {
    const job = await videoJobStore.refreshStatus()
    if (!job) {
      errorMessage.value = '生成中のジョブが見つかりません。プレビュー画面からやり直してください。'
      stopPolling()
      return
    }
    if (job.status === 'COMPLETED') {
      stopPolling()
      router.push({ name: 'download', params: { projectId: props.projectId } })
    } else if (job.status === 'FAILED') {
      stopPolling()
      errorMessage.value = job.errorMessage ?? '動画の生成に失敗しました'
    }
  } catch (error) {
    stopPolling()
    errorMessage.value = '状態の取得に失敗しました'
  }
}

function handleRetry() {
  router.push({ name: 'preview', params: { projectId: props.projectId } })
}

onMounted(async () => {
  if (!videoJobStore.current) {
    try {
      await videoJobStore.restoreLatest(props.projectId)
    } catch (error) {
      errorMessage.value = extractErrorMessage(error, '動画生成ジョブが見つかりません。')
      return
    }
  }
  poll()
  pollTimer = setInterval(poll, 2000)
})

onUnmounted(stopPolling)
</script>

<template>
  <div class="generating">
    <template v-if="errorMessage">
      <ErrorBanner :message="errorMessage" />
      <button type="button" @click="handleRetry">もう一度作成する</button>
    </template>
    <template v-else>
      <h1>動画を作成しています...</h1>
      <div class="progress-bar">
        <div class="progress-bar-fill" :style="{ width: (videoJobStore.current?.progress ?? 0) + '%' }" />
      </div>
      <p class="muted">{{ videoJobStore.current?.progress ?? 0 }}%</p>
    </template>
  </div>
</template>

<style scoped>
.generating {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  gap: 16px;
  text-align: center;
  padding: 20px;
}

.progress-bar {
  width: 320px;
  max-width: 100%;
  height: 12px;
  border-radius: 6px;
  background: var(--border);
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: var(--accent);
  transition: width 0.3s ease;
}
</style>
