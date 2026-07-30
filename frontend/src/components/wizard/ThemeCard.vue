<script setup lang="ts">
import { ref } from 'vue'
import { resolveApiUrl } from '../../utils/resolveApiUrl'
import type { Theme } from '../../types/theme'

defineProps<{ theme: Theme; selected: boolean }>()
defineEmits<{ select: [] }>()

const imageFailed = ref(false)
</script>

<template>
  <button type="button" class="theme-card" :class="{ selected }" @click="$emit('select')">
    <span class="thumbnail">
      <img
        v-if="!imageFailed"
        :src="resolveApiUrl(theme.thumbnailUrl)"
        :alt="theme.name"
        @error="imageFailed = true"
      />
      <span v-else class="placeholder">{{ theme.name }}</span>
    </span>
    <span class="name">{{ theme.name }}</span>
  </button>
</template>

<style scoped>
.theme-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 2px solid var(--border);
  border-radius: 10px;
  background: var(--bg);
  cursor: pointer;
}

.theme-card.selected {
  border-color: var(--accent);
}

.thumbnail {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 6px;
  overflow: hidden;
  background: var(--code-bg, #f4f3ec);
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder {
  color: var(--text-muted);
  font-size: 14px;
}

.name {
  font-size: 14px;
  color: var(--text);
}
</style>
