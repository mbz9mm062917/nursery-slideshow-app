<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{ 'files-selected': [files: File[]] }>()
const isDragOver = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

function handleDrop(event: DragEvent) {
  isDragOver.value = false
  const files = Array.from(event.dataTransfer?.files ?? [])
  if (files.length > 0) {
    emit('files-selected', files)
  }
}

function handleFileInputChange(event: Event) {
  const files = Array.from((event.target as HTMLInputElement).files ?? [])
  if (files.length > 0) {
    emit('files-selected', files)
  }
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}
</script>

<template>
  <div
    class="dropzone"
    :class="{ 'is-drag-over': isDragOver }"
    @dragover.prevent="isDragOver = true"
    @dragleave.prevent="isDragOver = false"
    @drop.prevent="handleDrop"
    @click="fileInput?.click()"
  >
    <svg class="dropzone-icon" viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <rect x="3" y="5" width="18" height="14" rx="3" />
      <circle cx="9" cy="10.5" r="1.6" />
      <path d="M21 16l-5.5-5.5a2 2 0 0 0-2.8 0L4 19" />
    </svg>
    <p>ここに写真をドラッグ、またはタップして選択</p>
    <input
      ref="fileInput"
      type="file"
      accept="image/jpeg,image/png"
      multiple
      hidden
      @change="handleFileInputChange"
    />
  </div>
</template>

<style scoped>
.dropzone {
  border: 2px dashed var(--border);
  border-radius: var(--radius-lg);
  padding: 44px 20px;
  text-align: center;
  cursor: pointer;
  color: var(--text-muted);
  background: var(--surface);
  transition: border-color 0.15s ease, background 0.15s ease, color 0.15s ease;
}

.dropzone-icon {
  margin-bottom: 8px;
}

.dropzone.is-drag-over {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}
</style>
