# Drools 动态规则 Demo（com.example.rule）

把 "规则做成可按页面配置" 落到了本工程里：运营在页面上配参数 / 上传 Excel 决策表，**运行时编译成规则并立即生效，不用重启**。

- 页面：<http://localhost:9008/eurekaclient/rule/ui>
- 代码：`src/main/java/com/example/rule/**` + `src/main/java/com/example/controller/RuleAdminController.java`
- 页面：`src/main/resources/static/rule-admin.html`
- 测试：`src/test/java/com/example/rule/RuleEngineLocalTest.java`（11 个用例，`mvn -Dtest=RuleEngineLocalTest test`）

## 依赖（Drools 10.2.0，本工程 JDK21）

```xml
<drools.version>10.2.0</drools.version>
org.drools:drools-engine        <!-- 10.x 聚合包：kie-api/kie-internal/core/kiesession/compiler/model-codegen/... -->
org.drools:drools-mvel          <!-- dialect "mvel" 需要 -->
org.drools:drools-decisiontables<!-- 决策表 + 自带 poi/poi-ooxml 5.4.1 -->
org.apache.commons:commons-compress:1.27.1  <!-- 覆盖 minio 带的 1.21，否则 POI 报 NoSuchMethodError -->
commons-io:commons-io:2.18.0                <!-- 原来重复声明 2.6，会让 POI 直接 NoClassDefFoundError -->
```
不需 `kie-spring`（它停更在 7.74.1.Final，且 Drools 与 Spring 没有必须的耦合，`KieContainer/KieBase` 自己 `@Bean`/`@Component` 持有即可）。

## 两条配置路线（同一套引擎）

| | 路线一：参数模板 | 路线二：Excel 决策表 |
|---|---|---|
| 谁在配 | 运营选类型、填参数（等级下拉、折扣率数字） | 业务方下载 xlsx 模板，只改 B/C 两列取值 |
| 后端做什么 | `RuleTypeCatalog` 的模板渲染 `$o.setDiscount(${rate})` → DRL | `drools-decisiontables` 把 xlsx 在运行时编译成规则 |
| 安全边界 | 白名单模板，页面永远写不到 RHS 代码 | `DecisionTableGuard`：结构行逐格比对 + 取值白名单/正则 + 禁公式 + D 列及以后不许有内容 |
| 新增规则类型 | 往 `rule_type_meta/field/template` 加数据，零 Java 改动 | 换一份 `DecisionTableSpec` |

**关键**：决策表单元格会被编译成规则 RHS（= 任意 Java 代码），所以"允许上传任意 xlsx"等于允许执行任意代码。
本次实测：往 D 列塞 `Runtime.getRuntime().exec(...)` 的 xlsx 被 400 拦下（`第 10 行第 4 列不允许填写内容`）。

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/rule/type/meta` | 规则类型 + 字段元数据（页面据此渲染表单） |
| POST | `/rule/rule/preview` | 渲染 DRL（不编译） |
| POST | `/rule/rule/validate` | 试编译（连同现有生效规则一起编译，不落库） |
| POST | `/rule/rule/publish` | 校验 → 渲染 → 编译 → 通过才落库换版 |
| GET | `/rule/rule/list` | 规则列表（含参数、DRL、版本、状态） |
| POST | `/rule/rule/status` | 启停（刷新失败会回滚状态） |
| POST | `/rule/rule/delete` | 删除并刷新 |
| POST | `/rule/run` | 试算：返回命中规则、命中说明、折扣/运费/拦截/最终应付 |
| GET | `/rule/state` | 生效版本、引擎规则数、上次点火次数 |
| GET | `/rule/dt/template` | 下载决策表模板 |
| POST | `/rule/dt/preview` | 上传 xlsx → 结构/取值守门 + 回显将生成的 DRL |
| POST | `/rule/dt/publish` | 上传 xlsx → 守门 → 编译 → 落库换版 |
| POST | `/rule/dt/disable` | 停用决策表 |

## 换 MySQL 持久化

`RuleRepository` 是内存实现，换成 Dao 即可（接口不变）。表结构见
`/Users/admin/Projects/drools-dt-probe/ANALYSIS.md` 第 4.1 节（`rule_asset` / `rule_definition` + 三类元数据表）。
多实例部署：发布落 DB，各实例各自刷新自己的 KieBase（轮询版本号 / Redis pub-sub / MQ 广播）。

## 踩过的坑（都已在代码注释里）

1. **commons-io 2.6 vs POI 5.4.1**：工程里 `commons-io` 声明了两次（2.11.0 与 2.6），后声明的覆盖前者；POI 5.4.1 需要 2.12+ 的 `UnsynchronizedByteArrayOutputStream.builder()` → 生成/读取 xlsx 时 `NoClassDefFoundError`/`NoSuchMethodError`。已去重并升到 2.18.0。
2. **commons-compress 1.21 vs POI 5.4.1**：minio 8.5.2 带的是 1.21，`ZipArchiveOutputStream.putArchiveEntry` 老签名不兼容 → 显式声明 1.27.1。
3. **POI 的 zip 炸弹阈值会误杀正常 Excel**：默认 `MIN_INFLATE_RATIO=0.01`，Excel/WPS 另存的 xlsx 常常低于阈值直接抛 `Zip bomb detected!`。已在 `DecisionTableGuard` 静态块里放宽到 0.001。
4. **`@EnableWebMvc` 会关掉静态资源自动配置**：`classpath:/static/` 直接 404，需要自己 `addResourceHandlers`；另外本工程有 `@GetMapping("/{sex}")` 单段通配，页面不能放根路径，故挂在 `/rule/ui/**`。
5. **`forward:` / `redirect:` 视图名在 @EnableWebMvc 下解析不了**（`Could not resolve view with name`），`/rule/ui` 短地址改成 302 + Location（`ServletUriComponentsBuilder` 生成，自动带 context-path）。
6. **同类型 + 同参数重复规则**：两条 LHS 完全相同的规则谁先点火由引擎决定，结果不可预期 → 发布时直接拒绝，要求覆盖同一业务键。
7. **mvel 方言下 `update()` 的收敛性**：RHS 里除了 setter 还调用别的方法（如 `addMessage`）时，Drools 推导不出属性掩码 → `update($o)` 退化成全量更新 → 同一条规则反复点火。因此本次模板设计成 RHS 只调 setter、累加规则都带状态守卫（`discount == 0.0`），提示文案由服务层按命中规则生成。详见 `ANALYSIS.md` 5.1 的 6 组对照实验。
8. **`fireAllRules(int max)` 必带上限**：规则写错时不至于把业务线程 100% CPU 挂死（`DynamicRuleEngine` 里上限 200 + 超限告警）。

## 怎么测

**一键验收脚本（只用 curl + grep，不依赖 python）**：

```bash
cd /Users/admin/Projects/spring-cloud-eureka
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.0.7+6.jdk/Contents/Home
mvn spring-boot:run &                       # 先起服务（:9008，context-path=/eurekaclient）
bash scripts/rule-demo-test.sh              # 29 项断言，跑完自动清理测试数据
# 换地址：BASE=http://host:port/ctx bash scripts/rule-demo-test.sh
```
脚本覆盖：类型元数据 → 参数预览 → 脏参数拦截 → 发布并立即生效 → 试算 → 决策表下载模板/上传试编译/发布/试算 →
伪装 xlsx 被拒 → 启停生效 → 清理回种子状态。

**手工点页面**：<http://localhost:9008/eurekaclient/rule/ui>
① 选类型填参数 →「预览 DRL」看渲染结果 →「试编译」→「发布」；② 规则列表停用/启用/删除（立刻生效）；
③ 「下载模板」→ 用 Excel/WPS 只改 B/C 两列 →「上传试编译」看会生成什么 DRL →「上传并发布」；
④ 试算看命中规则与最终应付（比如区域填"新疆"、金额 6000 → 命中运费规则，应付 6015）。

**单元测试**（不起 Spring 容器，1 秒级）：

```bash
mvn -Dtest=RuleEngineLocalTest test
```

**想验证"改动即生效"**：页面上把折扣率从 0.1 改成 0.35 → 发布 → 直接试算，金额立刻变，无需重启。

## 已验证（真实跑过，非描述）

- `mvn -Dtest=RuleEngineLocalTest test` → **Tests run: 11, Failures: 0, Errors: 0**
- HTTP：参数非法 400 带中文原因；发布 GOLD 0.25 后立刻试算 `discount=0.25 / finalAmount=750`；下载模板 → 填 BLUE 0.35 上传 → 试编译回显 DRL → 发布后 `discount=0.35 / finalAmount=650`；停用后 ruleCount 6→5 且该等级回落。
- 安全：D 列注入 400；外来结构 xlsx 400（`模板结构不匹配：第 1 行第 1 列应为 [RuleSet]`）。
- 浏览器：`/rule/ui` → 302 → 页面渲染 4 个规则类型 + 动态表单 + 3 条种子规则 + 决策表资产；点"预览 DRL"出 DRL；点"发布"→ `已发布：…（v1），生效版本 20260924-104416，引擎规则数 4`；点"试算"→ `命中规则(1): REGION_SURCHARGE_demo-region … 最终应付: 6015`。
