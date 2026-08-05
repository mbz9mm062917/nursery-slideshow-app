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

const LAYOUT_OPTIONS: Record<number, { value: string; label: string }[]> = {
  1: [
    { value: 'TILTED', label: '傾きあり' },
    { value: 'STRAIGHT', label: '傾きなし' },
  ],
  2: [
    { value: 'SIDE_BY_SIDE', label: '横に並べる' },
    { value: 'OFFSET', label: '上下に少しずらす' },
  ],
  3: [
    { value: 'SIDE_BY_SIDE', label: '横に並べる' },
    { value: 'ZIGZAG', label: '山谷に並べる' },
  ],
}

const CROP_SHAPE_OPTIONS = [
  { value: 'RECTANGLE', label: '四角' },
  { value: 'ROUNDED', label: '角丸' },
  { value: 'CIRCLE', label: '丸' },
  { value: 'OVAL', label: '楕円' },
]

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
  // ページ構成が変わると各ページの枚数が変わり、選択済みの並べ方が無効になりうるためリセットする
  localPhotos.value.forEach((p) => {
    p.layoutPattern = null
  })
  errorMessage.value = ''
}

function layoutOptionsFor(pageLength: number) {
  return LAYOUT_OPTIONS[pageLength] ?? []
}

function selectedLayoutPattern(page: Photo[]): string {
  const options = layoutOptionsFor(page.length)
  if (options.length === 0) {
    return ''
  }
  const lastPhoto = page[page.length - 1]
  return lastPhoto.layoutPattern ?? options[0].value
}

function selectLayoutPattern(page: Photo[], value: string) {
  page[page.length - 1].layoutPattern = value
}

function selectedCropShape(photo: Photo): string {
  return photo.cropShape ?? 'RECTANGLE'
}

function selectCropShape(photo: Photo, value: string) {
  photo.cropShape = value
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
    const layoutPatterns: Record<number, string> = {}
    const cropShapes: Record<number, string> = {}
    for (const photo of localPhotos.value) {
      if (photo.pageBreakAfter && photo.layoutPattern) {
        layoutPatterns[photo.id] = photo.layoutPattern
      }
      if (photo.cropShape) {
        cropShapes[photo.id] = photo.cropShape
      }
    }
    await Promise.all([
      photoApi.updatePageBreaks(props.projectId, pageBreakAfterPhotoIds, layoutPatterns),
      photoApi.updateCropShapes(props.projectId, cropShapes),
    ])
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
      「分ける」で別のページに分けられます。各ページで写真の並べ方、写真ごとにトリミング形状も選べます。
    </p>
    <div class="pages-flow">
      <template v-for="(page, pageIndex) in pages" :key="pageIndex">
        <div class="page-box">
          <p class="page-label">ページ{{ pageIndex + 1 }}({{ page.length }}枚)</p>
          <div class="page-photos">
            <template v-for="(photo, photoIndexInPage) in page" :key="photo.id">
              <div class="photo-cell">
                <img
                  class="page-thumb"
                  :class="'shape-' + selectedCropShape(photo).toLowerCase()"
                  :src="resolveApiUrl(photo.fileUrl)"
                  :alt="photo.originalFileName"
                />
                <div class="crop-shape-options">
                  <button
                    v-for="shape in CROP_SHAPE_OPTIONS"
                    :key="shape.value"
                    type="button"
                    class="crop-shape-option"
                    :class="{ selected: selectedCropShape(photo) === shape.value }"
                    @click="selectCropShape(photo, shape.value)"
                  >
                    {{ shape.label }}
                  </button>
                </div>
              </div>
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
          <div v-if="layoutOptionsFor(page.length).length > 0" class="layout-options">
            <button
              v-for="option in layoutOptionsFor(page.length)"
              :key="option.value"
              type="button"
              class="layout-option"
              :class="{ selected: selectedLayoutPattern(page) === option.value }"
              @click="selectLayoutPattern(page, option.value)"
            >
              {{ option.label }}
            </button>
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
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 14px;
}

.page-label {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
}

.page-photos {
  display: flex;
  align-items: center;
  gap: 8px;
}

.photo-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.page-thumb {
  width: 90px;
  height: 90px;
  object-fit: cover;
}

.page-thumb.shape-rectangle {
  border-radius: var(--radius-sm);
}

.page-thumb.shape-rounded {
  border-radius: var(--radius-md);
}

.page-thumb.shape-circle,
.page-thumb.shape-oval {
  border-radius: 50%;
}

.crop-shape-options {
  display: flex;
  gap: 4px;
}

.crop-shape-option {
  font: inherit;
  font-size: 10.5px;
  font-weight: 600;
  padding: 4px 9px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text-muted);
  white-space: nowrap;
}

.crop-shape-option.selected {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
  font-weight: 700;
}

.layout-options {
  display: flex;
  gap: 6px;
  margin-top: 10px;
}

.layout-option {
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  padding: 7px 14px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text-muted);
  white-space: nowrap;
}

.layout-option.selected {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
  font-weight: 700;
}

.split-button {
  font-size: 12px;
  padding: 6px 8px;
  border-radius: var(--radius-pill);
  border: 1px dashed var(--border);
  background: var(--surface);
  color: var(--text-muted);
  white-space: nowrap;
}

.merge-button {
  font-size: 12px;
  font-weight: 600;
  padding: 8px 12px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text);
  white-space: nowrap;
}

.merge-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
