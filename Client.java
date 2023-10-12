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
			//byte packetType = buffer.get();

			//switch (packetType) {
			///	case MessageType.DATA: {
			// Extract the data message
			//		byte[] dataBytes = new byte[buffer.remaining()]; // Assuming the rest of the buffer is the data
			//		buffer.get(dataBytes);
			//	String dataMessage = new String(dataBytes);
			//	System.out.println("Received data: " + dataMessage);
			//	break;
			//	}
			//	default: {
			PacketContent content = PacketContent.fromDatagramPacket(packet);
			System.out.println(content.toString());
			//	break;
			//	}
			//	}

			this.notify();
	}

	/**
	 * Sender Method
	 *
	 */

	public synchronized void start() throws Exception {
		Scanner scanner = new Scanner(System.in);

		while (true) {
			this.wait(5000);
			System.out.print("Enter a command (subscribe, unsubscribe, listen, or exit ) :");
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
				this.wait(5000);
			}

		}

		scanner.close();
	}

	public void sendSubscribeMessage() {
		try {
			byte[] producerId = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };
			ByteBuffer headerBuffer = createHeader(MessageType.SUBSCRIBE, producerId, (byte) 2);

			byte[] payload = "Client wants to subscribe".getBytes();
			ByteBuffer buffer = ByteBuffer.allocate(headerBuffer.capacity() + payload.length);
			buffer.put(headerBuffer.array());
			buffer.put(payload);

			DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.capacity(), InetAddress.getByName(dstAddress.getHostName()), dstAddress.getPort());
			socket.send(packet);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendUnsubscribeMessage() {
		try {
			byte[] producerId = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };
			ByteBuffer headerBuffer = createHeader(MessageType.UNSUBSCRIBE, producerId, (byte) 2);

			byte[] payload = "Client wants to unsubscribe".getBytes();
			ByteBuffer buffer = ByteBuffer.allocate(headerBuffer.capacity() + payload.length);
			buffer.put(headerBuffer.array());
			buffer.put(payload);

			DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.capacity(), InetAddress.getByName(dstAddress.getHostName()), dstAddress.getPort());
			socket.send(packet);


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ByteBuffer createHeader(byte messageType, byte[] producerId, byte streamId) {
		ByteBuffer headerBuffer = ByteBuffer.allocate(6); // Assuming 1 byte for messageType, 4 bytes for producerId, and 1 byte for streamId
		headerBuffer.put(messageType);
		headerBuffer.put(producerId);
		headerBuffer.put(streamId);
		return headerBuffer;
	}

	/**
	 * Test method
	 *
	 * Sends a packet to a given address
	 */
	public static void main(String[] args) {
		try {
			(new Client(DEFAULT_DST_NODE, DEFAULT_DST_PORT, DEFAULT_SRC_PORT)).start();
			System.out.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}
