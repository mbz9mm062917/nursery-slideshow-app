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
      <div
        class="wizard-dots"
        role="progressbar"
        :aria-valuenow="step"
        :aria-valuemin="1"
        :aria-valuemax="totalSteps"
      >
        <span
          v-for="n in totalSteps"
          :key="n"
          class="dot"
          :class="{ done: n < step, current: n === step }"
        />
      </div>
    </header>

    <section class="wizard-content">
      <slot />
    </section>

    <footer class="wizard-footer">
      <button v-if="showBack" type="button" class="btn-pill ghost" @click="emit('back')">← 戻る</button>
      <span v-else />
      <button type="button" class="btn-pill" :disabled="nextDisabled" @click="emit('next')">
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
  padding: 20px 20px 16px;
}

.wizard-step {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--accent);
  font-variant-numeric: tabular-nums;
}

.wizard-header h1 {
  font-size: 21px;
  margin: 6px 0 0;
}

.wizard-dots {
  display: flex;
  gap: 5px;
  margin-top: 14px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-pill);
  background: var(--border);
  transition: width 0.15s ease, background 0.15s ease;
}

.dot.done {
  background: var(--accent);
}

.dot.current {
  width: 18px;
  background: var(--accent);
}

.wizard-content {
  flex: 1;
  padding: 12px 20px 24px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.wizard-footer {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--bg);
}
</style>
