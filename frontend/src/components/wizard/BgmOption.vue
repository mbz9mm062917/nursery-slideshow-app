<script setup lang="ts">
import { ref, watch } from 'vue'
import { resolveApiUrl } from '../../utils/resolveApiUrl'
import type { Bgm } from '../../types/bgm'

const props = defineProps<{ bgm: Bgm; selected: boolean; isPlaying: boolean }>()
const emit = defineEmits<{ select: []; 'toggle-play': []; finished: [] }>()

const audioRef = ref<HTMLAudioElement | null>(null)

watch(
  () => props.isPlaying,
  (playing) => {
    if (!audioRef.value) return
    if (playing) {
      audioRef.value.play().catch(() => emit('finished'))
    } else {
      audioRef.value.pause()
      audioRef.value.currentTime = 0
    }
  },
)
</script>

<template>
  <label class="bgm-option" :class="{ selected }">
    <input type="radio" :checked="selected" @change="emit('select')" />
    <span class="name">{{ bgm.name }}</span>
    <button type="button" @click.prevent="emit('toggle-play')">
      {{ isPlaying ? '■ 停止' : '▶ 試聴' }}
    </button>
    <audio ref="audioRef" :src="resolveApiUrl(bgm.fileUrl)" @ended="emit('finished')" />
  </label>
</template>

<style scoped>
.bgm-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  cursor: pointer;
}

.bgm-option.selected {
  box-shadow: 0 0 0 2px var(--accent), var(--shadow-card);
  background: var(--accent-soft);
}

.name {
  flex: 1;
  font-weight: 600;
}

.bgm-option button {
  border-radius: var(--radius-pill);
}
</style>
