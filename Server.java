import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Server extends Node {
	static final int DEFAULT_PORT = 50001;
	static final int DEFAULT_CLIENT_PORT = 50000;

	private HashMap<String, ArrayList<InetSocketAddress>> producerSubscribers = new HashMap<>();
	/*
	 *
	 */
	Server(int port) {
		try {
			socket= new DatagramSocket(port);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/**
	 * Assume that incoming packets contain a String and print the string.
	 */
	public void onReceipt(DatagramPacket packet) {
		try {
			System.out.println("packet recieved");
			ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
			byte packetType = buffer.get();

			// Prepare a response packet to send back
			DatagramPacket responsePacket;
			ByteBuffer responseBuffer;
			byte[] responseMessage;
			String message = "";  // This will be set based on the case

			switch (packetType) {
				case MessageType.NEW_PRODUCER: {
					byte[] producerIdBytes = new byte[4]; // Assuming producerId is 4 bytes for simplicity
					buffer.get(producerIdBytes);
					String producerId = new String(producerIdBytes);
					System.out.println("New Producer with ID: " + producerId);
					break;
				}
				case MessageType.DATA: {
					// Extract producerId from buffer
					byte[] producerIdBytes = new byte[4]; // Assuming producerId is 4 bytes for simplicity
					buffer.get(producerIdBytes);
					String producerId = new String(producerIdBytes);
					System.out.println(producerId);

					// Extract the data message after producerId
					byte[] dataBytes = new byte[buffer.remaining()]; // Assuming the rest of the buffer is the data
					buffer.get(dataBytes);
					String dataMessage = new String(dataBytes);
					dataMessage = removeNonPrintable(dataMessage);
					System.out.println("Received data from producer: " + dataMessage);
					// Forward this data to all clients that are subscribed to this producer
					forwardDataToSubscribersNew(producerId, dataMessage);
					break;
				}
				case MessageType.SUBSCRIBE: {
					byte[] producerIdBytes = new byte[4]; // Assuming producerId is 4 bytes for simplicity
					buffer.get(producerIdBytes);
					String producerId = new String(producerIdBytes);

					producerSubscribers.putIfAbsent(producerId, new ArrayList<>());
					producerSubscribers.get(producerId).add((InetSocketAddress) packet.getSocketAddress());
					message = "Successfully subscribed to " + producerId;
					sendResponse(packet.getSocketAddress(), message);
					printSubscribers();
					break;
				}
				case MessageType.UNSUBSCRIBE: {
					byte[] unProducerIdBytes = new byte[4]; // Same assumption for the size
					buffer.get(unProducerIdBytes);
					String unProducerId = new String(unProducerIdBytes);

					if (producerSubscribers.containsKey(unProducerId)) {
						producerSubscribers.get(unProducerId).remove(packet.getSocketAddress());
						message = "Successfully unsubscribed from  " + unProducerId;
						sendResponse(packet.getSocketAddress(), message);
						printSubscribers();
					}
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    // This method sends a response packet back to the client
    private void sendResponse(SocketAddress target, String message) {
        try {
			DatagramPacket response;
			response= new AckPacketContent(message).toDatagramPacket();
			response.setSocketAddress(target);
			socket.send(response);
			System.out.println("sent to address : " + target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	// change from ackpacketcontent to new type of packet content for producers data
	private void forwardDataToSubscribersNew(String producerId, String dataMessage) {
		if (producerSubscribers.containsKey(producerId)) {
			System.out.println("Found subscribers for producer: " + producerId);
			ArrayList<InetSocketAddress> subscribers = producerSubscribers.get(producerId);

			// Create a ByteBuffer to combine the header and data for forwarding
			ByteBuffer buffer = ByteBuffer.allocate(1 + dataMessage.length());
			buffer.put(MessageType.DATA); // Adding the MessageType.DATA header
			buffer.put(dataMessage.getBytes());

			byte[] combinedData = buffer.array();

			for (InetSocketAddress subscriberAddress : subscribers) {
				try {
					DatagramPacket frame;
					frame= new AckPacketContent(dataMessage).toDatagramPacket();
					frame.setSocketAddress(subscriberAddress);
					socket.send(frame);
					System.out.println("sent to address : " + subscriberAddress);

				} catch (IOException e) {
					System.err.println("Error sending data to subscriber: " + e.getMessage());
				}
			}
		}
		else
			System.out.println("No subscribers found for producer: " + producerId);
	}

	private void forwardDataToSubscribers(String producerId, String dataMessage) {
		System.out.println(dataMessage);
		if (producerSubscribers.containsKey(producerId)) {
			System.out.println("Found subscribers for producer: " + producerId);
			ArrayList<InetSocketAddress> subscribers = producerSubscribers.get(producerId);

			// Create a ByteBuffer to combine the header and data for forwarding
			ByteBuffer buffer = ByteBuffer.allocate(1 + dataMessage.length());
			buffer.put(MessageType.DATA); // Adding the MessageType.DATA header
			buffer.put(dataMessage.getBytes());

			byte[] combinedData = buffer.array();

			for (InetSocketAddress subscriberAddress : subscribers) {
				try {
					// Create a DatagramPacket with the combined data and send it to the subscriber
					String combinedDataString = new String(combinedData);
					DatagramPacket dataPacket = new DatagramPacket(combinedData, combinedData.length, subscriberAddress);
					// TESTING FORWARDING TO SUBSCRIBERS
					socket.send(dataPacket);
					//sendResponse(subscriberAddress, combinedDataString);
				} catch (IOException e) {
					System.err.println("Error sending data to subscriber: " + e.getMessage());
				}
			}
		}
		else
			System.out.println("No subscribers found for producer: " + producerId);
	}

	/**
	 * Prints out the current list of subscribers for each producer.
	 */
	private void printSubscribers() {
		System.out.println("Current Subscribers List:");
		for (Map.Entry<String, ArrayList<InetSocketAddress>> entry : producerSubscribers.entrySet()) {
			String producerId = entry.getKey();
			ArrayList<InetSocketAddress> subscribers = entry.getValue();

			System.out.print("Producer " + producerId + " has subscribers: ");
			for (InetSocketAddress subscriberAddress : subscribers) {
				System.out.print(subscriberAddress + " ");
			}
			System.out.println();
		}
	}

	private String removeNonPrintable(String data){
		String cleanedData = data.replaceAll("[^\\x20-\\x7E]", "");
		return cleanedData;
	}


	private void handleSubscribe(String message) {
		System.out.println("Received subscribe message: " + message);
		// Handle the subscribe logic here...
	}

	private void handleUnsubscribe(String message) {
		System.out.println("Received unsubscribe message: " + message);
		// Handle the unsubscribe logic here...
	}


	public synchronized void start() throws Exception {
		System.out.println("Waiting for contact");
		this.wait();
	}

	/*
	 *
	 */
	public static void main(String[] args) {
		try {
			(new Server(DEFAULT_PORT)).start();
			System.out.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}
