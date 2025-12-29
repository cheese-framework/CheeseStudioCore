#项目的配置结构可能会随版本更新发生变化。为了确保配置文件正确，建议您定期访问(https://cheese.codeocean.net/other/project-information.html)获取最新的配置信息。
# 平台绑定
platform= "${platform}"
# 语法绑定
bindings= "${bindings}"
# ui类型
ui = "${ui}"
# 入口文件
main = "main"

[app]# 信息配置
# app版本号
version = "0.0.1"
# app包名
package = "${pkg}"
# app名
name = "${projectname}"

[build]# 构建配置
# 热更新 - 默认关闭
# hot = { version = "0.0.1" , url = "http://127.0.0.1:7777/update"}
# 架构支持 更改此项编译解包需要打开 - 默认关闭
# ndk = ["x86_64", "arm64-v8a"]
# 排除内置库 更改此项编译解包需要打开 - 默认关闭
# excludeLib = ["yolo", "opencv", "paddleocr", "onnx", "ddddocr", "mlkitocr"]



