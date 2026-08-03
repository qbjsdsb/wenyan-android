# 文研App 付费商业化设计（试用 3 天 → 全功能买断）

> 日期：2026-08-03
> 状态：设计评审稿（待用户确认后实施）
> 模式：**免费试用 3 天 → 一次性买断 ¥29.9 永久解锁（买断式）**
> 收款方式：发卡平台自动发码（人工收款兜底）
> **AI 成本：用户自带 API Key（BYOK）——你零 AI 成本**（App 已有 ApiConfigScreen 供用户配置）

## 1. 商业模式总览

```
用户首次启动
   │
   ├─ 进入 3 天试用期（全功能可用，显示剩余天数）
   │    AI 助手：用户自行配置 API Key（BYOK）即可使用
   │
   ├─ 3 天内：全功能可用（知识点/卡片/错题本/真题/论述题/AI 助手）
   │
   ├─ 3 天后未激活：
   │    App 启动进入「激活页」（购买墙）
   │    核心功能锁定，仅保留：激活入口 + 购买指引
   │
   ├─ 用户购买（发卡平台 ¥29.9）→ 获得永久激活码 → 输入 App
   │
   └─ 验证成功 → 全功能永久解锁
```

**成本结构**：
- 内容（知识点/卡片/真题/论述题/错题本）：你已投入，¥29.9 一次回收
- AI 助手：用户自带 API Key（DeepSeek/通义/智谱/月之暗面/自定义），调用成本由用户承担 → **你无 AI 持续成本**
- 因此 ¥29.9 买断无需 AI 限额（用户自己花钱，用多少是自己的事）

**设计原则**：
- 试用 3 天让用户完整体验价值（考研用户低频，3 天可体验全功能）
- 试用结束后明确付费墙（无免费功能，买断软件式）
- 激活码数字签名防伪（无法伪造、可绑定设备防共享）

## 2. 会员状态机

| 状态 | 条件 | 行为 |
|------|------|------|
| `TRIAL` | `now < install_time + 3天` | 全功能可用，提示"试用中·剩余 X 天" |
| `ACTIVE` | 激活码有效且未过期 | 全功能可用，显示"会员已激活" |
| `EXPIRED` | 激活码过期（限期卡到期） | 回到付费墙 |
| `LOCKED` | 试用结束且未激活 | 启动进激活页，核心功能锁定 |

## 3. 激活码系统设计

### 3.1 格式

```
WENY-XXXXX-XXXXX-XXXXX
（前缀 4 + 三组 5 位 Base32，共 19 字符，去易混字符 I/O/0/1）
```

### 3.2 载荷（签名前 JSON）

> v1 只发「永久卡」（一次性买断 ¥29.9）。脚本保留 days 参数便于未来加限期卡。

```json
{
  "type": "permanent",          // permanent（未来可加 month/year）
  "issued": "2026-08-03T00:00:00Z",
  "nonce": "8f3a..."            // 随机 8 字节，防重放/防碰撞
}
```

### 3.3 签名

- 算法：**Ed25519**（Python `cryptography` 库生成/签名；App 端可用 Java/Android 内置 `Ed25519` API 或 BouncyCastle）
- 私钥：你本地留存（tools/license/private.key，**不入库**）
- 公钥：嵌入 App（`core/license/PublicKey.kt` 常量），验证签名
- 激活码 = `Base32(载荷JSON + 签名)` → 分组展示

**安全边界**：
- 无私钥无法伪造激活码（公钥只验证）
- 激活码绑定首次激活设备（见 3.4），一码不能多设备同时用

### 3.4 设备绑定

- 首次激活时记录设备 ID（`Settings.Secure.ANDROID_ID`，取哈希）
- 同一激活码换设备：**允许迁移**（旧设备离线时，新设备激活需原设备"解绑"或等待？）
  - 简化版：激活码激活后绑定设备，换设备提示"该码已绑定其他设备"
  - 后续可加"解绑"机制（服务端才支持，侧载阶段先接受）

### 3.5 生成器脚本

```
tools/license/
├── gen_keypair.py      # 生成 Ed25519 私钥/公钥（一次）
├── gen_licenses.py     # 批量生成永久激活码 → licenses.csv
└── verify_license.py   # 本地校验工具（调试用）

用法：
  python gen_keypair.py                       # 输出 private.key / public.key
  python gen_licenses.py --batch 200 --out permanent.csv
  python verify_license.py --code WENY-XXXX-XXXX-XXXX
```

`licenses.csv` 可直接导入发卡平台库存（列：激活码 / 类型 / 状态）。

## 4. 试用期实现

### 4.1 存储（DataStore Preferences，`core/license` 模块）

```kotlin
data class LicenseState(
  val installTime: Long,       // 首次启动时间戳（试用计时起点）
  val activated: Boolean,
  val licenseType: String?,    // month/year/permanent
  val expiryAt: Long?,         // 激活码到期时间
  val deviceIdHash: String?,   // 激活绑定的设备
)
```

### 4.2 试用判定

```kotlin
fun isInTrial(state: LicenseState, now: Long): Boolean =
    !state.activated && now - state.installTime < TRIAL_DURATION_MS
// TRIAL_DURATION_MS = 3 * 24 * 3600 * 1000
```

### 4.3 防重置（侧载局限）

- 试用时间戳存 DataStore（卸载重装会重置）——**接受此弱点**（侧载无法完美防重置）
- 缓解：试用状态额外存一份到**外部存储/媒体库**（应用卸载后残留），重装可检测到旧安装时间
  - 简单版：存 `MediaStore` 一张 0 字节图/文本（卸载不清）
  - 若实现复杂，先接受 DataStore 弱点，后续加服务端再补

## 5. 付费墙实现

### 5.1 进入时机

- App 启动时判定：非 TRIAL 且非 ACTIVE → 进入激活页（`Route.Activate`）
- 导航守卫：所有业务路由检查 license 状态，未解锁 → 跳转激活页

### 5.2 激活页 UI

```
[标题] 文研App · 专业版
[副标题] 免费试用已结束，买断解锁全部功能
[剩余试用天数]（试用中显示）
[定价] 一次性买断 ¥29.9（永久有效）
[购买] 跳转发卡平台链接/二维码（WebView 或外部浏览器）
[激活码输入] WENY-XXXX-XXXX-XXXX
[激活按钮] → 校验 → 成功进入 App
[常见问题] 激活码在哪买 / 换设备怎么办
```

### 5.3 各功能锁定点

| 功能 | 试用中 | 已激活 | 未解锁 |
|------|--------|--------|--------|
| 知识点浏览 | ✅ | ✅ | ❌ 弹激活墙 |
| 卡片刷题 | ✅ | ✅ | ❌ 弹激活墙 |
| 错题本 | ✅ | ✅ | ❌ 弹激活墙 |
| 真题/解析 | ✅ | ✅ | ❌ 弹激活墙 |
| 论述题/AI 助手 | ✅ | ✅ | ❌ 弹激活墙 |

> 全功能买断：未解锁时所有业务功能锁定（保留：设置页基础项、激活入口、更新检查）。
> **AI 助手**：功能门禁随买断（试用/激活可用）；API Key 由用户自行配置（BYOK，零你成本）。

### 5.4 锁定交互

- 点击锁定功能 → Snackbar/弹窗"该功能需激活后使用" → 跳激活页
- 试用中顶部/设置页常显"试用剩余 X 天"（促进转化）

## 6. UI 改动清单

| 文件/位置 | 改动 |
|-----------|------|
| `feature/settings/SettingsScreen.kt` | 新增"会员/激活"卡片（状态 + 激活入口 + 剩余天数） |
| 新增 `feature/license/ActivateScreen.kt` | 激活页（购买引导 + 激活码输入） |
| 新增 `core/license/` 模块 | LicenseRepository + LicenseValidator + 状态管理 |
| `navigation/WenyanNavHost.kt` | 激活路由 + 启动守卫 |
| 各业务 Screen 入口 | 锁定检查（未解锁弹激活墙） |
| `tools/license/*.py` | 生成器脚本 |

## 7. 发卡平台对接

1. 注册发卡平台（iDataRiver / 酷发卡 / 码速达，个人邮箱）
2. 创建商品：**文研会员·永久买断 ¥29.9**（唯一商品）
3. `python gen_licenses.py --batch 200` → `permanent.csv` → 导入平台库存
4. 平台生成购买链接/二维码 → 填入 App 激活页"购买"入口（URL 常量）
5. 用户支付 ¥29.9 → 平台自动发货激活码 → 用户输入 App 激活 → 永久解锁

**收款兜底**：平台未开通前，先人工收款 + `gen_licenses.py` 发码（模式 ①）。

## 8. 风险与缓解（2026-08-03 深度审查后）

| 风险 | 级别 | 缓解 |
|------|------|------|
| **存量用户试用期**（已发布 v0.9.26，老用户升级被锁会差评） | 🔴 | **从新版本首次启动计时**（所有用户统一 3 天）；或 v0.9.26 及以前老用户永久宽限 |
| **Ed25519 Android 兼容**（java.security 各版本支持不一） | 🔴 | **BouncyCastle lightweight API**（bcprov-jdk18on:1.84，Ed25519Verifier）；生成器 Python cryptography |
| **发卡平台跑路**（2026 监管收紧、小平台卷款） | 🔴 | 选老牌有资质平台（酷发卡/iDataRiver）；及时提现不留大额；激活码本地验证不受平台影响 |
| 卸载重装重置试用 | 🟡 | DataStore + 媒体库标记缓解；先接受 |
| 重打包破解（移除校验逻辑） | 🟡 | 混淆 + 完整性校验缓解；正式防护需服务端（后续） |
| AI BYOK 体验门槛（用户配 Key 流程） | 🟡 | 激活页/引导页给 DeepSeek 等配置教程 |
| 换设备激活码失效 | 🟢 | 一码一设备；提供人工解绑兜底 |
| 试用结束突然锁定 | 🟢 | 试用最后 1 天提示"明天到期"；到期温和引导 |
| 旧版本不触发付费墙 | 🟢 | 侧载无自动更新；靠版本更新引导（可接受） |

## 8.1 关键实现决策

- **签名算法**：Ed25519（Python `cryptography` 生成/签名；App 端 **BouncyCastle** `Ed25519Verifier` 验证）
- **试用计时起点**：**新版本首次启动**（`install_time` 在 DataStore，首启写入）——解决存量用户
- **设备 ID**：`Settings.Secure.ANDROID_ID` 哈希（Android 8+ 同签名+同用户下稳定，卸载重装不变）
- **发卡平台**：注册老牌平台 → 导入永久卡 → 及时提现

## 9. 实施范围（App 侧，纯代码零外部依赖）

1. `core/license` 模块：LicenseRepository（DataStore 状态）、LicenseValidator（Ed25519 公钥验证）、试用判定
2. 激活页 `ActivateScreen` + 导航守卫
3. 设置页会员卡片 + 状态展示
4. 各功能锁定点 + 弹窗引导
5. 生成器脚本 `tools/license/*.py`
6. 公钥常量（生成 keypair 后嵌入）
7. 全量验证 + 单测（LicenseValidator 签名验证测试）

> 该实施不依赖发卡平台/支付上线，可先完成 App 侧，随后接入发卡平台。

---

## 待确认事项（开工前）

- [x] **定价**：一次性买断 **¥29.9（永久）** —— 已确认（2026-08-03）
- [ ] 是否接受"卸载重装可重新试用"（侧载局限，服务端后补）——默认接受
- [ ] 试用期 **3 天** 确认（还是 7 天）——默认 3 天
- [ ] 激活码设备绑定：**严格一码一设备**（默认）vs 允许换设备
