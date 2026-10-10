package cn.dot.budget;

import java.net.SocketTimeoutException;
import java.io.IOException;

/** Stable recovery instructions never expose remote response bodies, prompts or credentials. */
public final class AiErrors {
 public static final class Info {
  public final String code,message,action;
  private Info(String code,String message,String action){this.code=code;this.message=message;this.action=action;}
 }
 private AiErrors(){}
 public static Info from(Throwable failure){
  String code=failure instanceof DeepSeekTransport.Failure?((DeepSeekTransport.Failure)failure).code:failure instanceof SocketTimeoutException?"timeout":failure instanceof IllegalArgumentException?"parse":failure instanceof IOException?"network":"server";
  return forCode(code);
 }
 public static Info forCode(String code){switch(code){
  case "auth":return new Info(code,"API 密钥无效或权限不足。请重新配置密钥后，再次选择输入并确认发送。","configure");
  case "balance":return new Info(code,"DeepSeek 账户余额不足。请先检查账户余额，再自行重新选择输入。","account");
  case "rate":return new Info(code,"请求过于频繁。请稍后自行重试，每次发送都需重新确认。","wait");
  case "timeout":return new Info(code,"连接或识别超时。可继续手动记录，或检查网络后重新选择输入。","reselect");
  case "input":return new Info(code,"无法读取输入。请选择清晰且不超过 10 MB 的 PNG、JPEG 或 WebP；过大或过长的账单请分段截图，应用不会缩小文字。","reselect");
  case "parse":return new Info(code,"结果为空、不完整或格式无效，原草稿保持不变。请缩小输入范围或手动补充。","edit");
  case "cancelled":return new Info(code,"本次识别已取消，未保存数据。","none");
  case "network":return new Info(code,"无法连接识别服务。请检查网络，或继续使用本地手动记录。","reselect");
  default:return new Info("server","识别服务暂不可用。可继续手动记录，稍后自行重新选择输入。","reselect");
 }}
}
