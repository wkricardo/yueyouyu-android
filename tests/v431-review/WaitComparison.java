package cn.dot.budget;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** Controlled-path comparison, not a provider performance test. */
public final class WaitComparison {
 public static void main(String[] args)throws Exception {
  final boolean patched="patched".equals(args[0]);
  for(boolean upload:new boolean[]{false,true}){
   final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
   final AtomicInteger reads=new AtomicInteger();
   URL url=new URL(null,DeepSeekTransport.ENDPOINT,new URLStreamHandler(){protected URLConnection openConnection(URL u){return new HttpURLConnection(u){
    public void connect(){}public void disconnect(){release.countDown();}public boolean usingProxy(){return false;}
    public OutputStream getOutputStream(){return new OutputStream(){public void write(int b)throws IOException{if(upload){entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("synthetic interrupted upload");}}}};}
    public int getResponseCode(){return 200;}
    public InputStream getInputStream(){return new InputStream(){public int read(){entered.countDown();if(release.getCount()==0)return -1;try{Thread.sleep(5);}catch(InterruptedException ignored){}reads.incrementAndGet();return ' ';}public int read(byte[] b,int off,int len){int value=read();if(value<0)return -1;b[off]=(byte)value;return 1;}};}
   };}});
   final DeepSeekTransport transport;
   if(patched){Constructor<DeepSeekTransport> ctor=DeepSeekTransport.class.getDeclaredConstructor(long.class,long.class);ctor.setAccessible(true);transport=ctor.newInstance(180L,90L);}else transport=new DeepSeekTransport();
   FutureTask<String> task=new FutureTask<>(()->transport.sendTo(url,"synthetic-not-a-real-key","{}"));Thread thread=new Thread(task,"wait-comparison");thread.setDaemon(true);
   try{
    thread.start();if(!entered.await(1,TimeUnit.SECONDS))throw new AssertionError("mock stage was not entered");
    if(patched){try{task.get(1,TimeUnit.SECONDS);throw new AssertionError("timeout expected");}catch(ExecutionException expected){if(!(expected.getCause() instanceof DeepSeekTransport.Failure)||!"timeout".equals(((DeepSeekTransport.Failure)expected.getCause()).code))throw expected;}System.out.println("PATCHED "+(upload?"blocked upload":"continuous whitespace")+": returned timeout without harness cancellation (injected total180ms/upload90ms).");}
    else{Thread.sleep(450);if(task.isDone())throw new AssertionError("baseline unexpectedly completed");System.out.println("BASELINE4.3 "+(upload?"blocked upload":"continuous whitespace ("+reads.get()+" reads)")+": still pending after450ms observation; harness cancellation used for cleanup. This observation alone does not claim the production120s idle timeout elapsed.");}
   }finally{transport.cancel();release.countDown();try{task.get(1,TimeUnit.SECONDS);}catch(ExecutionException expected){}thread.join(500);if(thread.isAlive())throw new AssertionError("test caller leaked");}
  }
 }
}
