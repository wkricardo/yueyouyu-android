package cn.dot.budget;

import android.graphics.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Serial, cancellable image work. Camera input owns an open descriptor before unlink. */
final class AiImagePreparer {
 interface Source {InputStream open()throws IOException;}
 interface Result {void ready(Prepared image);void failed(Exception error);}
 static final class Prepared {
  final byte[] bytes;final String mime;final int width,height;final boolean reduced;
  Prepared(byte[] bytes,String mime,int width,int height,boolean reduced){this.bytes=bytes;this.mime=mime;this.width=width;this.height=height;this.reduced=reduced;}
 }
 private final File cache;
 // Shared across Activity recreation so cancelled native codecs cannot overlap new decodes.
 private static final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(1),r->new Thread(r,"ai-image-preparation"));
 private static final java.util.Set<File> activeStages=new java.util.HashSet<>();
 private static synchronized void cleanOrphans(File directory)throws IOException{File[] files=directory.listFiles();if(files==null)throw new IOException("无法检查临时图片，请重启后重试");int seen=0;for(File file:files){if(!file.getName().matches("ai-stage-[0-9]+\\.image")||activeStages.contains(file))continue;if(++seen>64)throw new IOException("临时图片过多，请重启后重试");if(file.exists()&&!file.delete())throw new IOException("上次临时图片无法清理，已停止处理");}}
 private static synchronized File stage(File directory)throws IOException{cleanOrphans(directory);File file=File.createTempFile("ai-stage-",".image",directory);activeStages.add(file);return file;}
 private static synchronized boolean removeStage(File file){boolean absent=!file.exists()||file.delete();activeStages.remove(file);return absent;}
 AiImagePreparer(File cache){this.cache=cache;}
 boolean cleanupOrphans(){try{cleanOrphans(cache);return true;}catch(IOException e){return false;}}
 final class Job implements Runnable {
  final Source source;final boolean food;final Result result;final AtomicBoolean cancelled=new AtomicBoolean();
  private InputStream open;private Future<?> future;
  Job(Source source,InputStream owned,boolean food,Result result){this.source=source;open=owned;this.food=food;this.result=result;}
  void check()throws InterruptedIOException{if(cancelled.get()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("已取消图片处理");}
  void cancel(){cancelled.set(true);InputStream old;Future<?> running;synchronized(this){old=open;open=null;running=future;}close(old);if(running!=null){running.cancel(true);if(running instanceof Runnable)worker.remove((Runnable)running);}}
  private synchronized void own(InputStream input)throws IOException{if(cancelled.get()){close(input);throw new InterruptedIOException();}open=input;}
  public void run(){File stage=null;Bitmap bitmap=null;try{
   check();InputStream input; synchronized(this){input=open;}if(input==null){input=source.open();if(input==null)throw new IOException("图片来源不可读取");own(input);}check();
   stage=stage(cache);
   try(InputStream in=input;OutputStream out=new FileOutputStream(stage)){AiSourceCopy.copy(in,out,cancelled::get);}finally{synchronized(this){open=null;}}
   check();BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(stage.getAbsolutePath(),bounds);
   if(!"image/jpeg".equals(bounds.outMimeType)&&!"image/png".equals(bounds.outMimeType)&&!"image/webp".equals(bounds.outMimeType)&&!"image/heif".equals(bounds.outMimeType)&&!"image/heic".equals(bounds.outMimeType))throw new IOException("无法解码这张图片；请选择手机支持的 PNG、JPEG、WebP 或 HEIF 图片");
   int orientation=1;try{android.media.ExifInterface exif=new android.media.ExifInterface(stage.getAbsolutePath());orientation=exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1);if(orientation==0)orientation=1;}catch(IOException ignored){}
   long budget=Math.min(AiImagePolicy.MAX_BITMAP_BUDGET,Math.max(8L,Runtime.getRuntime().maxMemory()/4));
   AiImagePolicy.Plan plan=AiImagePolicy.plan(bounds.outWidth,bounds.outHeight,food,orientation,budget);check();
   BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=plan.sampleSize;options.inPreferredConfig=Bitmap.Config.ARGB_8888;options.inScaled=false;
   bitmap=BitmapFactory.decodeFile(stage.getAbsolutePath(),options);if(bitmap==null)throw new IOException("照片无法解码，请重新拍摄或选择图片");check();
   if((long)bitmap.getWidth()*bitmap.getHeight()*4*((food||orientation!=1)?2:1)>budget)throw new IOException("图片解码超出安全内存，请分段选择");
   if(orientation!=1){Matrix matrix=new Matrix();matrix.setValues(AiImagePolicy.matrix(orientation));Bitmap oriented=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);if(oriented!=bitmap)bitmap.recycle();bitmap=oriented;}check();
   // A smaller heap also bounds transient byte/base64/JSON copies.
   int limit=(int)Math.min(AiBoundedBytes.MAX_BYTES,Math.max(256_000L,Runtime.getRuntime().maxMemory()/32));
   Prepared prepared=encode(bitmap,food,limit,plan.sampleSize>1);check();bitmap.recycle();bitmap=null;if(!removeStage(stage))throw new IOException("临时图片无法清理，本次未发送，请重试");stage=null;result.ready(prepared);
  }catch(Exception error){if(!cancelled.get())result.failed(error);}catch(OutOfMemoryError error){if(!cancelled.get())result.failed(new IOException("图片超出当前手机的处理内存，请裁剪后重试"));}
  finally{if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();InputStream old;synchronized(this){old=open;open=null;}close(old);if(stage!=null)removeStage(stage);}}
  private Prepared encode(Bitmap original,boolean food,int limit,boolean reduced)throws IOException{
   final Bitmap[] current={original};try{AiImageEncoding.Encoded encoded=AiImageEncoding.encode(new AiImageEncoding.Codec(){
    public boolean compress(boolean png,int quality,OutputStream out){return current[0].compress(png?Bitmap.CompressFormat.PNG:Bitmap.CompressFormat.JPEG,quality,out);}
    public void shrink(){Bitmap old=current[0];Bitmap next=Bitmap.createScaledBitmap(old,Math.max(1,old.getWidth()*3/4),Math.max(1,old.getHeight()*3/4),true);current[0]=next;if(old!=original&&old!=next)old.recycle();}
    public boolean hasAlpha(){return current[0].hasAlpha();}public int width(){return current[0].getWidth();}public int height(){return current[0].getHeight();}
   },food,limit,()->cancelled.get()||Thread.currentThread().isInterrupted());return new Prepared(encoded.bytes,encoded.mime,encoded.width,encoded.height,reduced||current[0]!=original);
   }finally{if(current[0]!=original&&!current[0].isRecycled())current[0].recycle();}
  }
 }
 Job prepare(Source source,InputStream owned,boolean food,Result result){Job job=new Job(source,owned,food,result);try{synchronized(job){job.future=worker.submit(job);}}catch(RejectedExecutionException e){job.cancel();result.failed(new IOException("上次图片仍在清理，请稍后重新选择"));}return job;}
 void destroy(){/* Active job is cancelled by the owning activity; shared serial worker survives recreation. */}
 static void close(InputStream input){if(input!=null)try{input.close();}catch(IOException ignored){}}
}
