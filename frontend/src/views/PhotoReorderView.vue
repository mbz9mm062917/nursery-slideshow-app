<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import WizardLayout from '../components/common/WizardLayout.vue'
import ErrorBanner from '../components/common/ErrorBanner.vue'
import PhotoSorter from '../components/wizard/PhotoSorter.vue'
import { photoApi } from '../api/photoApi'
import type { Photo } from '../types/photo'

const props = defineProps<{ projectId: string }>()
const router = useRouter()

const photos = ref<Photo[]>([])
const errorMessage = ref('')
const canGoNext = computed(() => photos.value.length > 0)

async function loadPhotos() {
  photos.value = await photoApi.list(props.projectId)
}

async function handleReorder(photoIds: number[]) {
  try {
    photos.value = await photoApi.reorder(props.projectId, photoIds)
  } catch (error) {
    errorMessage.value = '並び替えの保存に失敗しました'
    await loadPhotos()
  }
}

function handleNext() {
  router.push({ name: 'title-input', params: { projectId: props.projectId } })
}

function handleBack() {
  router.push({ name: 'photo-upload', params: { projectId: props.projectId } })
}

onMounted(loadPhotos)
</script>

<template>
  <WizardLayout
    :step="2"
    :total-steps="7"
    title="写真を並び替え"
    :next-disabled="!canGoNext"
    @back="handleBack"
    @next="handleNext"
  >
    <ErrorBanner v-if="errorMessage" :message="errorMessage" />
    <PhotoSorter :photos="photos" @reorder="handleReorder" />
  </WizardLayout>
</template>
