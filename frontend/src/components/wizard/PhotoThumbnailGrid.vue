<script setup lang="ts">
import { resolveApiUrl } from '../../utils/resolveApiUrl'
import type { Photo } from '../../types/photo'

defineProps<{ photos: Photo[] }>()
const emit = defineEmits<{ delete: [photoId: number] }>()
</script>

<template>
  <div class="grid">
    <div v-for="photo in photos" :key="photo.id" class="thumbnail">
      <img :src="resolveApiUrl(photo.fileUrl)" :alt="photo.originalFileName" />
      <button type="button" class="delete" @click="emit('delete', photo.id)">×</button>
    </div>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  margin-top: 20px;
}

.thumbnail {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.delete {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  padding: 0;
  border-radius: 50%;
  line-height: 1;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border: none;
}
</style>
