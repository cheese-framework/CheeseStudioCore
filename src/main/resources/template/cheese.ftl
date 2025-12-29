#项目的配置结构可能会随版本更新发生变化。为了确保配置文件正确，建议您定期访问(https://cheese.codeocean.net/other/project-information.html)获取最新的配置信息。
# 平台绑定
platform= "${platform}"
# 语法绑定
bindings= "${bindings}"
# ui类型
ui = "${ui}"
# 入口文件
main = "main"
# 项目依赖 需要开启使用jvm才会自动下载解析依赖 更多第三方库请访问【Maven仓库】:https://mvnrepository.com
dependencies = [
  "net.codeocean.cheese:demo:9.9.9"
]

# pip项目依赖 仅当项目语法绑定为python才会生效 更多第三方库请访问【Chaquo仓库】：https://chaquo.com/pypi-13.1
pip = [
"numpy"
]

[app]# 信息配置
# app版本号
version = "0.0.1"
# app包名
package = "${pkg}"
# app名
name = "${projectname}"
# 无障碍服务名称
accessible_service_name = "cheese"
# 无障碍服务描述
accessible_service_desc = "cheese"
# 输入法名称
inputmethod_service_name = "cheese"
# 权限清单
permissions = [
"android.permission.SYSTEM_ALERT_WINDOW",
]

[build]# 构建配置
# 热更新 - 默认关闭
# hot = { version = "0.0.1" , url = "http://127.0.0.1:7777/update"}
# 代码保护 推荐打包后的app再次使用如360、腾讯等第三方加固 - 默认关闭 模式：1.obfuscator 将js脚本混淆 2.cloak：汇编级别保护，将js脚本直接编译为机器码并进一步加密 参数(-a 目标架构)： -a x86,x86_64,arm64-v8a,armeabi-v7a
# protection = "obfuscator"
# 架构支持 更改此项编译解包需要打开 - 默认关闭
# ndk = ["x86_64", "arm64-v8a"]
# 排除内置库 更改此项编译解包需要打开 - 默认关闭
# excludeLib = ["yolo", "opencv", "paddleocr", "onnx", "ddddocr", "mlkitocr"]
# 使用jvm - 默认关闭
useJvm = false



