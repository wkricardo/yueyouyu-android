package cn.dot.budget;
import java.io.*;
import java.util.function.BooleanSupplier;

/** Shared finite encoding ladder; the codec never exposes original bytes as a fallback. */
public final class AiImageEncoding {
 public interface Codec {boolean compress(boolean png,int quality,OutputStream out)throws IOException;void shrink();boolean hasAlpha();int width();int height();}
 public static final class Encoded {public final byte[] bytes;public final String mime;public final int width,height;Encoded(byte[] bytes,String mime,int width,int height){this.bytes=bytes;this.mime=mime;this.width=width;this.height=height;}}
 private AiImageEncoding(){}
 private static void check(BooleanSupplier cancelled)throws IOException{if(cancelled.getAsBoolean())throw new InterruptedIOException("已取消图片处理");}
 public static Encoded encode(Codec codec,boolean food,int limit,BooleanSupplier cancelled)throws IOException{
  int stages=food?3:1;int[] qualities=food?new int[]{90,82,72,62}:new int[]{94,88,82};
  for(int stage=0;stage<stages;stage++){
   check(cancelled);if(!food){Encoded png=attempt(codec,true,100,limit,cancelled);if(png!=null)return png;if(codec.hasAlpha())throw new IOException("透明账单需分段保存为清晰截图后重试，不会转成可能改变底色的 JPEG");}
   for(int quality:qualities){Encoded jpeg=attempt(codec,false,quality,limit,cancelled);if(jpeg!=null)return jpeg;}
   if(stage+1<stages){check(cancelled);codec.shrink();}
  }
  throw new IOException(food?"图片无法压缩到安全大小，请裁剪后重试":"账单无法在保留清晰文字时压缩到安全大小，请分段截图");
 }
 private static Encoded attempt(Codec codec,boolean png,int quality,int limit,BooleanSupplier cancelled)throws IOException{
  check(cancelled);AiBoundedBytes out=new AiBoundedBytes(limit);boolean encoded;
  try{encoded=codec.compress(png,quality,out);}catch(Exception e){if(out.exceeded())return null;throw new IOException("图片编码失败，请换一张重试",e);}
  check(cancelled);if(out.exceeded())return null;if(!encoded||out.size()==0)throw new IOException("图片编码失败，请换一张重试");
  return new Encoded(out.toByteArray(),png?"image/png":"image/jpeg",codec.width(),codec.height());
 }
}
