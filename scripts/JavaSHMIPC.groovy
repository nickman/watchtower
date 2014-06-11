/*
    IPC Header:
     - Lock: byte, 0 unlocked, 1 locked by JVM1, 2 locked by JVM2
     - Participants: long, long, the PIDS of the participating JVMs
     - Last Write:  timestamp
     - Total Body Space: long 
     - Content:
         - From --> To, long, long  (from --> to)
         - Message Size:  long
         - Message Type: int
     - Offset of Body: long
     IPC Body:
     
*/

import java.nio.*;
import java.nio.channels.*;
import java.lang.management.*;
pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

println "PID: $pid";
File f = new File("/run/shm/java-ipc-$pid");
if(!f.exists()) {
    created = f.createNewFile();
    println "Created File:$f";
}
raf = null;
fc = null;
try {
    raf = new RandomAccessFile(f, "rw");
    fc = raf.getChannel();
    mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, 2048).load();
    println mbb.getClass().getSuperclass().getName();
    Buffer.class.getDeclaredFields().each() {
        println it;
    }
    addressField = Buffer.class.getDeclaredField("address");
    addressField.setAccessible(true);
    long address = addressField.get(mbb);
    println "IPC Channel Open. Address: ${address}";
    mbb.putLong(0, System.nanoTime());
    mbb.force();
    fc.force(true);
    f.setLastModified(System.currentTimeMillis());
} finally {
    if(fc!=null) try { fc.close(); } catch (e) {}
    if(raf!=null) try { raf.close(); } catch (e) {}
}