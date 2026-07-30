<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { devProjectApi } from '../api/devProjectApi'
import ProjectEditForm from './ProjectEditForm.vue'
import type { Project } from '../../types/project'

const projects = ref<Project[]>([])
const isLoading = ref(false)
const errorMessage = ref('')

async function loadProjects() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    projects.value = await devProjectApi.list()
  } catch (error) {
    errorMessage.value = '一覧の取得に失敗しました'
  } finally {
    isLoading.value = false
  }
}

async function handleDelete(projectId: string) {
  if (!window.confirm('このプロジェクトを削除しますか？')) {
    return
  }
  try {
    await devProjectApi.remove(projectId)
    await loadProjects()
  } catch (error) {
    errorMessage.value = '削除に失敗しました'
  }
}

onMounted(loadProjects)

defineExpose({ loadProjects })
</script>

<template>
  <div>
    <p v-if="isLoading">読み込み中...</p>
    <p v-else-if="errorMessage" class="error">{{ errorMessage }}</p>
    <p v-else-if="projects.length === 0">プロジェクトはまだありません</p>
    <ul v-else>
      <li v-for="project in projects" :key="project.id">
        {{ project.title ?? '(タイトル未設定)' }} — 写真{{ project.photoCount }}枚
        <button @click="handleDelete(project.id)">削除</button>
        <ProjectEditForm :project="project" @updated="loadProjects" />
      </li>
    </ul>
  </div>
</template>

<style scoped>
.error {
  color: #c0392b;
}
</style>
