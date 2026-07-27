#!/usr/bin/env bash
# scripts/setup-env.sh — 文研 App 一键环境准备脚本
#
# 适用：Linux / macOS 环境（沙箱 / TRAE Work 云端运行环境 / GitHub Actions CI）
# 不适用：Windows 本地（AGENTS.md 约束本地用 PowerShell + D 盘）
#
# 前置：先在项目根跑 `mise install`（mise.toml 已锁定 JDK 17.0.2 + Gradle 8.14.4）
#
# 用法：
#   bash scripts/setup-env.sh            # 检测并装缺失组件（默认）
#   bash scripts/setup-env.sh --check    # 仅检测，不安装（CI 用）
#   bash scripts/setup-env.sh --force    # 强制重装 SDK（调试用）
#
# 退出码：
#   0 = 全部就绪
#   1 = JDK/Gradle 缺失（需先 mise install）
#   2 = SDK 缺失且 --check 模式（CI 报警用）

set -euo pipefail

# === 配置（与项目版本要求严格对齐） ===
# 注：JDK 和 Gradle 版本由 mise.toml 锁定，本脚本只检测不安装
# 注：compileSdk/buildTools 与 app/build.gradle.kts 严格对齐
REQUIRED_PLATFORM="android-35"
REQUIRED_BUILD_TOOLS="35.0.0"
REQUIRED_CMDLINE_TOOLS_VERSION="11076708"

# === 颜色输出（CI 无 tty 时自动禁用） ===
if [[ -t 1 ]]; then
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[1;33m'
    BLUE='\033[0;34m'
    NC='\033[0m'
else
    RED=''; GREEN=''; YELLOW=''; BLUE=''; NC=''
fi

info()  { echo -e "${BLUE}[INFO]${NC} $*"; }
ok()    { echo -e "${GREEN}[ OK ]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()   { echo -e "${RED}[ERR]${NC} $*" >&2; }

# === 状态检测 ===

check_java() {
    if command -v java &>/dev/null; then
        local version
        # 注意：java -version 第一行可能是 "Picked up JAVA_TOOL_OPTIONS: ..."，
        # 真正的版本行以 "openjdk version" 或 "java version" 开头
        version=$(java -version 2>&1 | grep -E "^(openjdk|java) version" | head -1 | awk -F\" '{print $2}')
        if [[ "$version" == 17.* ]]; then
            ok "JDK 17 已安装: $version"
            return 0
        else
            warn "JDK 版本非 17: $version（项目要求 17.0.2，由 mise.toml 锁定）"
            return 1
        fi
    else
        err "未找到 java 命令"
        return 1
    fi
}

check_gradle() {
    if command -v gradle &>/dev/null; then
        local version
        version=$(gradle --version 2>/dev/null | grep "^Gradle" | awk '{print $2}')
        if [[ "$version" == "8.14.4" ]]; then
            ok "Gradle 8.14.4 已安装"
            return 0
        else
            warn "Gradle 版本非 8.14.4: $version（项目要求 8.14.4，由 mise.toml 锁定）"
            return 1
        fi
    else
        err "未找到 gradle 命令"
        return 1
    fi
}

detect_sdk_path() {
    # 检测顺序：
    # 1. ANDROID_HOME 环境变量（CI/沙箱常用）
    # 2. ANDROID_SDK_ROOT 环境变量（兼容别名）
    # 3. /opt/android-sdk（Trae 沙箱预装路径）
    # 4. $HOME/android-sdk（TRAE Work install 脚本默认路径）
    # 5. $HOME/Android/Sdk（Linux 手动安装约定）
    if [[ -n "${ANDROID_HOME:-}" && -d "$ANDROID_HOME" ]]; then
        echo "$ANDROID_HOME"
        return 0
    elif [[ -n "${ANDROID_SDK_ROOT:-}" && -d "$ANDROID_SDK_ROOT" ]]; then
        echo "$ANDROID_SDK_ROOT"
        return 0
    elif [[ -d "/opt/android-sdk" ]]; then
        echo "/opt/android-sdk"
        return 0
    elif [[ -d "$HOME/android-sdk" ]]; then
        echo "$HOME/android-sdk"
        return 0
    elif [[ -d "$HOME/Android/Sdk" ]]; then
        echo "$HOME/Android/Sdk"
        return 0
    else
        return 1
    fi
}

check_android_sdk() {
    local sdk_path
    sdk_path=$(detect_sdk_path) || {
        warn "未检测到 Android SDK 安装路径"
        return 1
    }

    if [[ -d "$sdk_path/platforms/$REQUIRED_PLATFORM" ]] && \
       [[ -d "$sdk_path/build-tools/$REQUIRED_BUILD_TOOLS" ]]; then
        ok "Android SDK 35 + build-tools 35.0.0 已安装: $sdk_path"
        return 0
    else
        warn "Android SDK 路径存在但缺组件: $sdk_path"
        info "  缺失: platforms/$REQUIRED_PLATFORM 或 build-tools/$REQUIRED_BUILD_TOOLS"
        return 1
    fi
}

install_android_sdk() {
    # 优先复用已存在的 SDK 目录，避免覆盖
    local sdk_path
    sdk_path=$(detect_sdk_path 2>/dev/null || echo "${ANDROID_HOME:-$HOME/android-sdk}")

    info "开始安装 Android SDK 到: $sdk_path"

    mkdir -p "$sdk_path/cmdline-tools"
    cd "$sdk_path/cmdline-tools"

    if [[ ! -d "latest" ]]; then
        info "下载 Android cmdline-tools (version $REQUIRED_CMDLINE_TOOLS_VERSION)..."
        curl -sSL "https://dl.google.com/android/repository/commandlinetools-linux-${REQUIRED_CMDLINE_TOOLS_VERSION}_latest.zip" -o cmdline-tools.zip
        unzip -q cmdline-tools.zip
        mv cmdline-tools latest
        rm cmdline-tools.zip
    else
        ok "cmdline-tools 已存在，跳过下载"
    fi

    export ANDROID_HOME="$sdk_path"
    export ANDROID_SDK_ROOT="$sdk_path"
    export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

    info "接受 SDK 许可..."
    yes | sdkmanager --licenses > /dev/null 2>&1 || true

    info "安装 platform-tools + platforms;android-35 + build-tools;35.0.0..."
    sdkmanager "platform-tools" "platforms;$REQUIRED_PLATFORM" "build-tools;$REQUIRED_BUILD_TOOLS" > /dev/null

    ok "Android SDK 安装完成: $sdk_path"
}

write_local_properties() {
    local sdk_path
    sdk_path=$(detect_sdk_path) || {
        err "无法写 local.properties：Android SDK 未安装"
        return 1
    }

    local project_root
    project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
    local properties_file="$project_root/local.properties"

    if [[ -f "$properties_file" ]]; then
        local existing_sdk
        existing_sdk=$(grep "^sdk.dir=" "$properties_file" 2>/dev/null | cut -d= -f2- || true)
        if [[ "$existing_sdk" == "$sdk_path" ]]; then
            ok "local.properties 已存在且 SDK 路径正确: $properties_file"
            return 0
        fi
        warn "local.properties 存在但 SDK 路径过期，将覆盖"
    fi

    info "写入 local.properties: sdk.dir=$sdk_path"
    cat > "$properties_file" <<EOF
## Auto-generated by scripts/setup-env.sh
## 不要手动修改 — 改了也会被覆盖
## 此文件已在 .gitignore 中，不会入仓库
sdk.dir=$sdk_path
EOF
    ok "local.properties 已生成: $properties_file"
}

print_summary() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}环境状态总览${NC}"
    echo -e "${BLUE}========================================${NC}"

    local java_ok=false gradle_ok=false sdk_ok=false props_ok=false

    check_java 2>/dev/null && java_ok=true
    check_gradle 2>/dev/null && gradle_ok=true
    check_android_sdk 2>/dev/null && sdk_ok=true

    local sdk_path
    sdk_path=$(detect_sdk_path 2>/dev/null || echo "未安装")
    local project_root
    project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
    [[ -f "$project_root/local.properties" ]] && props_ok=true

    echo ""
    echo "JDK 17.0.2:          $([[ $java_ok == true ]] && echo '✓' || echo '✗')"
    echo "Gradle 8.14.4:       $([[ $gradle_ok == true ]] && echo '✓' || echo '✗')"
    echo "Android SDK 35:      $([[ $sdk_ok == true ]] && echo '✓' || echo '✗')"
    echo "  路径:               $sdk_path"
    echo "local.properties:    $([[ $props_ok == true ]] && echo '✓' || echo '✗')"

    echo ""
    if [[ $java_ok == true && $gradle_ok == true && $sdk_ok == true && $props_ok == true ]]; then
        ok "环境就绪！下一步：gradle assembleDebug"
    else
        warn "环境未就绪，请按上述提示修复"
    fi
}

# === 主流程 ===

main() {
    local mode="${1:-}"

    if [[ "$mode" == "--help" || "$mode" == "-h" ]]; then
        sed -n '2,15p' "$0"
        exit 0
    fi

    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}文研 App 环境准备脚本${NC}"
    echo -e "${BLUE}========================================${NC}"

    # 1. JDK 检测（mise.toml 锁定，本脚本不安装）
    info "1/4 检测 JDK 17..."
    if ! check_java; then
        err "JDK 17 缺失。请在项目根目录运行：mise install"
        err "（mise.toml 已锁定 java=17.0.2，mise 会自动安装正确版本）"
        exit 1
    fi

    # 2. Gradle 检测（mise.toml 锁定，本脚本不安装）
    info "2/4 检测 Gradle 8.14.4..."
    if ! check_gradle; then
        err "Gradle 8.14.4 缺失。请在项目根目录运行：mise install"
        exit 1
    fi

    # 3. Android SDK 检测/安装
    info "3/4 检测 Android SDK 35 + build-tools 35.0.0..."
    if ! check_android_sdk; then
        if [[ "$mode" == "--check" ]]; then
            warn "Android SDK 缺失（--check 模式不安装）"
            exit 2
        fi
        if [[ "$mode" == "--force" ]]; then
            warn "--force 模式：重装 SDK"
            rm -rf "$HOME/android-sdk" 2>/dev/null || true
        fi
        install_android_sdk
    fi

    # 4. 写 local.properties
    info "4/4 生成 local.properties..."
    write_local_properties

    # 5. 总览
    print_summary
}

main "$@"
