package cn.dot.budget;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Production endpoint is fixed; no redirects, retries, logging, or third-party relay. */
public final class DeepSeekTransport {
 public static final class Failure extends IOException {public final String code;public Failure(String code,String message){super(message);this.code=code;}}
 public static final int MAX_REQUEST_BYTES=15_000_000;
 public static final String ENDPOINT="https://api.deepseek.com/chat/completions";
 private volatile HttpURLConnection active;
 private volatile boolean cancelled;
 public String send(String key,String payload)throws IOException{return sendTo(new URL(ENDPOINT),key,payload);}
 String sendTo(URL url,String key,String payload)throws IOException{
  if(cancelled)throw new Failure("cancelled","已取消");
  if(payload==null||payload.isEmpty()||payload.length()>MAX_REQUEST_BYTES)throw new Failure("input","图片请求过大");
  byte[] bytes=payload.getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_REQUEST_BYTES)throw new Failure("input","图片请求过大");
  HttpURLConnection c=(HttpURLConnection)url.openConnection();synchronized(this){if(cancelled){c.disconnect();throw new Failure("cancelled","已取消");}active=c;}
  try{
   c.setRequestMethod("POST");c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(120000);c.setDoOutput(true);
   c.setRequestProperty("Authorization","Bearer "+key);c.setRequestProperty("Content-Type","application/json; charset=utf-8");
   c.setFixedLengthStreamingMode(bytes.length);
   if(cancelled)throw new Failure("cancelled","已取消");
   try(OutputStream out=c.getOutputStream()){if(cancelled)throw new Failure("cancelled","已取消");out.write(bytes);}
   if(cancelled)throw new Failure("cancelled","已取消");
   int status=c.getResponseCode();if(status!=200){if(status==401||status==403)throw new Failure("auth","API 密钥无效或没有权限，请检查配置");if(status==402)throw new Failure("balance","DeepSeek 账户余额不足");if(status==429)throw new Failure("rate","请求过于频繁，请稍后重试");if(status>=300&&status<400)throw new Failure("server","服务返回重定向，已停止以保护密钥");throw new Failure("server","识别服务暂不可用（HTTP "+status+"），未写入账目");}
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(cancelled)throw new Failure("cancelled","已取消");if(out.size()+n>2000000)throw new Failure("parse","识别响应过大");out.write(b,0,n);}}
   if(cancelled)throw new Failure("cancelled","已取消");return out.toString("UTF-8");
  }finally{c.disconnect();synchronized(this){if(active==c)active=null;}}
 }
 public void cancel(){HttpURLConnection c;synchronized(this){cancelled=true;c=active;}if(c!=null)c.disconnect();}
}
