/**
 *
 */
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Scanner;


/**
 *
 * Client class
 *
 * An instance accepts user input
 *
 */
public class Client extends Node {
	static final int DEFAULT_SRC_PORT = 50000;
	static final int DEFAULT_DST_PORT = 50001;
	static final String DEFAULT_DST_NODE = "server";
	InetSocketAddress dstAddress;

	/**
	 * Constructor
	 *
	 * Attempts to create socket at given port and create an InetSocketAddress for the destinations
	 */
	Client(String dstHost, int dstPort, int srcPort) {
		try {
			dstAddress= new InetSocketAddress(dstHost, dstPort);
			socket= new DatagramSocket(srcPort);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/**
	 * Assume that incoming packets contain a String and print the string.
	 */
	public synchronized void onReceipt(DatagramPacket packet) {


		System.out.println("packet received");
		ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
		byte packetType = buffer.get();

		switch (packetType) {
			case MessageType.DATA:
			case MessageType.AUDIO:{
				//Extract the data message
				byte[] producerIdBytes = new byte[4]; // producerId is 4 bytes
				buffer.get(producerIdBytes);
				String producerId = new String(producerIdBytes, StandardCharsets.UTF_8);

				byte[] dataBytes = new byte[buffer.remaining()]; // the rest of the buffer is the data
				buffer.get(dataBytes);
				String dataMessage = new String(dataBytes);
				if(packetType == MessageType.DATA)
					System.out.println("Received frame from Streamer " + producerId + ": " + dataMessage);
				else if (packetType == MessageType.AUDIO)
					System.out.println("Received audio from Streamer " + producerId + ": " + dataMessage);
				break;
			}
			default: {
				PacketContent content = PacketContent.fromDatagramPacket(packet);
				System.out.println(content.toString());
				break;
			}
		}

		this.notify();
	}

	/**
	 * Sender Method
	 *
	 */

	public synchronized void start() throws Exception {
		Scanner scanner = new Scanner(System.in);

		while (true) {
			System.out.print("Enter a command (subscribe, unsubscribe, listen, or exit, e.g. subscribe abcd) :");
			System.out.flush();

			String message = scanner.nextLine();

			String[] inputParts = message.split(" ");
			String command = inputParts[0].toLowerCase();
			String producerId = inputParts.length > 1 ? inputParts[1] : "";

			if (command.equalsIgnoreCase("exit")) {
				break;
			} else if (command.equalsIgnoreCase("subscribe") || command.equalsIgnoreCase("unsubscribe")) {
				byte messageType = command.equalsIgnoreCase("subscribe") ? MessageType.SUBSCRIBE : MessageType.UNSUBSCRIBE;
				byte[] producerBytes = producerId.getBytes();
				ByteBuffer headerBuffer = createHeader(messageType, producerBytes, (byte) 2);

				byte[] payload = "This should be bytes of a video frame".getBytes();
				ByteBuffer buffer = ByteBuffer.allocate(headerBuffer.capacity() + payload.length);
				buffer.put(headerBuffer.array());
				buffer.put(payload);

				DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.capacity(), dstAddress);
				socket.send(packet);
				System.out.println("packet sent");
				this.wait();
			}else if (command.equalsIgnoreCase("listen")){
				System.out.println("listening for stream:");
				for(int i=0;i<20;i++) {
					this.wait(100);
				}
			}

		}

		scanner.close();
	}

	private ByteBuffer createHeader(byte messageType, byte[] producerId, byte streamId) {
		ByteBuffer headerBuffer = ByteBuffer.allocate(6); // Assuming 1 byte for messageType, 4 bytes for producerId, and 1 byte for streamId
		headerBuffer.put(messageType);
		headerBuffer.put(producerId);
		headerBuffer.put(streamId);
		return headerBuffer;
	}

	/**
	 * Generates a random port number between 50000 and 60000.
	 *
	 * @return Random port number between 50000 and 60000.
	 */
	public static int getRandomSrcPort() {
		Random rand = new Random();
		return 50000 + rand.nextInt(10001);
	}

	/**
	 * Test method
	 *
	 * Sends a packet to a given address
	 */
	public static void main(String[] args) {
		try {
			int randomPort = Client.getRandomSrcPort();
			(new Client(DEFAULT_DST_NODE, DEFAULT_DST_PORT, randomPort)).start();
			System.out.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}
