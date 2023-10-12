import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;

public class FramePacketContent extends PacketContent {
    String message;

    FramePacketContent(String message) {
        type = DATA; // Assuming you've defined a DATA constant in PacketContent
        this.message = message;
    }

    protected FramePacketContent(ObjectInputStream oin){
        try{
            type = DATA;
            message = oin.readUTF();
        }
        catch(Exception e){e.printStackTrace();}
    }

    protected void toObjectOutputStream(ObjectOutputStream out) {
        try {
            out.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        return "DATA: " + message;
    }
}
