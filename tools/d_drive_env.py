"""D盘环境配置模块 - 将所有缓存/临时文件/配置都重定向到D盘工作文件夹。

使用方法：在其他脚本开头导入此模块，或直接运行此脚本设置环境变量。

作用：
  1. 将MinerU配置文件指向 D:\\wenyan\\.config\\mineru.json
  2. 将模型缓存指向 D:\\wenyan\\.cache\\mineru_models
  3. 将pip缓存指向 D:\\wenyan\\.cache\\pip
  4. 将临时目录指向 D:\\wenyan\\temp
  5. 将HuggingFace缓存指向 D:\\wenyan\\.cache\\huggingface
  6. 将PyTorch缓存指向 D:\\wenyan\\.cache\\torch
"""

import os


def setup_d_drive_env():
    """配置所有环境变量到D盘工作文件夹。

    必须在任何使用MinerU/模型下载的代码之前调用。
    """
    # ===== D盘目录定义 =====
    D_ROOT = r"D:\wenyan"
    D_CONFIG = os.path.join(D_ROOT, ".config")
    D_CACHE = os.path.join(D_ROOT, ".cache")
    D_TEMP = os.path.join(D_ROOT, "temp")

    # 各类缓存子目录
    D_MODELSCOPE = os.path.join(D_CACHE, "modelscope")
    D_HUGGINGFACE = os.path.join(D_CACHE, "huggingface")
    D_TORCH = os.path.join(D_CACHE, "torch")
    D_PIP = os.path.join(D_CACHE, "pip")
    D_MINERU_MODELS = os.path.join(D_CACHE, "mineru_models")

    # ===== 创建所有目录 =====
    for path in [
        D_CONFIG, D_CACHE, D_TEMP,
        D_MODELSCOPE, D_HUGGINGFACE, D_TORCH, D_PIP, D_MINERU_MODELS,
    ]:
        os.makedirs(path, exist_ok=True)

    # ===== 设置环境变量 =====
    # MinerU配置文件（绝对路径，避免写到C盘用户主目录）
    os.environ["MINERU_TOOLS_CONFIG_JSON"] = os.path.join(D_CONFIG, "mineru.json")

    # MinerU模型源（使用modelscope，国内速度快）
    os.environ["MINERU_MODEL_SOURCE"] = "modelscope"

    # ModelScope模型缓存
    os.environ["MODELSCOPE_CACHE"] = D_MODELSCOPE
    os.environ["MODELSCOPE_MODULES_CACHE"] = os.path.join(D_MODELSCOPE, "modules")

    # HuggingFace缓存
    os.environ["HF_HOME"] = D_HUGGINGFACE
    os.environ["TRANSFORMERS_CACHE"] = os.path.join(D_HUGGINGFACE, "transformers")
    os.environ["HF_DATASETS_CACHE"] = os.path.join(D_HUGGINGFACE, "datasets")

    # PyTorch缓存
    os.environ["TORCH_HOME"] = D_TORCH

    # pip缓存
    os.environ["PIP_CACHE_DIR"] = D_PIP

    # 临时目录（避免写C盘AppData\Local\Temp）
    os.environ["TEMP"] = D_TEMP
    os.environ["TMP"] = D_TEMP

    # 用户主目录重定向（让os.path.expanduser('~')返回D盘路径）
    # 这是关键！modelscope/huggingface等库通过~确定默认缓存位置
    D_USERPROFILE = os.path.join(D_ROOT, ".userhome")
    os.makedirs(D_USERPROFILE, exist_ok=True)
    os.environ["USERPROFILE"] = D_USERPROFILE
    os.environ["HOME"] = D_USERPROFILE
    os.environ["HOMEPATH"] = D_USERPROFILE

    # 返回配置信息（供调试用）
    return {
        "MINERU_TOOLS_CONFIG_JSON": os.environ["MINERU_TOOLS_CONFIG_JSON"],
        "MINERU_MODEL_SOURCE": os.environ["MINERU_MODEL_SOURCE"],
        "MODELSCOPE_CACHE": os.environ["MODELSCOPE_CACHE"],
        "HF_HOME": os.environ["HF_HOME"],
        "TORCH_HOME": os.environ["TORCH_HOME"],
        "PIP_CACHE_DIR": os.environ["PIP_CACHE_DIR"],
        "TEMP": os.environ["TEMP"],
        "USERPROFILE": os.environ["USERPROFILE"],
        "HOME": os.environ["HOME"],
    }


def print_env():
    """打印当前环境变量配置。"""
    config = setup_d_drive_env()
    print("=== D盘环境变量配置 ===")
    for key, value in config.items():
        print(f"  {key} = {value}")
    print()


# 模块导入时自动执行配置
setup_d_drive_env()


if __name__ == "__main__":
    print_env()
