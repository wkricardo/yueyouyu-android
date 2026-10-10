package cn.dot.budget;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Native source callbacks never borrow whichever request happens to be active later. */
public final class AiSourceRegistry {
 public static final int FIRST=1000,LAST=65534;
 private static final AtomicInteger next=new AtomicInteger(FIRST);
 public static final class Launch {public final int code;public final AiRequestSession.Ticket ticket;public final String kind;private Launch(int code,AiRequestSession.Ticket ticket,String kind){this.code=code;this.ticket=ticket;this.kind=kind;}}
 public interface FloorStore {boolean persist(int nextCode);}
 private final Map<Integer,Launch> pending=new HashMap<>();
 public static int floor(){return next.get();}
 public static void advanceTo(int floor){if(floor<FIRST||floor>LAST+1)throw new IllegalArgumentException("Source code floor invalid");synchronized(next){next.accumulateAndGet(floor,Math::max);}}
 public static boolean isSourceCode(int code){return code>=FIRST&&code<=LAST;}
 public synchronized Launch reserve(AiRequestSession.Ticket ticket,String kind){return reserve(ticket,kind,null);}
 public synchronized Launch reserve(AiRequestSession.Ticket ticket,String kind,FloorStore store){if(ticket==null||!Arrays.asList("statement_image","food_image","food_camera").contains(kind)||pending.size()>=32)throw new IllegalArgumentException("Source launch unavailable");synchronized(next){int code=next.get();if(code>LAST)throw new IllegalStateException("Source codes exhausted");next.set(code+1);if(store!=null&&!store.persist(code+1))throw new IllegalStateException("Source floor could not be stored");Launch launch=new Launch(code,ticket,kind);pending.put(code,launch);return launch;}}
 public synchronized Launch take(int code){return pending.remove(code);}
 public synchronized void revoke(AiRequestSession.Ticket ticket){pending.values().removeIf(launch->launch.ticket==ticket);}
 public synchronized void reset(){pending.clear();}
}
