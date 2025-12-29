const core = require('cheese-js');
const xml = core.ui.xml;

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

let button= xml.getRunActivity().findViewById(xml.getID("t2"));
button.setOnClickListener(() => {
console.log("我是id是t2的按钮，我被点击了")
});