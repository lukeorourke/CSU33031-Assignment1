import java.io.File;
import java.io.FileInputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;


public class Producer extends Node {
    static final int DEFAULT_SRC_PORT = 50002;  // Unique from Client and Server
    static final int DEFAULT_DST_PORT = 50001;
    static final String DEFAULT_DST_NODE = "server";
    static InetSocketAddress dstAddress;

    private static String producerId = "DEFAULT";

    private final String predefinedMessage = "Producer's pre-defined message!"; // This is our predefined message for demonstration

    Producer(String dstHost, int dstPort, int srcPort, String producerId) {
        try {
            dstAddress = new InetSocketAddress(dstHost, dstPort);
            socket = new DatagramSocket(srcPort);
            this.producerId = producerId;
            listener.go();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void onReceipt(DatagramPacket packet) {
        PacketContent content = PacketContent.fromDatagramPacket(packet);
        System.out.println(content.toString());
        this.notify();
    }

    public synchronized void start() throws Exception {
            notifyServerOfNewProducer();
            Scanner scanner = new Scanner(System.in);
            String userInput;
            do {
                System.out.print("Enter the frame name (e.g. frame001) or type 'exit' to quit: ");
                userInput = scanner.nextLine();

                if (!userInput.equals("exit")) {
                    byte[] frameData = readFrame(userInput);
                    sendFrameToServer(frameData);
                    Thread.sleep(500); // Add a delay between sending frames, adjust as needed
                }
            } while (!userInput.equals("exit"));
            scanner.close();
        }


    private void sendToServer(String data) {
        try {
            byte[] producerBytes = producerId.getBytes();
            ByteBuffer headerBuffer = createHeader(MessageType.DATA, producerBytes, (byte) 1); // Assuming streamId is 1

            byte[] payload = data.getBytes();
            ByteBuffer buffer = ByteBuffer.allocate(headerBuffer.capacity() + payload.length);
            buffer.put(headerBuffer.array());
            buffer.put(payload);

            DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.capacity(), dstAddress);
            socket.send(packet);
            System.out.println("Data sent to server");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFrameToServer(byte[] data) {
        try {
            byte[] producerBytes = producerId.getBytes();
            ByteBuffer headerBuffer = createHeader(MessageType.DATA, producerBytes, (byte) 1); // Assuming streamId is 1

            ByteBuffer buffer = ByteBuffer.allocate(headerBuffer.capacity() + data.length);
            buffer.put(headerBuffer.array());
            buffer.put(data);

            DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.capacity(), dstAddress);
            socket.send(packet);
            System.out.println("Frame sent to server");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void notifyServerOfNewProducer() {
        try {
            byte[] producerBytes = producerId.getBytes();
            ByteBuffer headerBuffer = createHeader(MessageType.NEW_PRODUCER, producerBytes, (byte) 1); // Assuming streamId is 1

            ByteBuffer buffer = ByteBuffer.allocate(headerBuffer.capacity());
            buffer.put(headerBuffer.array());

            DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.capacity(), dstAddress);
            socket.send(packet);
            System.out.println("Notified server of new producer: " + producerId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static ByteBuffer createHeader(byte messageType, byte[] producerId, byte streamId) {
        ByteBuffer headerBuffer = ByteBuffer.allocate(1 + producerId.length + 1);
        headerBuffer.put(messageType);
        headerBuffer.put(producerId);
        headerBuffer.put(streamId);
        return headerBuffer;
    }

    // read frame data
    private byte[] readFrame(String frameName) {
        String baseFolderPath = "First20Frames";
        String filename = String.format("%s/%s.png", baseFolderPath, frameName);

        byte[] frameData = new byte[0]; // Default empty array
        try {
            File frameFile = new File(filename);
            FileInputStream fis = new FileInputStream(frameFile);
            frameData = new byte[(int) frameFile.length()];
            fis.read(frameData);
            fis.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return frameData;
    }

    private static String generateID(){
        Random random = new Random();
        StringBuilder sb = new StringBuilder(4);

        for (int i = 0; i < 4; i++) {
            int randomNum = random.nextInt(52); // Random number from 0 to 51
            if (randomNum < 26) {
                sb.append((char) ('A' + randomNum));
            } else {
                sb.append((char) ('a' + (randomNum - 26)));
            }
        }
        String randomString = sb.toString();
        return randomString;
    }

    public static void main(String[] args) {
        try {
            String producerID = generateID();
            (new Producer(DEFAULT_DST_NODE, DEFAULT_DST_PORT, DEFAULT_SRC_PORT, producerID)).start();
            System.out.println("Producer completed");
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }
}

