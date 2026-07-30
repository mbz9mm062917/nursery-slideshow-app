<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import WizardLayout from '../components/common/WizardLayout.vue'
import ErrorBanner from '../components/common/ErrorBanner.vue'
import BgmOption from '../components/wizard/BgmOption.vue'
import { bgmApi } from '../api/bgmApi'
import { extractErrorMessage } from '../utils/errorMessage'
import { useProjectStore } from '../stores/projectStore'
import type { Bgm } from '../types/bgm'

const props = defineProps<{ projectId: string }>()
const router = useRouter()
const projectStore = useProjectStore()

const bgms = ref<Bgm[]>([])
const selectedCode = ref<string | null>(null)
const playingId = ref<number | null>(null)
const errorMessage = ref('')

const canGoNext = computed(() => selectedCode.value !== null)

onMounted(async () => {
  const [project, bgmList] = await Promise.all([projectStore.ensureLoaded(props.projectId), bgmApi.list()])
  selectedCode.value = project.bgmCode
  bgms.value = bgmList
})

function handleTogglePlay(bgmId: number) {
  playingId.value = playingId.value === bgmId ? null : bgmId
}

async function handleNext() {
  if (!selectedCode.value) {
    return
  }
  errorMessage.value = ''
  try {
    await projectStore.patchProject(props.projectId, { bgmCode: selectedCode.value })
    router.push({ name: 'duration-select', params: { projectId: props.projectId } })
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, 'BGMの保存に失敗しました')
  }
}

function handleBack() {
  router.push({ name: 'theme-select', params: { projectId: props.projectId } })
}
</script>

<template>
  <WizardLayout
    :step="5"
    :total-steps="7"
    title="BGMを選択"
    :next-disabled="!canGoNext"
    @back="handleBack"
    @next="handleNext"
  >
    <ErrorBanner v-if="errorMessage" :message="errorMessage" />
    <div class="bgm-list">
      <BgmOption
        v-for="bgm in bgms"
        :key="bgm.id"
        :bgm="bgm"
        :selected="selectedCode === bgm.code"
        :is-playing="playingId === bgm.id"
        @select="selectedCode = bgm.code"
        @toggle-play="handleTogglePlay(bgm.id)"
        @finished="playingId = null"
      />
    </div>
  </WizardLayout>
</template>

<style scoped>
.bgm-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
