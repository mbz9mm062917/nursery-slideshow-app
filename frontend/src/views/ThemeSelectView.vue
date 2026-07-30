<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import WizardLayout from '../components/common/WizardLayout.vue'
import ErrorBanner from '../components/common/ErrorBanner.vue'
import ThemeCard from '../components/wizard/ThemeCard.vue'
import { themeApi } from '../api/themeApi'
import { extractErrorMessage } from '../utils/errorMessage'
import { useProjectStore } from '../stores/projectStore'
import type { Theme } from '../types/theme'
import { WIZARD_TOTAL_STEPS } from '../constants/wizard'

const props = defineProps<{ projectId: string }>()
const router = useRouter()
const projectStore = useProjectStore()

const themes = ref<Theme[]>([])
const selectedCode = ref<string | null>(null)
const errorMessage = ref('')
const isSubmitting = ref(false)

const canGoNext = computed(() => selectedCode.value !== null)

onMounted(async () => {
  const [project, themeList] = await Promise.all([
    projectStore.ensureLoaded(props.projectId),
    themeApi.list(),
  ])
  selectedCode.value = project.themeCode
  themes.value = themeList
})

async function handleNext() {
  if (!selectedCode.value || isSubmitting.value) {
    return
  }
  isSubmitting.value = true
  errorMessage.value = ''
  try {
    await projectStore.patchProject(props.projectId, { themeCode: selectedCode.value })
    router.push({ name: 'bgm-select', params: { projectId: props.projectId } })
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, 'テーマの保存に失敗しました')
  } finally {
    isSubmitting.value = false
  }
}

function handleBack() {
  router.push({ name: 'title-input', params: { projectId: props.projectId } })
}
</script>

<template>
  <WizardLayout
    :step="4"
    :total-steps="WIZARD_TOTAL_STEPS"
    title="テーマを選択"
    :next-disabled="!canGoNext || isSubmitting"
    @back="handleBack"
    @next="handleNext"
  >
    <ErrorBanner v-if="errorMessage" :message="errorMessage" />
    <div class="theme-grid">
      <ThemeCard
        v-for="theme in themes"
        :key="theme.id"
        :theme="theme"
        :selected="selectedCode === theme.code"
        @select="selectedCode = theme.code"
      />
    </div>
  </WizardLayout>
</template>

<style scoped>
.theme-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 768px) {
  .theme-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
