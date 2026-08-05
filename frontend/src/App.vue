<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="ambient" aria-hidden="true">
    <span class="ambient-blob ambient-blob-a"></span>
    <span class="ambient-blob ambient-blob-b"></span>
    <span class="ambient-blob ambient-blob-c"></span>
  </div>
  <RouterView />
</template>

<style scoped>
/*
 * 画面の内容自体は#app(max-width指定あり)の中に収まるため、
 * 広いPC画面だと左上に寄って余白だけが目立ってしまう。
 * ビューポート全体に固定表示するこの背景だけは#appの外に置き、
 * どのページでも画面全体に奥行きを持たせる。
 */
.ambient {
  position: fixed;
  inset: 0;
  z-index: -1;
  overflow: hidden;
}

.ambient-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.22;
}

.ambient-blob-a {
  width: 46vw;
  height: 46vw;
  max-width: 640px;
  max-height: 640px;
  background: var(--accent-yellow);
  top: -18vw;
  right: -14vw;
}

.ambient-blob-b {
  width: 38vw;
  height: 38vw;
  max-width: 520px;
  max-height: 520px;
  background: var(--accent-mint);
  bottom: -14vw;
  left: -12vw;
}

.ambient-blob-c {
  width: 26vw;
  height: 26vw;
  max-width: 380px;
  max-height: 380px;
  background: var(--accent);
  opacity: 0.12;
  top: 30vh;
  left: 60vw;
}
</style>
