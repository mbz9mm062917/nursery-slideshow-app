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
  border-radius: 12px;
  padding: 48px 20px;
  text-align: center;
  cursor: pointer;
  color: var(--text-muted);
}

.dropzone.is-drag-over {
  border-color: var(--accent);
  color: var(--accent);
}
</style>
