const core = require('cheese-js');
const webview = core.webview;
const toolwindow = core.toolwindow;
const thread = new core.thread;

console.log(`
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
░

Cheese 提供高扩展性，助您快速开发自动化测试脚本！
`);

console.log("拖拽可视化 UI 在线生成网站： [vform666.com](https://vform666.com)");

console.log("现在，开始监听 UI 按钮点击事件，并触发相应操作。");

webview.ui((e) => {
    switch (String(e.id)) {
        case 'btn1':
            toolwindow.floatingConsole().show()
            break;
        case 'btn2':  //耗时任务放到线程调度
         let th =thread.create(() => {
         console.log(webview.window("input63059"))
         console.log(webview.window("textarea81572"))
         console.log(webview.window("radio80266"))
         console.log(webview.window("checkbox13588"))
         console.log(webview.window("select91051"))
         console.log(webview.window("date15598"))
         console.log(webview.window("slider47455"))
         console.log(webview.window("switch74528"))
         })
         console.log(th.getId())
           break;
        default:
            console.log("Unknown:", e.id);
            break;
    }
    return 0  //返回给vue的结果
})

