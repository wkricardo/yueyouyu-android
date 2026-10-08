package cn.dot.budget;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Production endpoint is fixed; no redirects, retries, logging, or third-party relay. */
public final class DeepSeekTransport {
 public static final String ENDPOINT="https://api.deepseek.com/chat/completions";
 private volatile HttpURLConnection active;
 private volatile boolean cancelled;
 public String send(String key,String payload)throws IOException{return sendTo(new URL(ENDPOINT),key,payload);}
 String sendTo(URL url,String key,String payload)throws IOException{
  if(cancelled)throw new IOException("已取消");
  HttpURLConnection c=(HttpURLConnection)url.openConnection();active=c;
  try{
   c.setRequestMethod("POST");c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(120000);c.setDoOutput(true);
   c.setRequestProperty("Authorization","Bearer "+key);c.setRequestProperty("Content-Type","application/json; charset=utf-8");
   byte[] bytes=payload.getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);
   try(OutputStream out=c.getOutputStream()){out.write(bytes);}
   int status=c.getResponseCode();if(status!=200){if(status==401||status==403)throw new IOException("API 密钥无效或没有权限，请检查配置");if(status==402)throw new IOException("DeepSeek 账户余额不足");if(status==429)throw new IOException("请求过于频繁，请稍后重试");if(status>=300&&status<400)throw new IOException("服务返回重定向，已停止以保护密钥");throw new IOException("识别服务暂不可用（HTTP "+status+"），未写入账目");}
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(cancelled)throw new IOException("已取消");if(out.size()+n>2000000)throw new IOException("识别响应过大");out.write(b,0,n);}}
   if(cancelled)throw new IOException("已取消");return out.toString("UTF-8");
  }finally{c.disconnect();active=null;}
 }
 public void cancel(){cancelled=true;HttpURLConnection c=active;if(c!=null)c.disconnect();}
}
