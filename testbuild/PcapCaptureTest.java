import com.nexvary.trafficlab.packet.BoundedPcapCapture;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

public class PcapCaptureTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Path path=Files.createTempFile("nexvary-pcap", ".pcap");
        byte[] ipv4=new byte[]{0x45,0,0,4},ipv6=new byte[]{0x60,1,2,3,4,5};
        try{
            BoundedPcapCapture capture=new BoundedPcapCapture(path.toFile(),24+16+ipv4.length+16+ipv6.length);
            capture.record(ipv4,ipv4.length,1234567);capture.record(ipv6,ipv6.length,1234999);
            capture.record(ipv4,ipv4.length,1235000);check(capture.status().startsWith("LIMIT_REACHED"),"must stop at byte budget");
            capture.close();capture.close();check(capture.packets()==2,"must not overwrite packets or record after close");
            ByteBuffer data=ByteBuffer.wrap(Files.readAllBytes(path)).order(ByteOrder.LITTLE_ENDIAN);
            check(data.getInt()==0xa1b2c3d4,"magic");check(data.getShort()==2&&data.getShort()==4,"version");data.position(16);
            check(data.getInt()==65535&&data.getInt()==101,"raw IP link type and snap length");
            for(byte[] expected:new byte[][]{ipv4,ipv6}){
                check(data.getInt()==1234,"seconds");int micros=data.getInt();check(micros==567000||micros==999000,"microseconds");
                check(data.getInt()==expected.length&&data.getInt()==expected.length,"complete packet lengths");byte[] actual=new byte[expected.length];data.get(actual);check(Arrays.equals(expected,actual),"byte exact packet");
            }
            check(!data.hasRemaining(),"bounded complete records");
            BoundedPcapCapture invalid=new BoundedPcapCapture(path.toFile(),100);invalid.record(ipv4,5,0);check(invalid.status().startsWith("ERROR"),"invalid counts rejected");invalid.close();
            check(Files.size(path)==24,"new recording replaces old file");
        }finally{Files.deleteIfExists(path);}
        System.out.println("PcapCaptureTest OK");
    }
}
