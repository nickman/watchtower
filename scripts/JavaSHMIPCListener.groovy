import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.lang.management.*;
import static java.nio.file.StandardWatchEventKinds.*;

pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

println "PID: $pid";
File shmDir = new File("/run/shm");
File ipcFile = null;
shmDir.listFiles().each() {
    if(it.getName().startsWith("java-ipc-")) {
        ipcFile = it;
    }
}
if(ipcFile == null) {
    println "No Java IPC Files Found";
    return;
}
path = Paths.get(shmDir.getAbsolutePath());
println "Listening on $ipcFile, Path: [$path]";
WatchKey wk = null;

watchService = null;
try {
    watchService = FileSystems.getDefault().newWatchService();
    path.register(watchService, ENTRY_MODIFY);
    Thread.startDaemon("FileEventPoller") {
        println "Started FileEventPoller Thread";
        while(true) {
            try {
                println "Polling for events....";
                wk = watchService.take();        
                events = wk.pollEvents();
                println "Processing ${events.size()} events";    
                events.each() { event ->
                    println "IPC Event: ${event.kind()} - [${event.context()}]";
                }
                boolean valid = wk.reset();
                 if (!valid) {
                     println "Invalid Watch Key";
                     break;
                 }                
            } catch (InterruptedException ie) {
                println "Stopped FileEventPoller";
                break;
            } catch (ClosedWatchServiceException cwse) {
                println "Stopped FileEventPoller";
                break;
            }                
        }

        
    }
    try {
        Thread.currentThread().join();
    } catch (e) {
        println "Watch Service Stopped";
    }
} finally {
    if(wk!=null) try { wk.cancel(); } catch (e) {}    
    if(watchService!=null) try { watchService.close(); } catch (e) {}

}    



