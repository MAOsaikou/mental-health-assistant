# 领域文档

工程技能在探索代码库时，应按本文件消费本仓库的领域文档。

## 探索前先读这些

- 仓库根目录的 **`CONTEXT.md`**，或
- 若根目录存在 **`CONTEXT-MAP.md`**：它指向每个上下文各自的 `CONTEXT.md`。读与当前主题相关的那些。
- **`docs/adr/`**：读与即将动手的区域相关的 ADR。多上下文仓库还要检查 `src/<context>/docs/adr/` 里的上下文级决策。

如果这些文件还不存在，**静默继续**。不要指出缺失，也不要主动建议先创建它们。`/domain-modeling` 技能（经 `/grill-with-docs` 和 `/improve-codebase-architecture` 进入）会在术语或决策真正敲定后，再按需创建。

## 文件结构

单上下文仓库（绝大多数仓库）：

```
/
├── CONTEXT.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

多上下文仓库（根目录存在 `CONTEXT-MAP.md`）：

```
/
├── CONTEXT-MAP.md
├── docs/adr/                          ← 系统级决策
└── src/
    ├── ordering/
    │   ├── CONTEXT.md
    │   └── docs/adr/                  ← 该上下文的决策
    └── billing/
        ├── CONTEXT.md
        └── docs/adr/
```

## 使用术语表里的词汇

产出里若要命名领域概念（工单标题、重构提案、假设、测试名），使用 `CONTEXT.md` 里定义的术语，不要改用术语表明确避免的同义词。

如果需要的概念还不在术语表里，这是一个信号：要么你在发明项目不用的说法（应重新考虑），要么存在真实缺口（记下来交给 `/domain-modeling`）。

## 标出与 ADR 的冲突

若产出与已有 ADR 矛盾，要明确指出，不要悄悄覆盖：

> _与 ADR-0007（事件溯源订单）矛盾，但值得重开讨论，因为……_
