<script setup lang="ts">
import { ref, watch } from 'vue'
import { resolveApiUrl } from '../../utils/resolveApiUrl'
import type { Photo } from '../../types/photo'

const props = defineProps<{ photos: Photo[] }>()
const emit = defineEmits<{ reorder: [photoIds: number[]] }>()

const localPhotos = ref<Photo[]>([...props.photos])
watch(
  () => props.photos,
  (newPhotos) => {
    localPhotos.value = [...newPhotos]
  },
)

const draggedIndex = ref<number | null>(null)

function handleDrop(targetIndex: number) {
  if (draggedIndex.value === null || draggedIndex.value === targetIndex) {
    return
  }
  const items = [...localPhotos.value]
  const [moved] = items.splice(draggedIndex.value, 1)
  items.splice(targetIndex, 0, moved)
  localPhotos.value = items
  draggedIndex.value = null
  emit(
    'reorder',
    items.map((photo) => photo.id),
  )
}
</script>

<template>
  <p class="muted">写真をドラッグして順番を並び替えられます</p>
  <div class="sorter-grid">
    <div
      v-for="(photo, index) in localPhotos"
      :key="photo.id"
      class="sorter-item"
      draggable="true"
      @dragstart="draggedIndex = index"
      @dragover.prevent
      @drop="handleDrop(index)"
    >
      <img :src="resolveApiUrl(photo.fileUrl)" :alt="photo.originalFileName" />
      <span class="order-badge">{{ index + 1 }}</span>
    </div>
  </div>
</template>

<style scoped>
.sorter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.sorter-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border);
  cursor: grab;
}

.sorter-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  pointer-events: none;
}

.order-badge {
  position: absolute;
  top: 4px;
  left: 4px;
  min-width: 20px;
  text-align: center;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 12px;
  padding: 2px 6px;
}
</style>
