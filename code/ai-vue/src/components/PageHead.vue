<template>
  <div class="page-head">
    <p class="page-title">{{ title }}</p>
    <div class="action-btn">
      <slot name="buttons" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '页面标题',
  },
})
</script>

<style lang="scss" scoped>
.page-head {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--ink);
}
</style>
