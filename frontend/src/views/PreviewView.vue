<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import WizardLayout from '../components/common/WizardLayout.vue'
import ErrorBanner from '../components/common/ErrorBanner.vue'
import { photoApi } from '../api/photoApi'
import { themeApi } from '../api/themeApi'
import { bgmApi } from '../api/bgmApi'
import { extractErrorMessage } from '../utils/errorMessage'
import { resolveApiUrl } from '../utils/resolveApiUrl'
import { useProjectStore } from '../stores/projectStore'
import { useVideoJobStore } from '../stores/videoJobStore'
import type { Photo } from '../types/photo'
import { WIZARD_TOTAL_STEPS } from '../constants/wizard'

const props = defineProps<{ projectId: string }>()
const router = useRouter()
const projectStore = useProjectStore()
const videoJobStore = useVideoJobStore()

const photos = ref<Photo[]>([])
const themeName = ref('')
const bgmName = ref('')
const isSubmitting = ref(false)
const errorMessage = ref('')

const project = computed(() => projectStore.current)
const previewPhotos = computed(() => photos.value.slice(0, 6))

onMounted(async () => {
  const [proj, photoList, themes, bgms] = await Promise.all([
    projectStore.ensureLoaded(props.projectId),
    photoApi.list(props.projectId),
    themeApi.list(),
    bgmApi.list(),
  ])
  photos.value = photoList
  themeName.value = themes.find((t) => t.code === proj.themeCode)?.name ?? ''
  bgmName.value = bgms.find((b) => b.code === proj.bgmCode)?.name ?? ''
})

async function handleNext() {
  isSubmitting.value = true
  errorMessage.value = ''
  try {
    await videoJobStore.start(props.projectId)
    router.push({ name: 'video-generating', params: { projectId: props.projectId } })
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, '動画生成の開始に失敗しました')
  } finally {
    isSubmitting.value = false
  }
}

function handleBack() {
  router.push({ name: 'duration-select', params: { projectId: props.projectId } })
}
</script>

<template>
  <WizardLayout
    :step="8"
    :total-steps="WIZARD_TOTAL_STEPS"
    title="内容を確認"
    :next-disabled="isSubmitting"
    next-label="この内容で動画を作成する"
    @back="handleBack"
    @next="handleNext"
  >
    <ErrorBanner v-if="errorMessage" :message="errorMessage" />
    <dl v-if="project" class="summary">
      <dt>タイトル</dt>
      <dd>{{ project.title }}</dd>
      <dt>テーマ</dt>
      <dd>{{ themeName }}</dd>
      <dt>BGM</dt>
      <dd>{{ bgmName }}</dd>
      <dt>スライド時間</dt>
      <dd>{{ project.slideDurationSec }}秒</dd>
      <dt>写真枚数</dt>
      <dd>{{ project.photoCount }}枚</dd>
    </dl>
    <div class="preview-grid">
      <img
        v-for="photo in previewPhotos"
        :key="photo.id"
        :src="resolveApiUrl(photo.fileUrl)"
        :alt="photo.originalFileName"
      />
    </div>
  </WizardLayout>
</template>

<style scoped>
.summary {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px 16px;
  margin-bottom: 24px;
}

.summary dt {
  color: var(--text-muted);
}

.summary dd {
  margin: 0;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 8px;
}

.preview-grid img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 6px;
}
</style>
