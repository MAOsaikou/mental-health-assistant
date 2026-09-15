<template>
  <div class="auth-layout">
    <section class="left">
      <div class="orb" aria-hidden="true">光</div>
      <h2>小光心理助手</h2>
      <p>这里很慢，也很安全。你不必先变得更好，才有资格被听见。</p>
      <ul>
        <li>感受会被接住，而不是被纠正</li>
        <li>想说多少就说多少</li>
        <li>需要支持可拨 12356，紧急时拨 120 或 110</li>
      </ul>
    </section>
    <section class="right">
      <router-view v-slot="{ Component, route: currentRoute }">
        <component :is="Component" :key="currentRoute.fullPath" />
      </router-view>
    </section>
  </div>
</template>

<style scoped lang="scss">
.auth-layout {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  min-height: 100vh;
}

.left {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px;
  color: #fffaf2;
  background:
    radial-gradient(circle at 20% 20%, rgba(246, 226, 184, 0.28), transparent 36%),
    linear-gradient(160deg, #2c6a5b 0%, #1e4c41 100%);
}

.orb {
  width: 84px;
  height: 84px;
  margin-bottom: 24px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: radial-gradient(circle at 35% 30%, #fff6e4, #d4a056 60%, transparent 78%);
  font-family: "Fraunces", Georgia, serif;
  font-size: 32px;
  animation: glow-shift 6s ease-in-out infinite;
}

h2 {
  font-family: "Fraunces", Georgia, serif;
  font-size: 40px;
  margin-bottom: 16px;
}

p, li {
  max-width: 420px;
  line-height: 1.8;
  opacity: 0.92;
}

ul {
  margin-top: 24px;
}

li {
  padding-left: 16px;
  position: relative;
  margin-bottom: 8px;
}

li::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0.7em;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f4e2c0;
}

.right {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: var(--paper);
}

@media (max-width: 860px) {
  .auth-layout {
    grid-template-columns: 1fr;
  }

  .left {
    min-height: 280px;
    padding: 32px 24px;
  }
}
</style>
