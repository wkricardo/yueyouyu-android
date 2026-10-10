package cn.dot.budget;

/** Bound source dimensions before allocation, and bill downsampling before losing text. */
public final class AiImagePolicy {
 public static final int MAX_EDGE=8192,MAX_SOURCE_EDGE=65535,FOOD_EDGE=2048,BILL_EDGE=4096,PREVIEW_EDGE=1024;
 public static final long MAX_SOURCE_PIXELS=268_435_456L;
 public static final long MAX_BITMAP_BUDGET=64L*1024*1024;
 public static final class Plan {
  public final int sampleSize,outputWidth,outputHeight,orientation;
  public final long estimatedBytes;
  private Plan(int sample,int width,int height,int orientation,long bytes){sampleSize=sample;outputWidth=width;outputHeight=height;this.orientation=orientation;estimatedBytes=bytes;}
 }
 private AiImagePolicy(){}
 public static boolean swapsAxes(int orientation){if(orientation<1||orientation>8)throw new IllegalArgumentException("图片方向信息无效");return orientation>=5;}
 public static int previewSample(int width,int height){dimensions(width,height,MAX_EDGE);int sample=1;while(ceil(width,sample)>PREVIEW_EDGE||ceil(height,sample)>PREVIEW_EDGE)sample*=2;return sample;}
 private static int ceil(int value,int divisor){return (value+divisor-1)/divisor;}
 private static void dimensions(int width,int height,int maximum){if(width<1||height<1||width>maximum||height>maximum)throw new IllegalArgumentException("图片尺寸无效，请分段选择清晰截图");}
 public static Plan plan(int width,int height,boolean food,int orientation,long bitmapBudgetBytes){
  dimensions(width,height,MAX_SOURCE_EDGE);if((long)width*height>MAX_SOURCE_PIXELS)throw new IllegalArgumentException("原图像素过多，请裁剪后重试");boolean swap=swapsAxes(orientation);if(bitmapBudgetBytes<8||bitmapBudgetBytes>MAX_BITMAP_BUDGET)throw new IllegalArgumentException("图片内存预算无效");int sample=1,edge=food?FOOD_EDGE:BILL_EDGE;
  while(ceil(width,sample)>edge||ceil(height,sample)>edge||bytes(width,height,sample,orientation,food)>bitmapBudgetBytes){sample*=2;if(sample>65536)throw new IllegalArgumentException("图片过大，无法安全处理");}
  // Long, narrow bills need readable crops, not automatic tiny text.
  if(!food&&sample>1&&(sample>2||Math.min(ceil(width,sample),ceil(height,sample))<1024))throw new IllegalArgumentException("账单过长或像素过多，请分段截取清晰交易；不会把文字压成难以核对的小字");
  int w=ceil(width,sample),h=ceil(height,sample);return new Plan(sample,swap?h:w,swap?w:h,orientation,bytes(width,height,sample,orientation,food));
 }
 private static long bytes(int width,int height,int sample,int orientation,boolean food){return (long)ceil(width,sample)*ceil(height,sample)*4*((food||orientation!=1)?2:1);}
 /** Android Matrix entries, including all mirrored EXIF orientations. */
 public static float[] matrix(int orientation){switch(orientation){case 1:return new float[]{1,0,0,0,1,0,0,0,1};case 2:return new float[]{-1,0,0,0,1,0,0,0,1};case 3:return new float[]{-1,0,0,0,-1,0,0,0,1};case 4:return new float[]{1,0,0,0,-1,0,0,0,1};case 5:return new float[]{0,1,0,1,0,0,0,0,1};case 6:return new float[]{0,-1,0,1,0,0,0,0,1};case 7:return new float[]{0,-1,0,-1,0,0,0,0,1};case 8:return new float[]{0,1,0,-1,0,0,0,0,1};default:throw new IllegalArgumentException("图片方向无效");}}
}
