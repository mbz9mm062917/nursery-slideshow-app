<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import WizardLayout from '../components/common/WizardLayout.vue'
import ErrorBanner from '../components/common/ErrorBanner.vue'
import { photoApi } from '../api/photoApi'
import { extractErrorMessage } from '../utils/errorMessage'
import { resolveApiUrl } from '../utils/resolveApiUrl'
import { WIZARD_TOTAL_STEPS } from '../constants/wizard'
import type { Photo } from '../types/photo'

const MAX_PHOTOS_PER_PAGE = 3

const props = defineProps<{ projectId: string }>()
const router = useRouter()

const localPhotos = ref<Photo[]>([])
const errorMessage = ref('')
const isSubmitting = ref(false)
const canGoNext = computed(() => localPhotos.value.length > 0)

const pages = computed(() => {
  const result: Photo[][] = []
  let current: Photo[] = []
  for (const photo of localPhotos.value) {
    current.push(photo)
    if (photo.pageBreakAfter) {
      result.push(current)
      current = []
    }
  }
  if (current.length > 0) {
    result.push(current)
  }
  return result
})

async function loadPhotos() {
  localPhotos.value = await photoApi.list(props.projectId)
}

function groupStartIndex(index: number): number {
  let i = index
  while (i > 0 && !localPhotos.value[i - 1].pageBreakAfter) {
    i--
  }
  return i
}

function groupEndIndex(index: number): number {
  let i = index
  while (i < localPhotos.value.length - 1 && !localPhotos.value[i].pageBreakAfter) {
    i++
  }
  return i
}

function canMergeAt(index: number): boolean {
  const start = groupStartIndex(index)
  const end = groupEndIndex(index + 1)
  return end - start + 1 <= MAX_PHOTOS_PER_PAGE
}

function toggleDivider(index: number) {
  const photo = localPhotos.value[index]
  if (photo.pageBreakAfter) {
    // 現在は別ページ → つなげる(上限チェックのうえ同じページにまとめる)
    if (!canMergeAt(index)) {
      errorMessage.value = `1ページに配置できる写真は${MAX_PHOTOS_PER_PAGE}枚までです`
      return
    }
    photo.pageBreakAfter = false
  } else {
    // 現在は同じページ → 分ける
    photo.pageBreakAfter = true
  }
  errorMessage.value = ''
}

function globalIndexOf(photo: Photo): number {
  return localPhotos.value.findIndex((p) => p.id === photo.id)
}

async function handleNext() {
  if (isSubmitting.value) {
    return
  }
  isSubmitting.value = true
  errorMessage.value = ''
  try {
    const pageBreakAfterPhotoIds = localPhotos.value.filter((p) => p.pageBreakAfter).map((p) => p.id)
    await photoApi.updatePageBreaks(props.projectId, pageBreakAfterPhotoIds)
    router.push({ name: 'title-input', params: { projectId: props.projectId } })
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, 'ページ構成の保存に失敗しました')
  } finally {
    isSubmitting.value = false
  }
}

function handleBack() {
  router.push({ name: 'photo-reorder', params: { projectId: props.projectId } })
}

onMounted(loadPhotos)
</script>

<template>
  <WizardLayout
    :step="3"
    :total-steps="WIZARD_TOTAL_STEPS"
    title="ページ構成を決める"
    :next-disabled="!canGoNext || isSubmitting"
    @back="handleBack"
    @next="handleNext"
  >
    <ErrorBanner v-if="errorMessage" :message="errorMessage" />
    <p class="muted">
      1ページに入れる写真をまとめられます(最大{{ MAX_PHOTOS_PER_PAGE }}枚まで)。「つなげる」で同じページに、
      「分ける」で別のページに分けられます。
    </p>
    <div class="pages-flow">
      <template v-for="(page, pageIndex) in pages" :key="pageIndex">
        <div class="page-box">
          <p class="page-label">ページ{{ pageIndex + 1 }}({{ page.length }}枚)</p>
          <div class="page-photos">
            <template v-for="(photo, photoIndexInPage) in page" :key="photo.id">
              <img class="page-thumb" :src="resolveApiUrl(photo.fileUrl)" :alt="photo.originalFileName" />
              <button
                v-if="photoIndexInPage < page.length - 1"
                type="button"
                class="split-button"
                @click="toggleDivider(globalIndexOf(photo))"
              >
                ✂ 分ける
              </button>
            </template>
          </div>
        </div>
        <button
          v-if="pageIndex < pages.length - 1"
          type="button"
          class="merge-button"
          :disabled="!canMergeAt(globalIndexOf(page[page.length - 1]))"
          @click="toggleDivider(globalIndexOf(page[page.length - 1]))"
        >
          + つなげる
        </button>
      </template>
    </div>
  </WizardLayout>
</template>

<style scoped>
.pages-flow {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.page-box {
  border: 2px solid var(--accent);
  border-radius: 10px;
  padding: 10px;
}

.page-label {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.page-photos {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-thumb {
  width: 90px;
  height: 90px;
  object-fit: cover;
  border-radius: 6px;
}

.split-button {
  font-size: 12px;
  padding: 6px 8px;
  border-radius: 8px;
  border: 1px dashed var(--border);
  background: var(--bg);
  color: var(--text-muted);
  white-space: nowrap;
}

.merge-button {
  font-size: 12px;
  padding: 8px 10px;
  border-radius: 20px;
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--text);
  white-space: nowrap;
}

.merge-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
