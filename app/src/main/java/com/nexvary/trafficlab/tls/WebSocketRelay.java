package com.nexvary.trafficlab.tls;
import com.nexvary.trafficlab.core.WebSocketWire;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Two bounded full-duplex directions; preserves masking, fragments and control bytes. */
final class WebSocketRelay {
    static void run(Socket client,Socket server,Consumer<String> preview)throws Exception{
        ExecutorService pool=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"nexvary-websocket");t.setDaemon(true);return t;});
        CompletableFuture<Void> up=CompletableFuture.runAsync(()->pump(client,server,true,preview),pool),down=CompletableFuture.runAsync(()->pump(server,client,false,preview),pool);
        try{CompletableFuture.anyOf(up,down).get(120,TimeUnit.SECONDS);
            try{CompletableFuture.allOf(up,down).get(1,TimeUnit.SECONDS);}catch(TimeoutException ignored){}
        }finally{try{client.close();}catch(IOException ignored){}try{server.close();}catch(IOException ignored){}pool.shutdownNow();}
    }
    private static void pump(Socket source,Socket target,boolean client,Consumer<String> preview){
        WebSocketWire.Messages messages=new WebSocketWire.Messages();long total=0;
        try{for(int count=0;count<4096;count++){
            WebSocketWire.Frame frame=WebSocketWire.read(source.getInputStream(),client);if(frame==null)return;
            total+=frame.wire().length;if(total>16L*1024*1024)throw new IOException("WebSocket session exceeds 16 MiB per direction");
            String value=messages.accept(frame);target.getOutputStream().write(frame.wire());target.getOutputStream().flush();
            if(value!=null)preview.accept((client?"CLIENT → SERVER · ":"SERVER → CLIENT · ")+value);
            if(frame.opcode()==8)return;
        }throw new IOException("WebSocket session frame limit");}catch(IOException e){throw new CompletionException(e);}
    }
}
