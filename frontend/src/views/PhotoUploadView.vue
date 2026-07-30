<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import WizardLayout from '../components/common/WizardLayout.vue'
import ErrorBanner from '../components/common/ErrorBanner.vue'
import PhotoDropzone from '../components/wizard/PhotoDropzone.vue'
import PhotoThumbnailGrid from '../components/wizard/PhotoThumbnailGrid.vue'
import { photoApi } from '../api/photoApi'
import type { Photo } from '../types/photo'

const props = defineProps<{ projectId: string }>()
const router = useRouter()

const photos = ref<Photo[]>([])
const errorMessage = ref('')
const ALLOWED_EXTENSIONS = ['jpg', 'jpeg', 'png']

const canGoNext = computed(() => photos.value.length > 0)

function extensionOf(fileName: string): string {
  const dotIndex = fileName.lastIndexOf('.')
  return dotIndex >= 0 ? fileName.slice(dotIndex + 1).toLowerCase() : ''
}

async function loadPhotos() {
  photos.value = await photoApi.list(props.projectId)
}

async function handleFilesSelected(files: File[]) {
  errorMessage.value = ''
  const validFiles = files.filter((file) => ALLOWED_EXTENSIONS.includes(extensionOf(file.name)))
  const rejectedFiles = files.filter((file) => !ALLOWED_EXTENSIONS.includes(extensionOf(file.name)))

  if (rejectedFiles.length > 0) {
    errorMessage.value = `対応していない形式のため取り込めませんでした(jpg, jpeg, pngのみ): ${rejectedFiles.map((f) => f.name).join(', ')}`
  }
  if (validFiles.length === 0) {
    return
  }

  try {
    await photoApi.upload(props.projectId, validFiles)
    await loadPhotos()
  } catch (error) {
    errorMessage.value = 'アップロードに失敗しました'
  }
}

async function handleDelete(photoId: number) {
  try {
    await photoApi.remove(photoId)
    await loadPhotos()
  } catch (error) {
    errorMessage.value = '削除に失敗しました'
  }
}

function handleNext() {
  router.push({ name: 'photo-reorder', params: { projectId: props.projectId } })
}

function handleBack() {
  router.push({ name: 'home' })
}

onMounted(loadPhotos)
</script>

<template>
  <WizardLayout
    :step="1"
    :total-steps="7"
    title="写真をアップロード"
    :next-disabled="!canGoNext"
    @back="handleBack"
    @next="handleNext"
  >
    <ErrorBanner v-if="errorMessage" :message="errorMessage" />
    <PhotoDropzone @files-selected="handleFilesSelected" />
    <p class="muted">{{ photos.length }}枚選択中</p>
    <PhotoThumbnailGrid :photos="photos" @delete="handleDelete" />
  </WizardLayout>
</template>
