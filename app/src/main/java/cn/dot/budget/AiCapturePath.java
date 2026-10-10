package cn.dot.budget;

import java.io.FileNotFoundException;
import java.util.*;

/** Each camera grant refers to one pre-created, never-reused cache file. */
public final class AiCapturePath {
 public static final int MAX_ACTIVE=8;
 private static final String UUID="[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";
 private static final Set<String> active=new HashSet<>();
 private AiCapturePath(){}
 public static boolean isFileName(String name){return name!=null&&name.matches("food-capture-"+UUID+"\\.jpg");}
 public static String path(String name)throws FileNotFoundException{if(!isFileName(name))throw new FileNotFoundException("Unknown capture file");return "/images/"+name.substring(5);}
 public static String fileName(String path)throws FileNotFoundException{if(path==null||!path.matches("/images/capture-"+UUID+"\\.jpg"))throw new FileNotFoundException("Unknown capture path");return "food-"+path.substring("/images/".length());}
 public static synchronized String acquire(){if(active.size()>=MAX_ACTIVE)throw new IllegalStateException("Too many active captures");String name="food-capture-"+java.util.UUID.randomUUID().toString()+".jpg";active.add(name);return name;}
 public static synchronized boolean isActive(String name){return active.contains(name);}
 public static synchronized void release(String name){active.remove(name);}
}
