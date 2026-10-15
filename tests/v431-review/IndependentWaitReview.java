package cn.dot.budget;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Independent no-network checks. Every blocking fake is explicitly released, even on failure. */
public final class IndependentWaitReview {
 private static int checks;
 private static final String PAYLOAD="{\"synthetic\":true}";
 private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 private enum Stage {OPEN,OUTPUT,WRITE,HEADERS,READ,WHITESPACE,NONE}
 private static void waitIgnoringInterrupt(CountDownLatch latch){boolean interrupted=false;for(;;)try{latch.await();break;}catch(InterruptedException ignored){interrupted=true;}if(interrupted)Thread.currentThread().interrupt();}
 private static final class Fake extends HttpURLConnection {
  final Stage stage; final boolean blockDisconnect; final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),disconnectEntered=new CountDownLatch(1),disconnectReleased=new CountDownLatch(1);
  final AtomicInteger readCalls=new AtomicInteger(),disconnectCalls=new AtomicInteger();
  final ByteArrayOutputStream captured=new ByteArrayOutputStream();
  Fake(Stage stage,boolean blockDisconnect)throws Exception{super(new URL("https://api.deepseek.com/chat/completions"));this.stage=stage;this.blockDisconnect=blockDisconnect;}
  void block(Stage current){if(stage==current){entered.countDown();waitIgnoringInterrupt(release);}}
  public void connect(){} public boolean usingProxy(){return false;}
  public void disconnect(){disconnectCalls.incrementAndGet();disconnectEntered.countDown();if(blockDisconnect)waitIgnoringInterrupt(disconnectReleased);}
  public OutputStream getOutputStream(){block(Stage.OUTPUT);return new OutputStream(){public void write(int b){block(Stage.WRITE);captured.write(b);}public void write(byte[] bytes,int off,int len){block(Stage.WRITE);captured.write(bytes,off,len);}};}
  public int getResponseCode(){block(Stage.HEADERS);return 200;}
  public InputStream getInputStream(){return new InputStream(){int pos;final byte[] data="{}".getBytes(StandardCharsets.UTF_8);public int read()throws IOException{byte[] one=new byte[1];return read(one,0,1)<0?-1:one[0]&255;}public int read(byte[] b,int off,int len){readCalls.incrementAndGet();block(Stage.READ);if(stage==Stage.WHITESPACE){entered.countDown();try{Thread.sleep(15);}catch(InterruptedException ignored){}if(release.getCount()>0){b[off]=' ';return 1;}}if(pos>=data.length)return -1;int count=Math.min(len,data.length-pos);System.arraycopy(data,pos,b,off,count);pos+=count;return count;}};}
  URL url()throws Exception{return new URL(null,"https://api.deepseek.com/chat/completions",new URLStreamHandler(){protected URLConnection openConnection(URL ignored){block(Stage.OPEN);return Fake.this;}});}
  void releaseAll(){release.countDown();disconnectReleased.countDown();}
 }
 private static final class Call {
  final CountDownLatch done=new CountDownLatch(1);final AtomicReference<String> value=new AtomicReference<>();final AtomicReference<Throwable> failure=new AtomicReference<>();final Thread thread;
  Call(DeepSeekTransport t,Fake fake)throws Exception{URL url=fake.url();thread=new Thread(()->{try{value.set(t.sendTo(url,"synthetic-not-a-real-key",PAYLOAD));}catch(Throwable e){failure.set(e);}finally{done.countDown();}},"independent-transport-caller");thread.setDaemon(true);thread.start();}
  void finished(String description)throws Exception{check(done.await(1800,TimeUnit.MILLISECONDS),description+" did not release caller within bounded margin");}
  void error(String code,String description){Throwable error=failure.get();check(error instanceof DeepSeekTransport.Failure,description+" should have stable failure type but got "+error);check(code.equals(((DeepSeekTransport.Failure)error).code),description+" should report "+code+" but got "+((DeepSeekTransport.Failure)error).code);check(value.get()==null,description+" cannot also return success");}
 }
 private static void deadline(Stage stage,long total,long upload)throws Exception{
  Fake fake=new Fake(stage,true);DeepSeekTransport t=new DeepSeekTransport(total,upload);Call call=null;
  try{call=new Call(t,fake);check(fake.entered.await(1000,TimeUnit.MILLISECONDS),stage+" fake was entered");call.finished(stage+" deadline");call.error("timeout",stage+" deadline");if(stage==Stage.WHITESPACE)check(fake.readCalls.get()>1,"periodic whitespace keeps reads active");fake.releaseAll();Thread.sleep(60);call.error("timeout",stage+" late success must not replace timeout");}
  finally{fake.releaseAll();if(call!=null)call.thread.join(500);}
 }
 private static void cancelled(Stage stage)throws Exception{
  Fake fake=new Fake(stage,true);DeepSeekTransport t=new DeepSeekTransport(2000,1500);Call call=null;
  try{call=new Call(t,fake);check(fake.entered.await(1000,TimeUnit.MILLISECONDS),stage+" cancellation fake was entered");long start=System.nanoTime();t.cancel();check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<300,"cancel must not wait for blocked disconnect");call.finished(stage+" cancel");call.error("cancelled",stage+" cancel");fake.releaseAll();Thread.sleep(60);call.error("cancelled",stage+" late success must not replace cancellation");}
  finally{fake.releaseAll();if(call!=null)call.thread.join(500);}
 }
 private static void saturationAndRecovery()throws Exception{
  Thread.sleep(150); // Let the preceding deliberately blocked cleanup return to its executor.
  Fake first=new Fake(Stage.READ,true),second=new Fake(Stage.READ,true),third=new Fake(Stage.NONE,false);Call a=null,b=null,c=null;
  try{
   a=new Call(new DeepSeekTransport(500,300),first);b=new Call(new DeepSeekTransport(500,300),second);
   check(first.entered.await(1000,TimeUnit.MILLISECONDS)&&second.entered.await(1000,TimeUnit.MILLISECONDS),"both bounded I/O workers occupied");
   a.finished("first saturated worker");b.finished("second saturated worker");a.error("timeout","first saturated worker");b.error("timeout","second saturated worker");
   c=new Call(new DeepSeekTransport(800,300),third);c.finished("bounded-pool rejection");c.error("network","bounded-pool rejection");check(third.captured.size()==0,"saturated executor does not silently queue or send a third request");
  }finally{first.releaseAll();second.releaseAll();third.releaseAll();if(a!=null)a.thread.join(500);if(b!=null)b.thread.join(500);if(c!=null)c.thread.join(500);}
  long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);boolean recovered=false;
  while(System.nanoTime()<until&&!recovered){Fake probe=new Fake(Stage.NONE,false);try{String response=new DeepSeekTransport(500,250).sendTo(probe.url(),"synthetic-not-a-real-key",PAYLOAD);recovered="{}".equals(response);}catch(DeepSeekTransport.Failure e){check("network".equals(e.code),"recovery probe fails only while executor still occupied");Thread.sleep(20);}finally{probe.releaseAll();}}
  check(recovered,"executor becomes usable after old blocked operations are released");
 }
 public static void main(String[] args)throws Exception{
  for(Stage stage:new Stage[]{Stage.OPEN,Stage.OUTPUT,Stage.WRITE})deadline(stage,800,180);
  for(Stage stage:new Stage[]{Stage.HEADERS,Stage.READ,Stage.WHITESPACE})deadline(stage,220,800);
  for(Stage stage:new Stage[]{Stage.OUTPUT,Stage.WRITE,Stage.READ})cancelled(stage);
  Fake fake=new Fake(Stage.NONE,true);Call call=null;
  try{call=new Call(new DeepSeekTransport(800,300),fake);call.finished("completed response with blocked disconnect");check(call.failure.get()==null,"disconnect must not turn completed response into failure");check("{}".equals(call.value.get()),"completed response survives blocking cleanup");check(PAYLOAD.equals(fake.captured.toString("UTF-8")),"exact payload bytes preserved");check(!fake.getInstanceFollowRedirects(),"redirects stay disabled");check("Bearer synthetic-not-a-real-key".equals(fake.getRequestProperty("Authorization")),"synthetic test credential scoped to the fixed endpoint");}
  finally{fake.releaseAll();if(call!=null)call.thread.join(500);}
  saturationAndRecovery();
  DeepSeekTransport before=new DeepSeekTransport(800,300);before.cancel();Fake never=new Fake(Stage.NONE,false);try{before.sendTo(never.url(),"synthetic-not-a-real-key",PAYLOAD);throw new AssertionError("pre-cancelled request sent");}catch(DeepSeekTransport.Failure e){check("cancelled".equals(e.code),"pre-cancel preserves cancellation code");check(never.captured.size()==0,"pre-cancel sends no bytes");}finally{never.releaseAll();}
  System.out.println("PASS: "+checks+" independent total/upload timeout, active-whitespace, non-interruptible I/O, blocked-disconnect, cancellation and late-success assertions. No network or real credentials used.");
 }
}
