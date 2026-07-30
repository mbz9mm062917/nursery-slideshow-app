<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import WizardLayout from '../components/common/WizardLayout.vue'
import ErrorBanner from '../components/common/ErrorBanner.vue'
import { extractErrorMessage } from '../utils/errorMessage'
import { useProjectStore } from '../stores/projectStore'

const props = defineProps<{ projectId: string }>()
const router = useRouter()
const projectStore = useProjectStore()

const title = ref('')
const errorMessage = ref('')
const isSubmitting = ref(false)
const TITLE_MAX_LENGTH = 50

onMounted(async () => {
  const project = await projectStore.ensureLoaded(props.projectId)
  title.value = project.title ?? ''
})

async function handleNext() {
  if (isSubmitting.value) {
    return
  }
  isSubmitting.value = true
  errorMessage.value = ''
  try {
    await projectStore.patchProject(props.projectId, { title: title.value })
    router.push({ name: 'theme-select', params: { projectId: props.projectId } })
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, 'タイトルの保存に失敗しました')
  } finally {
    isSubmitting.value = false
  }
}

function handleBack() {
  router.push({ name: 'photo-reorder', params: { projectId: props.projectId } })
}
</script>

<template>
  <WizardLayout
    :step="3"
    :total-steps="7"
    title="タイトルを入力"
    :next-disabled="!title.trim() || isSubmitting"
    @back="handleBack"
    @next="handleNext"
  >
    <ErrorBanner v-if="errorMessage" :message="errorMessage" />
    <input v-model="title" type="text" placeholder="例: さくら組 運動会" :maxlength="TITLE_MAX_LENGTH" />
    <p class="muted">{{ title.length }} / {{ TITLE_MAX_LENGTH }}文字</p>
  </WizardLayout>
</template>
