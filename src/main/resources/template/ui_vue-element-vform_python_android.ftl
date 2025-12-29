from cheese_core import *

print(r"""
▄████▄   ██░ ██ ▓█████ ▓█████   ██████ ▓█████
▒██▀ ▀█  ▓██░ ██▒▓█   ▀ ▓█   ▀ ▒██    ▒ ▓█   ▀
▒▓█    ▄ ▒██▀▀██░▒███   ▒███   ░ ▓██▄   ▒███
▒▓▓▄ ▄██▒░▓█ ░██ ▒▓█  ▄ ▒▓█  ▄   ▒   ██▒▒▓█  ▄
▒ ▓███▀ ░░▓█▒░██▓░▒████▒░▒████▒▒██████▒▒░▒████▒
░ ░▒ ▒  ░ ▒ ░░▒░▒░░ ▒░ ░░░ ▒░ ░▒ ▒▓▒ ▒ ░░░ ▒░ ░
░  ▒    ▒ ░▒░ ░ ░ ░  ░ ░ ░  ░░ ░▒  ░ ░ ░ ░  ░
░         ░  ░░ ░   ░      ░   ░  ░  ░     ░
░ ░       ░  ░  ░   ░  ░   ░  ░      ░     ░  ░
░

Cheese 提供高扩展性，助您快速开发自动化测试脚本！

支持大多数 Pypi 第三方包，涵盖超过 200 万个包，包括：
- numpy
- opencv
- 及更多...

您可以访问 [pypi官网](https://pypi.org) 搜索并选择所需包。
根据每个包页面的安装提示，轻松将其添加到您的 Cheese 项目中，直接在项目中使用。
""")



def ui_callback(e) -> int:
        if e.getId() == "btn1":
            print("Case 1")
        elif e.getId() == "btn2":
            print("Case 2")
        elif e.getId() == "btn3":
            print("Case 3")
        else:
            print("Default case")
        return 0


def main():
    webview.ui(ui_callback)

