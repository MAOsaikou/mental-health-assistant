# 工单跟踪：本地 Markdown

本仓库的工单和 spec 以 markdown 文件存放在 `.scratch/`。

## 约定

- 每个功能一个目录：`.scratch/<feature-slug>/`
- spec 文件是 `.scratch/<feature-slug>/spec.md`
- 实现工单按票拆文件：`.scratch/<feature-slug>/issues/<NN>-<slug>.md`，从 `01` 起编号，不要写成一份合并的 tickets 文件
- 分诊状态写在每个工单文件靠近顶部的 `Status:` 行（角色字符串见 `triage-labels.md`）
- 评论和讨论追加在文件末尾的 `## Comments` 标题下

## 当技能说「发布到 issue tracker」

在 `.scratch/<feature-slug>/` 下新建文件（目录不存在就先建）。

## 当技能说「读取相关工单」

读取引用路径上的文件。用户通常会直接给出路径或工单编号。

## Wayfinding 操作

供 `/wayfinder` 使用。**地图**是一份文件，每张工单对应一个 **子文件**。

- **地图**：`.scratch/<effort>/map.md`（Notes / Decisions-so-far / Fog 正文）。
- **子工单**：`.scratch/<effort>/issues/NN-<slug>.md`，从 `01` 起编号，问题写在正文里。`Type:` 行记录工单类型（`research` / `prototype` / `grilling` / `task`）；`Status:` 行记录 `claimed` / `resolved`。
- **阻塞**：靠近顶部写 `Blocked by: NN, NN`。列出的每一份文件都变成 `resolved` 后，该工单才算解除阻塞。
- **前沿**：扫描 `.scratch/<effort>/issues/`，找未关闭、未阻塞、未被认领的文件；编号最小的优先。
- **认领**：开始工作前先把 `Status:` 设为 `claimed` 并保存。
- **完成**：在 `## Answer` 标题下追加答案，把 `Status:` 设为 `resolved`，再把一条上下文指针（摘要 + 链接）追加到 `map.md` 的 Decisions-so-far。
