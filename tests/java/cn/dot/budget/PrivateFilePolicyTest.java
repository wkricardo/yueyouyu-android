package cn.dot.budget;import java.io.*;
public class PrivateFilePolicyTest{
 static int checks=0;
 static void good(String path,String mode,String expected)throws Exception{if(!PrivateFilePolicy.fileName("cn.dot.budget.files",path,mode).equals(expected))throw new AssertionError();checks++;}
 static void bad(String authority,String path,String mode)throws Exception{try{PrivateFilePolicy.fileName(authority,path,mode);throw new AssertionError("Unexpected access");}catch(FileNotFoundException ok){checks++;}}
 public static void main(String[]args)throws Exception{
  good("/images/capture.jpg","w","food-capture.jpg");good("/images/capture.jpg","rw","food-capture.jpg");good("/images/capture.jpg","r","food-capture.jpg");good("/apk/update.apk","r","update.apk");
  for(String path:new String[]{"/","/apk/../secret","/images/../../secret","/shared_prefs/deepseek-secure.xml","/images/capture.jpg/else","/images/%2e%2e/secret"})bad("cn.dot.budget.files",path,"r");
  for(String mode:new String[]{"w","rw","wt","rwt","a"})bad("cn.dot.budget.files","/apk/update.apk",mode);
  bad("evil","/images/capture.jpg","r");bad("cn.dot.budget.files:80","/images/capture.jpg","r");bad("cn.dot.budget.files","/images/capture.jpg",null);
  System.out.println("PASS: "+checks+" exact-path/private-provider access assertions");
 }
}
