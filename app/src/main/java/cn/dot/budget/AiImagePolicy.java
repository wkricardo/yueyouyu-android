package cn.dot.budget;

/** Arithmetic-only allocation policy. Statement images are rejected rather than shrunk. */
public final class AiImagePolicy {
 public static final int MAX_EDGE=8192,FOOD_EDGE=2048,PREVIEW_EDGE=1024;
 public static final long MAX_BITMAP_BUDGET=64L*1024*1024;
 public static final class Plan {
  public final int sampleSize,outputWidth,outputHeight,orientation;
  public final long estimatedBytes;
  private Plan(int sample,int width,int height,int orientation,long bytes){sampleSize=sample;outputWidth=width;outputHeight=height;this.orientation=orientation;estimatedBytes=bytes;}
 }
 private AiImagePolicy(){}
 public static boolean swapsAxes(int orientation){if(orientation<1||orientation>8)throw new IllegalArgumentException("图片方向信息无效");return orientation>=5;}
 public static int previewSample(int width,int height){dimensions(width,height);int sample=1;while(ceil(width,sample)>PREVIEW_EDGE||ceil(height,sample)>PREVIEW_EDGE)sample*=2;return sample;}
 private static int ceil(int value,int divisor){return (value+divisor-1)/divisor;}
 private static void dimensions(int width,int height){if(width<1||height<1||width>MAX_EDGE||height>MAX_EDGE)throw new IllegalArgumentException("图片尺寸无效，请分段选择清晰截图");}
 public static Plan plan(int width,int height,boolean food,int orientation,long bitmapBudgetBytes){dimensions(width,height);boolean swap=swapsAxes(orientation);if(bitmapBudgetBytes<8||bitmapBudgetBytes>MAX_BITMAP_BUDGET)throw new IllegalArgumentException("图片内存预算无效");int sample=1;while(food&&(ceil(width,sample)>FOOD_EDGE||ceil(height,sample)>FOOD_EDGE||bytes(width,height,sample,orientation)>bitmapBudgetBytes)){sample*=2;if(sample>MAX_EDGE)throw new IllegalArgumentException("图片过大，无法安全处理");}long bytes=bytes(width,height,sample,orientation);if(bytes>bitmapBudgetBytes)throw new IllegalArgumentException("截图像素过多，请分段截图；不会缩小账单文字");int w=ceil(width,sample),h=ceil(height,sample);return new Plan(sample,swap?h:w,swap?w:h,orientation,bytes);}
 private static long bytes(int width,int height,int sample,int orientation){return (long)ceil(width,sample)*ceil(height,sample)*4*(orientation==1?1:2);}
}
