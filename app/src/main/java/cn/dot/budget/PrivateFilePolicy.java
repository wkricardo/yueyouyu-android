package cn.dot.budget;
import java.io.FileNotFoundException;
final class PrivateFilePolicy{
 static String fileName(String authority,String path,String mode)throws FileNotFoundException{
  if(!"cn.dot.budget.files".equals(authority))throw new FileNotFoundException("Unknown authority");
  if(!"r".equals(mode)&&!"w".equals(mode)&&!"wt".equals(mode)&&!"rw".equals(mode)&&!"rwt".equals(mode))throw new FileNotFoundException("Mode not allowed");
  if("/images/capture.jpg".equals(path))return "food-capture.jpg";
  if(path!=null&&path.startsWith("/images/capture-"))return AiCapturePath.fileName(path);
  if("/apk/update.apk".equals(path)&&"r".equals(mode))return "update.apk";
  throw new FileNotFoundException("Not an allowed file or access mode");
 }
}
