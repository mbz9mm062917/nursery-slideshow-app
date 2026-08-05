<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ErrorBanner from '../components/common/ErrorBanner.vue'
import { useVideoJobStore } from '../stores/videoJobStore'
import { extractErrorMessage } from '../utils/errorMessage'
import type { VideoJob } from '../types/videoJob'

const POLL_INTERVAL_MS = 2000

const props = defineProps<{ projectId: string }>()
const router = useRouter()
const videoJobStore = useVideoJobStore()

const errorMessage = ref('')
let pollTimeoutId: ReturnType<typeof setTimeout> | null = null
let isPollingStopped = false

function stopPolling() {
  isPollingStopped = true
  if (pollTimeoutId !== null) {
    clearTimeout(pollTimeoutId)
    pollTimeoutId = null
  }
}

async function poll() {
  let job: VideoJob | null
  try {
    job = await videoJobStore.refreshStatus()
  } catch (error) {
    if (isPollingStopped) {
      return
    }
    stopPolling()
    errorMessage.value = '状態の取得に失敗しました'
    return
  }

  // 画面離脱等でポーリングが停止済みの場合、遅れて届いたレスポンスは無視する
  if (isPollingStopped) {
    return
  }

  if (!job) {
    errorMessage.value = '生成中のジョブが見つかりません。プレビュー画面からやり直してください。'
    stopPolling()
    return
  }

  if (job.status === 'COMPLETED') {
    stopPolling()
    router.push({ name: 'download', params: { projectId: props.projectId } })
    return
  }

  if (job.status === 'FAILED') {
    stopPolling()
    errorMessage.value = job.errorMessage ?? '動画の生成に失敗しました'
    return
  }

  // 前回のリクエストが完了してから次回ポーリングをスケジュールする(setIntervalによる多重実行を避ける)
  pollTimeoutId = setTimeout(poll, POLL_INTERVAL_MS)
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
})

onUnmounted(stopPolling)
</script>

<template>
  <div class="generating">
    <template v-if="errorMessage">
      <ErrorBanner :message="errorMessage" />
      <button type="button" class="btn-pill" @click="handleRetry">もう一度作成する</button>
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
  gap: 18px;
  text-align: center;
  padding: 20px;
}

.progress-bar {
  width: 320px;
  max-width: 100%;
  height: 10px;
  border-radius: var(--radius-pill);
  background: var(--border);
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent), var(--accent-mint));
  border-radius: var(--radius-pill);
  transition: width 0.3s ease;
}
</style>
