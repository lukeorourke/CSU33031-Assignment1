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

    private final String predefinedMessage = "Producer's pre-defined message!"; // This is my predefined message for demonstration

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
                System.out.println("Do you want to send [1] Frame, [2] Audio or type 'exit' to quit?");
                userInput = scanner.nextLine();

                if (userInput.equals("1")) {
                    for (int i = 1; i <= 20; i++) {
                        String frameName = String.format("frame%03d", i);
                        System.out.println("Sending " + frameName);

                        byte[] frameData = readFrame(frameName);
                        sendFrameToServer(frameData);

                        Thread.sleep(10); // Add a delay between sending frames
                    }

                } else if (userInput.equals("2")) {
                    for (int i = 1; i <= 20; i++) {
                        String audioName = String.format("audio%03d", i);
                        System.out.println("Sending " + audioName);

                        byte[] audioData = readAudioFile(audioName);
                        sendAudioToServer(audioData);

                        Thread.sleep(10); // Add a delay between sending audios
                    }
                }
            } while (!userInput.equals("exit"));
            scanner.close();
    }


    private void sendAudioToServer(byte[] data) {
        try {
            byte[] producerBytes = producerId.getBytes();
            ByteBuffer headerBuffer = createHeader(MessageType.AUDIO, producerBytes, (byte) 1); // Assuming streamId is 1

            ByteBuffer buffer = ByteBuffer.allocate(headerBuffer.capacity() + data.length);
            buffer.put(headerBuffer.array());
            buffer.put(data);

            DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.capacity(), dstAddress);
            socket.send(packet);
            System.out.println("Audio sent to server");
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

    // Modified to read audio files
    private byte[] readAudioFile(String audioFileName) {
        String baseFolderPath = "AudioFiles";
        String filename = String.format("%s/%s.wav", baseFolderPath, audioFileName);

        byte[] audioData = new byte[0];
        try {
            File audioFile = new File(filename);
            FileInputStream fis = new FileInputStream(audioFile);
            audioData = new byte[(int) audioFile.length()];
            fis.read(audioData);
            fis.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return audioData;
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

