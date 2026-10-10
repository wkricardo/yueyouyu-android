package cn.dot.budget;
import java.io.*;
import java.util.function.BooleanSupplier;

/** Original bytes stream to private disk; never reject at the outbound image limit. */
public final class AiSourceCopy {
 public static final long MAX_SOURCE_BYTES=128L*1024*1024;
 private AiSourceCopy(){}
 public static long copy(InputStream in,OutputStream out,BooleanSupplier cancelled)throws IOException{
  byte[] block=new byte[32*1024];long total=0;int n;
  while(true){if(cancelled.getAsBoolean())throw new InterruptedIOException("已取消图片处理");n=in.read(block);if(n<0)break;if(n==0)throw new IOException("图片来源读取失败");if(n>MAX_SOURCE_BYTES-total)throw new IOException("原图超过安全处理上限，请裁剪或选择较小原图");out.write(block,0,n);total+=n;}
  if(cancelled.getAsBoolean())throw new InterruptedIOException("已取消图片处理");if(total==0)throw new IOException("相机未写入照片，请重新拍摄或选择图片");return total;
 }
}
