package cn.dot.budget;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Streaming encoding bound: do not allocate an oversized re-encoded photo first. */
public final class AiBoundedBytes extends OutputStream {
 public static final int MAX_BYTES=9_999_999;
 private boolean exceeded;
 private final int limit;private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
 public AiBoundedBytes(int limit){if(limit<1||limit>MAX_BYTES)throw new IllegalArgumentException("Invalid image bound");this.limit=limit;}
 @Override public void write(int value)throws IOException{if(bytes.size()>=limit){exceeded=true;throw new IOException("处理后图片超过 10 MB，请分段截图");}bytes.write(value);}
 @Override public void write(byte[] buffer,int offset,int length)throws IOException{if(buffer==null)throw new NullPointerException();if(offset<0||length<0||offset>buffer.length-length)throw new IndexOutOfBoundsException();if(length>limit-bytes.size()){exceeded=true;throw new IOException("处理后图片超过 10 MB，请分段截图");}bytes.write(buffer,offset,length);}
 public boolean exceeded(){return exceeded;}
 public int size(){return bytes.size();}
 public byte[] toByteArray(){return bytes.toByteArray();}
}
