# Stage 4 · 目标 3：使用 Room 管理结构化本地数据

## 1. 本节目标

这一节完成了 Room 的完整入门链路：

```text
Compose UI
→ ViewModel
→ Repository
→ DAO
→ Room
→ SQLite
```

同时把数据库变化通过 Flow 自动传回 UI：

```text
SQLite / Room
→ Flow
→ ViewModel
→ StateFlow
→ Compose
```

最终实现了：

- 新增任务
- 查询任务
- 删除任务
- 更新完成状态
- 条件查询
- 参数化查询
- 数据库版本迁移
- 使用 Database Inspector / Device Explorer 查看真实数据库

---

## 2. Room 的三个核心角色

### Entity

Entity 用来描述一张表的数据结构。

文件：

```text
app/src/main/java/com/example/learnandroidfromai/data/local/TaskEntity.kt
```

示例：

```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,

    val completed: Boolean = false,

    val description: String? = null
)
```

对应 SQLite 表：

```text
tasks
├── id
├── title
├── completed
└── description
```

---

### DAO

DAO 定义数据库操作。

文件：

```text
app/src/main/java/com/example/learnandroidfromai/data/local/TaskDao.kt
```

本节使用过：

```kotlin
@Insert
suspend fun insert(task: TaskEntity)

@Delete
suspend fun delete(task: TaskEntity)

@Update
suspend fun update(task: TaskEntity)
```

查询使用 `@Query`：

```kotlin
@Query("SELECT * FROM tasks")
fun getAll(): Flow<List<TaskEntity>>
```

---

### RoomDatabase

Database 把 Entity 和 DAO 组织到一个实际数据库中。

文件：

```text
app/src/main/java/com/example/learnandroidfromai/data/local/AppDatabase.kt
```

核心结构：

```kotlin
@Database(
    entities = [TaskEntity::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
```

一个 `RoomDatabase` 通常对应一个逻辑 SQLite 数据库文件。

---

## 3. 数据库实例与单例

数据库通过：

```kotlin
Room.databaseBuilder(
    context.applicationContext,
    AppDatabase::class.java,
    "app_database"
)
```

创建。

第三个参数：

```text
app_database
```

就是数据库文件名。

为了避免重复创建数据库实例，使用：

```kotlin
@Volatile
private var INSTANCE: AppDatabase? = null
```

配合：

```kotlin
synchronized(this)
```

实现单例获取。

---

## 4. Repository 的作用

文件：

```text
app/src/main/java/com/example/learnandroidfromai/data/TaskRepository.kt
```

Repository 位于 ViewModel 和 DAO 之间：

```text
ViewModel
→ Repository
→ DAO
```

例如：

```kotlin
suspend fun addTask(title: String) {
    taskDao.insert(
        TaskEntity(title = title)
    )
}
```

删除：

```kotlin
suspend fun deleteTask(task: TaskEntity) {
    taskDao.delete(task)
}
```

更新：

```kotlin
suspend fun updateTask(task: TaskEntity) {
    taskDao.update(task)
}
```

查询：

```kotlin
fun getAllTasks(): Flow<List<TaskEntity>> {
    return taskDao.getAll()
}
```

Repository 的价值之一是：

> DAO 的实现方式改变时，上层 ViewModel 不一定需要跟着改变。

例如 DAO 从固定查询改成参数化查询后，Repository 仍然可以保留：

```kotlin
fun getIncompleteTasks(): Flow<List<TaskEntity>>
```

内部改成：

```kotlin
return taskDao.getTasksByCompleted(false)
```

这样 ViewModel 不需要修改。

---

## 5. Flow 与 Room

最开始查询写成：

```kotlin
suspend fun getAll(): List<TaskEntity>
```

这表示：

> 查询一次，然后返回一次结果。

后来改成：

```kotlin
fun getAll(): Flow<List<TaskEntity>>
```

含义变成：

> 持续观察这条查询结果。

当 `tasks` 表发生变化时，Room 会重新执行查询，并通过 Flow 发出新的结果。

因此：

```text
insert / update / delete
↓
tasks 表变化
↓
Room 重新执行查询
↓
Flow 发出新的 List<TaskEntity>
↓
ViewModel collect
↓
StateFlow 更新
↓
Compose 自动刷新
```

不再需要手动调用“刷新”。

---

## 6. ViewModel 中 collect Flow

ViewModel 中使用：

```kotlin
viewModelScope.launch {
    repository.getAllTasks().collect { tasks ->
        _uiState.update {
            it.copy(tasks = tasks)
        }
    }
}
```

因此数据库成为 UI 的真实数据源。

---

## 7. CRUD

本节完整跑通了 CRUD。

### Create

```kotlin
@Insert
suspend fun insert(task: TaskEntity)
```

### Read

```kotlin
@Query("SELECT * FROM tasks")
fun getAll(): Flow<List<TaskEntity>>
```

### Update

```kotlin
@Update
suspend fun update(task: TaskEntity)
```

例如切换完成状态：

```kotlin
task.copy(
    completed = !task.completed
)
```

再交给 Room 更新。

### Delete

```kotlin
@Delete
suspend fun delete(task: TaskEntity)
```

Room 根据 Entity 的主键 `id` 判断删除哪一行。

---

## 8. 主键

Entity 中：

```kotlin
@PrimaryKey(autoGenerate = true)
val id: Int = 0
```

让 SQLite / Room 自动生成主键。

删除旧记录后，新记录的主键仍然继续递增，例如：

```text
1
2
3
```

删除 `2`：

```text
1
3
```

再新增：

```text
1
3
4
```

主键的意义是：

> 唯一标识一条记录。

它不是必须连续的“列表序号”。

---

## 9. 条件查询

固定条件查询：

```kotlin
@Query("SELECT * FROM tasks WHERE completed = 0")
fun getIncompleteTasks(): Flow<List<TaskEntity>>
```

含义：

```text
只返回 completed = false 的任务
```

当某个任务从：

```text
completed = false
```

变成：

```text
completed = true
```

它会自动从查询结果中消失。

---

## 10. 参数化查询

按 id 查询：

```kotlin
@Query("SELECT * FROM tasks WHERE id = :taskId")
suspend fun getById(taskId: Int): TaskEntity?
```

其中：

```text
:taskId
```

对应 Kotlin 参数：

```kotlin
taskId
```

例如：

```kotlin
taskDao.getById(3)
```

相当于查询：

```sql
SELECT * FROM tasks WHERE id = 3
```

返回类型是：

```kotlin
TaskEntity?
```

因为对应 id 可能不存在。

---

更通用的完成状态查询：

```kotlin
@Query("SELECT * FROM tasks WHERE completed = :completed")
fun getTasksByCompleted(
    completed: Boolean
): Flow<List<TaskEntity>>
```

调用：

```kotlin
getTasksByCompleted(false)
```

查未完成任务。

调用：

```kotlin
getTasksByCompleted(true)
```

查已完成任务。

Room 会负责把 Kotlin `Boolean` 正确绑定到 SQLite 参数。

---

## 11. flatMapLatest 切换不同查询 Flow

为了在“全部任务”和“仅未完成”之间切换，ViewModel 使用：

```kotlin
showIncompleteOnly
    .flatMapLatest { onlyIncomplete ->
        if (onlyIncomplete) {
            repository.getIncompleteTasks()
        } else {
            repository.getAllTasks()
        }
    }
```

可以理解成：

```text
筛选条件变化
↓
停止收集旧 Flow
↓
改为收集新的 Flow
```

例如：

```text
false
→ getAllTasks()

true
→ getIncompleteTasks()
```

---

## 12. UI 不自己保存数据库状态

Checkbox：

```kotlin
Checkbox(
    checked = task.completed,
    onCheckedChange = {
        taskViewModel.toggleCompleted(task)
    }
)
```

其中：

```kotlin
checked = task.completed
```

说明 UI 的显示状态来自数据库。

流程：

```text
点击 Checkbox
↓
ViewModel 更新数据库
↓
Room Flow 发新数据
↓
UI 自动显示新状态
```

UI 不是数据库状态的最终来源。

---

## 13. Migration：数据库版本迁移

数据库 schema 变化时，需要处理已有用户的旧数据库。

例如原来：

```text
version 1

tasks
├── id
├── title
└── completed
```

后来新增：

```text
description
```

Entity 改成：

```kotlin
val description: String? = null
```

Database 版本：

```kotlin
version = 2
```

迁移：

```kotlin
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE tasks ADD COLUMN description TEXT"
        )
    }
}
```

注册：

```kotlin
.addMigrations(MIGRATION_1_2)
```

最终过程：

```text
数据库 v1
↓
MIGRATION_1_2
↓
执行 ALTER TABLE
↓
数据库 v2
```

旧数据仍然保留，新字段的旧记录值为：

```text
NULL
```

---

## 14. 多版本迁移

通常按版本逐步维护：

```text
1 → 2
2 → 3
3 → 4
```

对应：

```kotlin
MIGRATION_1_2
MIGRATION_2_3
MIGRATION_3_4
```

注册：

```kotlin
.addMigrations(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4
)
```

如果某个用户数据库还是 v1，而当前 App 已经要求 v4：

```text
1 → 2 → 3 → 4
```

Room 会沿着可用 migration 路径升级。

已经发布过的 migration 一般不要回头修改。

因为真实用户可能仍然停留在不同数据库版本：

```text
用户 A：v1
用户 B：v2
用户 C：v3
```

最新 App 必须保证它们都能安全升级到当前版本。

---

## 15. Room 与 Alembic 版本迁移的区别

Room 使用简单的整数数据库版本：

```text
1 → 2 → 3 → 4
```

并通过：

```kotlin
Migration(1, 2)
Migration(2, 3)
```

描述升级步骤。

Alembic 通常使用 revision 字符串：

```text
revision A
→ revision B
→ revision C
```

并通过：

```text
revision
down_revision
```

描述迁移依赖关系。

Room 更像是一条整数版本升级链。

---

## 16. 数据库文件在 Android 手机上的位置

本项目数据库文件名：

```text
app_database
```

通常位于 App 私有目录：

```text
/data/user/0/com.example.learnandroidfromai/databases/app_database
```

常见等价路径：

```text
/data/data/com.example.learnandroidfromai/databases/app_database
```

实际目录里通常能看到：

```text
app_database
app_database-wal
app_database-shm
```

其中：

```text
app_database
```

是主 SQLite 数据库文件。

```text
app_database-wal
```

是 WAL 日志。

```text
app_database-shm
```

是 WAL 模式配套的共享内存文件。

---

## 17. Database Inspector

Android Studio：

```text
View
→ Tool Windows
→ App Inspection
→ Database Inspector
```

可以直接查看：

```text
app_database
→ tasks
```

本节实际确认到：

```text
id
title
completed
description
```

旧记录的：

```text
description
```

为：

```text
NULL
```

说明 migration 已成功执行。

还可以执行：

```sql
PRAGMA table_info(tasks);
```

直接查看 SQLite 真实表结构。

---

## 18. Device Explorer

Android Studio：

```text
View
→ Tool Windows
→ Device Explorer
```

可以找到 App 私有目录中的：

```text
com.example.learnandroidfromai
└── databases
    ├── app_database
    ├── app_database-wal
    └── app_database-shm
```

这说明 Room 最终仍然是在操作设备上的 SQLite 文件。

---

## 19. 本节最终心智模型

Room 并不是另一种数据库。

它是 Android 官方提供的一层 SQLite 抽象：

```text
Kotlin Entity
↓
Room 映射
↓
SQLite Table
```

数据库操作：

```text
DAO
↓
Room 生成实现
↓
SQLite SQL
```

查询结果：

```text
SQLite
↓
Room
↓
Flow<List<Entity>>
↓
ViewModel
↓
StateFlow
↓
Compose
```

数据库 schema 变化：

```text
旧数据库版本
↓
Migration
↓
新数据库版本
```

---

## 20. 本节已经掌握

完成这一节后，已经能够：

- 定义 Room Entity
- 定义 DAO
- 创建 RoomDatabase
- 创建数据库单例
- 使用 Repository 隔离数据层
- 使用 `@Insert`
- 使用 `@Delete`
- 使用 `@Update`
- 使用 `@Query`
- 使用参数化查询
- 使用 Flow 观察数据库变化
- 使用 `flatMapLatest` 切换查询
- 让 Compose 自动响应数据库变化
- 理解主键与自动递增
- 理解数据库版本号
- 编写最基本的 Migration
- 理解多版本逐步迁移
- 使用 Database Inspector 查看 SQLite
- 使用 Device Explorer 找到真实数据库文件

至此，Stage 4 目标 3：

> 能够使用本地数据库管理结构化数据

已经完成。
