# v4.0.1 AI工作流设计与隐私边界

本版采用任务内可编辑草稿，而非通用聊天或健康建议。保留现有四个同级目的地，把状态、纠错、撤回本地修改和重试放在任务上下文中。模糊材质主要用于导航，内容使用清楚实色背景；保留48px操作目标、较高文字对比、降低动态与高对比回退。

设计参考：Material 3 https://m3.material.io/ ；Android导航 https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns ；Apple生成式AI交互 https://developer.apple.com/design/human-interface-guidelines/generative-ai ；材质 https://developer.apple.com/design/human-interface-guidelines/materials 。参考不等于已通过平台认证或像素验收。

## 发送范围
固定官方DeepSeek Chat Completions接口和deepseek-flash，非流式JSON对象输出，关闭thinking；无需迁移到beta工具或Responses接口。JSON格式保证不替代本地严格验证。服务端只收到固定任务提示和当次明确同意的图片/文字，不包含内部请求身份、账目历史、体重、资料或隐藏日期上下文。模型没有工具执行权限。

官方接口：https://api-docs.deepseek.com/api/create-chat-completion/ ；视觉说明：https://api-docs.deepseek.com/guides/vision/ 。图片采用更保守的本地大小/内存限制：输出严格小于10,000,000字节，包含Base64的完整请求最多15,000,000字节，低于官方32MiB单张内联图片和48MiB请求限制。原图最多128MiB，通过有界磁盘流和采样解码处理；这不是任意大文件支持。无法保留可读账单时需要裁剪/分段。限制核对于2026-10-10。原生凭据框与可选Keystore加密存储不向JavaScript提供密钥。

## 费用与留存
首次同意可主动记住具体输入类别的发送授权；未记住时每次询问。授权只覆盖本人主动发起识别的该类选定内容，可在本机撤销，不包含后台收集、历史或身体资料。旧版单次确认不会迁移为长期授权。手动重试仍明确提示可能再次收费。取消只能尽力中止，不能撤回已发送数据，也不能保证免于计费。没有自动重试或自动调用模型修复。服务端可能缓存内容，应用不能承诺零保留。参考：https://api-docs.deepseek.com/guides/kv_cache/ 。实际收费遵循本人DeepSeek账户，不硬编码不断变化的价格。

## 本地决定权
请求结果只构成草稿。金额转整数分、日期补全、交易类型收支、食物份量缩放、标签换算及热量规划由本地规则完成。未知事实保持未解决。AI规范保存网关仅允许预期新增财务或饮食行，不能修改既有历史、身体资料、运动、体重、预算设置或常用模板。

同意、取消、输入版本和页面生命周期都有身份校验。重试使用冻结的获准输入并创建新请求身份。原始提示、图片、关联身份和草稿元数据不进入备份；可明确结束会话。确认后的规范账目/食物名称、金额/热量和本人接受的备注属于正常记录，会随备份保存。

## 验证边界
所有服务调用测试使用合成输入与模拟传输，没有真实API密钥、用户图片或付费调用。DOM、源码和原生模拟测试不等于Android真机、真实屏幕阅读器、图片方向/像素或服务端端到端验证。这些限制在最终报告中保留。
