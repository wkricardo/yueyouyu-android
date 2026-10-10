package cn.dot.budget;

import java.util.Arrays;

/** Frozen, memory-only outbound bytes. No storage or access to ledger/profile data. */
public final class AiConsentSnapshot {
 public interface TextPayloadFactory {String build(String approvedText)throws Exception;}
 public interface PayloadFactory { String build(byte[] approvedImage)throws Exception; }
 private final AiRequestSession.Ticket owner;
 private final byte[] image;
 private final String mime,payload,text;
 public AiConsentSnapshot(AiRequestSession.Ticket owner,byte[] image,String mime,PayloadFactory factory)throws Exception{
  if(owner==null||image==null||image.length==0||image.length>10*1024*1024||factory==null||!("image/png".equals(mime)||"image/jpeg".equals(mime)||"image/webp".equals(mime)))throw new IllegalArgumentException("无效的图片确认快照");
  this.text=null;this.owner=owner;this.image=Arrays.copyOf(image,image.length);this.mime=mime;
  // Builder receives a separate copy: it cannot mutate the bytes shown in consent.
  byte[] builderImage=Arrays.copyOf(this.image,this.image.length);
  this.payload=factory.build(builderImage);
  if(!Arrays.equals(builderImage,this.image))throw new IllegalArgumentException("图片确认快照发生变更");
  if(payload==null||payload.isEmpty()||payload.length()>15*1024*1024||payload.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>15*1024*1024)throw new IllegalArgumentException("图片请求过大");
 }
 public AiConsentSnapshot(AiRequestSession.Ticket owner,String text,TextPayloadFactory factory)throws Exception{if(owner==null||factory==null||!("budget_text".equals(owner.request.source)||"food_text".equals(owner.request.source))||!AiRequestContract.validateText(text).equals(owner.request.text))throw new IllegalArgumentException("无效的文字确认快照");this.owner=owner;this.text=text;this.image=null;this.mime="text/plain";this.payload=factory.build(text);if(payload==null||payload.isEmpty()||payload.length()>15*1024*1024||payload.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>15*1024*1024)throw new IllegalArgumentException("文字请求过大");}
 public boolean isText(){return text!=null;}
 public String text(){return text;}
 public boolean matches(AiRequestSession.Ticket ticket){return owner==ticket;}
 public byte[] copyImageBytes(){if(image==null)throw new IllegalStateException("Text snapshot has no image");return Arrays.copyOf(image,image.length);}
 public String mime(){return mime;}
 public String payload(){return payload;}
}
