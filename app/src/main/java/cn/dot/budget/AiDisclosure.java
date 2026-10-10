package cn.dot.budget;

/** User-visible disclosure derives only from the outbound image and fixed destination. */
public final class AiDisclosure {
 private AiDisclosure(){}
 public static boolean maySend(boolean reviewed,boolean current){return reviewed&&current;}
 public static String text(AiRequestContract request){if(request==null||request.text==null)throw new IllegalArgumentException("Missing typed content");return "接收方：DeepSeek 官方 API\n"+DeepSeekTransport.ENDPOINT+"\n模型："+AiPayloadBuilder.MODEL+"\n用途："+("food".equals(request.mode)?"整理本次食物文字并估算份量、热量范围":"整理本次文字为待核对账目")+"。\n仅发送下方完整文字和固定识别说明；不附加草稿、已保存记录、身体资料、历史消息或当前日期。缺少的日期在本机核对时补充。\n请先移除完整卡号、银行账号、密码、验证码和 API 密钥。\nAPI 可能收费，由你的 DeepSeek 账户承担；未勾选记住时仅授权本次文字发送。取消不会发送；发送后不能撤回，失败也可能已计费。不会自动重试。结果需核对后才能保存。";}
 public static String image(AiRequestContract request,int width,int height,int bytes){if(request==null)throw new IllegalArgumentException("Missing request");return image(request,width,height,bytes,"food".equals(request.mode)?"image/jpeg":"image/png");}
 public static String image(AiRequestContract request,int width,int height,int bytes,String mime){if(request==null||width<1||height<1||width>AiImagePolicy.MAX_EDGE||height>AiImagePolicy.MAX_EDGE||bytes<1||bytes>AiBoundedBytes.MAX_BYTES)throw new IllegalArgumentException("图片预览无效");return
  "接收方：DeepSeek 官方 API\n"+DeepSeekTransport.ENDPOINT+"\n模型："+AiPayloadBuilder.MODEL+"\n用途："+("food".equals(request.mode)?"估算这一张食物照片的名称、份量和热量范围":"提取这一张账单截图的逐笔交易，供本地核对")+"\n本次仅发送下方处理后的图片及固定识别说明；不发送已有账目、饮食历史、体重或身体资料。\n实际发送："+width+" × "+height+" 像素，"+bytes+" 字节，"+("image/png".equals(mime)?"PNG":"JPEG")+"。\n图片已自动压缩到小于 10 MB；账单优先保留清晰字迹，请核对处理后文字，过长账单应分段截图。\n已重新编码去除原文件的位置、设备等附加元数据；图片上可见的姓名、卡号、交易、人脸等仍会发送。发送前必须在相册移除完整卡号、银行账号、密码、验证码和 API 密钥等凭据，仅保留本次记账或食物识别所需内容，再重新选择。应用不会自动遮盖图片文字。\nAPI 可能收费，由你的 DeepSeek 账户承担；未勾选记住时仅授权本次发送。取消不会发送；发送后不能撤回，取消或失败也可能已计费。应用不会自动重试。\n结果可能有误，只生成未保存的核对草稿。";}
}
