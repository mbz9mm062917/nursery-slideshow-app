<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import WizardLayout from '../components/common/WizardLayout.vue'
import ErrorBanner from '../components/common/ErrorBanner.vue'
import { extractErrorMessage } from '../utils/errorMessage'
import { useProjectStore } from '../stores/projectStore'
import { WIZARD_TOTAL_STEPS } from '../constants/wizard'

const props = defineProps<{ projectId: string }>()
const router = useRouter()
const projectStore = useProjectStore()

const DURATION_OPTIONS = [3, 5, 7] as const
const selectedDuration = ref<number | null>(null)
const errorMessage = ref('')
const isSubmitting = ref(false)

const canGoNext = computed(() => selectedDuration.value !== null)

onMounted(async () => {
  const project = await projectStore.ensureLoaded(props.projectId)
  selectedDuration.value = project.slideDurationSec
})

async function handleNext() {
  if (selectedDuration.value === null || isSubmitting.value) {
    return
  }
  isSubmitting.value = true
  errorMessage.value = ''
  try {
    await projectStore.patchProject(props.projectId, { slideDurationSec: selectedDuration.value })
    router.push({ name: 'preview', params: { projectId: props.projectId } })
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, '保存に失敗しました')
  } finally {
    isSubmitting.value = false
  }
}

function handleBack() {
  router.push({ name: 'bgm-select', params: { projectId: props.projectId } })
}
</script>

<template>
  <WizardLayout
    :step="7"
    :total-steps="WIZARD_TOTAL_STEPS"
    title="スライドの表示時間"
    :next-disabled="!canGoNext || isSubmitting"
    @back="handleBack"
    @next="handleNext"
  >
    <ErrorBanner v-if="errorMessage" :message="errorMessage" />
    <div class="duration-options">
      <button
        v-for="duration in DURATION_OPTIONS"
        :key="duration"
        type="button"
        class="duration-button"
        :class="{ selected: selectedDuration === duration }"
        @click="selectedDuration = duration"
      >
        {{ duration }}秒
      </button>
    </div>
  </WizardLayout>
</template>

<style scoped>
.duration-options {
  display: flex;
  gap: 16px;
}

.duration-button {
  flex: 1;
  padding: 24px;
  font-size: 20px;
  font-weight: 700;
  border: none;
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-card);
  color: var(--text);
}

.duration-button.selected {
  background: var(--accent);
  color: #fff;
  box-shadow: var(--shadow-btn);
}
</style>
