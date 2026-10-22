# 个人数据到同学共享：预留框架与伪代码

当前版本只在本机运行。没有服务器地址、账号、邀请和联网行为；UI不显示尚不可用的共享入口。

## 已存在的边界

每条课程、笔记、作业包含 `id`, `ownerId`, `spaceId`, `revision`, `updatedAt`, `deletedAt`。`id`用于稳定引用；本机新记录使用UUID；`spaceId=personal`。ownerId只是本机数据归属标记，**不能作为服务端身份凭据**。

`sharing.js` 导出三个明确禁用的方法：

```javascript
SharingPort.pull(spaceId, cursor)
SharingPort.push(spaceId, changes)
SharingPort.invite(spaceId, member)
```

未来通过注入一个已登录的网络适配器替换它们，保持个人笔记与课表可离线使用。同步只能由独立服务接入，不能直接让网页访问应用私有目录。

## 共享时的数据拆分

| 数据 | 共享范围 | 权限 |
|---|---|---|
| Course、Lesson | 一个学习小组空间 | editor可编辑，viewer只读 |
| AssignmentDefinition | 老师要求、序号、截止时间 | editor可编辑 |
| AssignmentProgress | 每人的完成状态、完成时间、提醒设置 | 仅本人可修改 |
| Note | 默认个人；用户主动选择后共享一份 | 作者可编辑，按空间成员可读 |
| MediaObject | 属于明确的记录与空间 | 服务端逐次检查读取权限 |
| Membership | userId、spaceId、role | owner管理 |

个人v1的task未来迁移为一个AssignmentDefinition加一个仅本人拥有的AssignmentProgress。不得把甲同学的“已完成”同步为乙同学也完成，不共享私人备注和个人提醒。

## 服务端提交伪代码

```python
def push(session_token, space_id, mutations):
    user = authenticate(session_token)          # 从可信会话解析身份
    membership = require_member(user.id, space_id)
    results = []
    with database.transaction():
        for m in mutations:
            require_valid_schema(m)
            require(m.space_id == space_id)
            require_role_for_entity(membership.role, m.entity_type, m.action)
            if m.entity_type == 'AssignmentProgress':
                require(m.user_id == user.id)  # 不能提交别人的完成状态
            existing = database.get_for_update(space_id, m.entity_type, m.id)
            if database.has_applied(user.id, m.operation_id):
                results.append(database.previous_result(m.operation_id))
                continue                       # 断线重试不产生重复记录
            if existing and existing.revision != m.base_revision:
                results.append(conflict(m, existing))
                continue                       # 保留两版，返回用户决定
            require_media_belongs_to_space(m.media_ids, space_id, user)
            next_record = server_merge(existing, m)
            next_record.owner_id = existing.owner_id if existing else user.id
            next_record.revision = (existing.revision if existing else 0) + 1
            next_record.updated_at = server_clock.now()
            database.save_with_change_cursor(next_record)
            database.mark_applied(user.id, m.operation_id, next_record)
            results.append(accepted(next_record))
    return results
```

## 拉取与本地队列伪代码

```python
def pull(session_token, space_id, cursor):
    user = authenticate(session_token)
    require_member(user.id, space_id)
    changes = database.changes_after(space_id, cursor, limit=200)
    visible = filter_by_record_permissions(changes, user.id)
    return { 'changes': visible, 'next_cursor': changes.last_cursor }

def sync_local(adapter, space_id):
    # 先拉取变更，再上传本地队列；断网不妨碍编辑和拍照。
    remote = adapter.pull(space_id, local.cursor(space_id))
    with local.transaction():
        for record in remote.changes:
            if local.has_pending_edit(record.id):
                local.keep_conflict_copy(record)  # 不覆盖未上传的本地改动
            else:
                local.upsert(record)
        local.set_cursor(space_id, remote.next_cursor)
    for batch in local.pending_batches(space_id):
        media_map = upload_missing_media_with_permission(adapter, batch)
        results = adapter.push(space_id, bind_media(batch, media_map))
        local.acknowledge_only_accepted(results)
```

## 照片与邀请

- 原图按SHA-256内容校验，断点上传；读取使用短时授权，离开小组后不能继续从服务端取图。
- 邀请由空间owner创建，短时有效、一次性兑换；服务端在兑换时重新检查权限。
- 分享笔记前明确列出将共享的正文和照片，不默认上传整份个人备份。
- 冲突UI显示本地与远端版本，用户选择保留或合并；不使用“客户端更新时间较新就覆盖”的策略。
- 删除墓碑在所有活跃客户端确认到对应cursor后再按服务端保留策略清理。

这些是后续服务端的接口契约与伪代码，不表示1.0已具有同步、账号或多人权限能力。

## 调课共享预留（1.1）

个人版本的 `adjustments` 是有序原子事务日志，每项包含 id、kind、title、createdAt、semesterStart、weeks 和 changes（key / before / after）。这部分目前只本地保存并随完整备份导出，不发送给任何同学。

未来共享需增加班级 scheduleRevision 与 transactionId；服务端比较班级当前版本后，重放并检验整个事务、冲突和来源日期，整项成功或整项拒绝。不能按单条课程 last-write-wins 合并，否则对调会只成功一半。

```text
submitScheduleChange(space, expectedScheduleRevision, transaction):
    require member.role in [owner, editor]
    begin database transaction
    current = lockSchedule(space)
    if current.revision != expectedScheduleRevision:
        return conflict(current)  // 客户端重新预览，不能静默覆盖
    next = replayAndValidate(current, transaction)
    save(next, revision=current.revision+1)
    appendAudit(transaction, actor=member.id)
    commit
    publishScheduleRevision(space)
    // 个人作业截止时间与完成状态保持独立
```

撤销共享事务应写入新的反向事务并检查后续依赖，不能沿用个人日志直接 pop 的方式。
