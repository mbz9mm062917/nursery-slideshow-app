<script setup lang="ts">
withDefaults(
  defineProps<{
    step: number
    totalSteps: number
    title: string
    nextDisabled?: boolean
    nextLabel?: string
    showBack?: boolean
  }>(),
  {
    nextDisabled: false,
    nextLabel: '次へ →',
    showBack: true,
  },
)

const emit = defineEmits<{ back: []; next: [] }>()
</script>

<template>
  <div class="wizard">
    <header class="wizard-header">
      <span class="wizard-step">STEP {{ step }} / {{ totalSteps }}</span>
      <h1>{{ title }}</h1>
    </header>

    <section class="wizard-content">
      <slot />
    </section>

    <footer class="wizard-footer">
      <button v-if="showBack" type="button" @click="emit('back')">← 戻る</button>
      <span v-else />
      <button type="button" class="primary" :disabled="nextDisabled" @click="emit('next')">
        {{ nextLabel }}
      </button>
    </footer>
  </div>
</template>

<style scoped>
.wizard {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.wizard-header {
  padding: 20px 20px 12px;
  border-bottom: 1px solid var(--border);
}

.wizard-step {
  font-size: 13px;
  color: var(--text-muted);
}

.wizard-header h1 {
  font-size: 20px;
  margin: 4px 0 0;
}

.wizard-content {
  flex: 1;
  padding: 24px 20px;
}

.wizard-footer {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: space-between;
  padding: 16px 20px;
  border-top: 1px solid var(--border);
  background: var(--bg);
}

.wizard-footer .primary {
  background: var(--accent);
  color: #fff;
  border: none;
  padding: 10px 28px;
  font-size: 16px;
}
</style>
