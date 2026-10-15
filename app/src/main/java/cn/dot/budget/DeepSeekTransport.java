package cn.dot.budget;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Production endpoint is fixed; no redirects, retries, logging, or third-party relay. */
public final class DeepSeekTransport {
 public static final class Failure extends IOException {public final String code;public Failure(String code,String message){super(message);this.code=code;}}
 public static final int MAX_REQUEST_BYTES=15_000_000;
 public static final long REQUEST_TIMEOUT_MILLIS=120_000,UPLOAD_TIMEOUT_MILLIS=30_000;
 public static final String ENDPOINT="https://api.deepseek.com/chat/completions";
 // A broken platform socket must not keep a caller waiting or create unlimited abandoned workers.
 private static ThreadFactory threads(String name){return runnable->{Thread thread=new Thread(runnable,name);thread.setDaemon(true);return thread;};}
 private static ExecutorService pool(String name){return new ThreadPoolExecutor(0,2,30,TimeUnit.SECONDS,new SynchronousQueue<Runnable>(),threads(name),new ThreadPoolExecutor.AbortPolicy());}
 private static final ExecutorService IO=pool("deepseek-io"),CLOSE=pool("deepseek-close");
 private static final ScheduledThreadPoolExecutor TIMER=new ScheduledThreadPoolExecutor(1,threads("deepseek-deadline"));
 static {TIMER.setRemoveOnCancelPolicy(true);}
 private final long requestTimeoutMillis,uploadTimeoutMillis;
 private Attempt active;
 private boolean cancelled;
 public DeepSeekTransport(){this(REQUEST_TIMEOUT_MILLIS,UPLOAD_TIMEOUT_MILLIS);}
 // Package-only timing seam for offline tests; callers cannot choose an endpoint or extend a deadline.
 DeepSeekTransport(long requestTimeoutMillis,long uploadTimeoutMillis){if(requestTimeoutMillis<1||uploadTimeoutMillis<1)throw new IllegalArgumentException("Invalid deadline");this.requestTimeoutMillis=requestTimeoutMillis;this.uploadTimeoutMillis=Math.min(requestTimeoutMillis,uploadTimeoutMillis);}
 public String send(String key,String payload)throws IOException{return sendTo(new URL(ENDPOINT),key,payload);}
 String sendTo(URL url,String key,String payload)throws IOException{
  synchronized(this){if(cancelled)throw cancelled();if(active!=null)throw new Failure("network","已有识别请求正在进行");}
  if(payload==null||payload.isEmpty()||payload.length()>MAX_REQUEST_BYTES)throw new Failure("input","图片请求过大");
  byte[] bytes=payload.getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_REQUEST_BYTES)throw new Failure("input","图片请求过大");
  Attempt attempt=new Attempt(url,key,bytes);synchronized(this){if(cancelled)throw cancelled();if(active!=null)throw new Failure("network","已有识别请求正在进行");active=attempt;}
  try{
   try{IO.execute(attempt);}catch(RejectedExecutionException busy){attempt.abort(new Failure("network","上次网络连接尚未释放，请稍后主动重试"));}
   try{return attempt.result.get(requestTimeoutMillis,TimeUnit.MILLISECONDS);}
   catch(TimeoutException elapsed){attempt.abort(timeout());return completed(attempt);}
   catch(InterruptedException interrupted){attempt.abort(cancelled());Thread.currentThread().interrupt();throw cancelled();}
   catch(ExecutionException failed){throw failure(failed.getCause());}
  }finally{synchronized(this){if(active==attempt)active=null;}}
 }
 private static String completed(Attempt attempt)throws IOException{try{return attempt.result.get();}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw cancelled();}catch(ExecutionException failed){throw failure(failed.getCause());}}
 private static IOException failure(Throwable error){return error instanceof IOException?(IOException)error:new Failure("network","识别连接未完成");}
 private static Failure timeout(){return new Failure("timeout","连接或识别超时");}
 private static Failure cancelled(){return new Failure("cancelled","已取消");}
 private final class Attempt implements Runnable {
  final URL url;final String key;final byte[] bytes;
  final CompletableFuture<String> result=new CompletableFuture<>();
  final AtomicBoolean disconnectStarted=new AtomicBoolean();
  volatile HttpURLConnection connection;volatile Thread runner;volatile ScheduledFuture<?> uploadTimer;
  Attempt(URL url,String key,byte[] bytes){this.url=url;this.key=key;this.bytes=bytes;}
  void checkRunning()throws IOException{if(result.isDone())throw cancelled();}
  void abort(Failure error){if(!result.completeExceptionally(error))return;ScheduledFuture<?> timer=uploadTimer;if(timer!=null)timer.cancel(false);Thread thread=runner;if(thread!=null)thread.interrupt();try{CLOSE.execute(this::disconnect);}catch(RejectedExecutionException ignored){/* Still bounded; the I/O worker also closes on exit. */}}
  void disconnect(){HttpURLConnection c=connection;if(c!=null&&disconnectStarted.compareAndSet(false,true))try{c.disconnect();}catch(RuntimeException ignored){}}
  public void run(){runner=Thread.currentThread();String answer=null;Throwable error=null;try{answer=exchange();}catch(Exception|OutOfMemoryError failed){error=failed;}finally{ScheduledFuture<?> timer=uploadTimer;if(timer!=null)timer.cancel(false);}
   if(error==null)result.complete(answer);else result.completeExceptionally(failure(error));
   disconnect();runner=null;
  }
  String exchange()throws IOException{
   checkRunning();HttpURLConnection c=(HttpURLConnection)url.openConnection();connection=c;checkRunning();
   c.setRequestMethod("POST");c.setInstanceFollowRedirects(false);c.setConnectTimeout((int)Math.min(20_000,requestTimeoutMillis));c.setReadTimeout((int)Math.min(120_000,requestTimeoutMillis));c.setDoOutput(true);
   c.setRequestProperty("Authorization","Bearer "+key);c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setFixedLengthStreamingMode(bytes.length);
   checkRunning();uploadTimer=TIMER.schedule(()->abort(timeout()),uploadTimeoutMillis,TimeUnit.MILLISECONDS);
   try{checkRunning();try(OutputStream out=c.getOutputStream()){checkRunning();out.write(bytes);}}
   finally{uploadTimer.cancel(false);}
   checkRunning();int status=c.getResponseCode();if(status!=200){if(status==401||status==403)throw new Failure("auth","API 密钥无效或没有权限，请检查配置");if(status==402)throw new Failure("balance","DeepSeek 账户余额不足");if(status==429)throw new Failure("rate","请求过于频繁，请稍后重试");if(status>=300&&status<400)throw new Failure("server","服务返回重定向，已停止以保护密钥");throw new Failure("server","识别服务暂不可用（HTTP "+status+"），未写入账目");}
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){checkRunning();if(out.size()+n>2_000_000)throw new Failure("parse","识别响应过大");out.write(b,0,n);}}
   checkRunning();return out.toString("UTF-8");
  }
 }
 public void cancel(){Attempt attempt;synchronized(this){cancelled=true;attempt=active;}if(attempt!=null)attempt.abort(cancelled());}
}
