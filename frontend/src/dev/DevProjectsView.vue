<script setup lang="ts">
import { ref } from 'vue'
import ProjectCreateButton from './components/ProjectCreateButton.vue'
import ProjectList from './components/ProjectList.vue'

const projectListRef = ref<InstanceType<typeof ProjectList> | null>(null)

function handleCreated() {
  projectListRef.value?.loadProjects()
}
</script>

<template>
  <main>
    <h1>スライドショー一覧(開発確認用)</h1>
    <p class="muted">このページは開発・デバッグ用です。本番のスライドショー作成はホーム画面から行います。</p>
    <ProjectCreateButton @created="handleCreated" />
    <ProjectList ref="projectListRef" />
  </main>
</template>
