import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FileSender {
	private static final int PORT = 8888;
	private static final String HOST = "localhost";
	private static final int ACK_TIMEOUT = 3000;
	public static int RECEIVER_PORT = 5000; 
	public boolean expectedAck = true;
	private static final int PACKET_SIZE = 300;
	

	private DatagramSocket clientSocket;

	public FileSender() throws SocketException, UnknownHostException {
		clientSocket = new DatagramSocket(PORT);
		clientSocket.setSoTimeout(ACK_TIMEOUT);
		InetAddress IPAddress = InetAddress.getByName(HOST);
		clientSocket.connect(IPAddress, RECEIVER_PORT);
	}
	
	public boolean sendPackage(DatagramPacket pkt) {
		try {
			clientSocket.send(pkt);
			System.out.println("S: SENT PKT");
			
			byte[] ackBuffer = new byte[7];
			DatagramPacket rcvPkt = new DatagramPacket(ackBuffer, ackBuffer.length);
			clientSocket.receive(rcvPkt);
			System.out.println("S: RECEIVED ACK");

			Packet ackPkt = Packet.deserialize(rcvPkt.getData());

			short recChecksum = ackPkt.header.checksum;
			ackPkt.header.checksum = 0;
			short calcChecksum = ackPkt.header.createChecksum(ackPkt.serialize());

			if (ackPkt.header.ack == expectedAck && recChecksum == calcChecksum) {
				expectedAck = !expectedAck;
				return true;
			}
			return false;
			
        } catch (SocketTimeoutException e) {
            System.out.println("TIMEOUT!");
		    return false;
		} catch (IOException e) {
			return false;
		}	
	}
	
	public static void main(String[] args) throws IOException {
		FileSender s = new FileSender();
		//s.start("E:\\WALTERJ\\Desktop\\CHRIS.txt");
		//s.start("C:\\Users\\Public\\Pictures\\Sample Pictures\\Chrysanthemum.jpg");
		s.start("E:\\Bilder\\2008\\BEHAPPY.jpg");
		//s.start("E:\\Bilder\\2008\\text.txt");
	}
	
	public void start(String filePath) throws IOException {
		List<DatagramPacket> packages = getDatagramPackets(getFileBytes(filePath));
	
		for (int i = 0; i < packages.size(); i++) {
			boolean sending = true;
			while(sending) {

				//todo: hier abganfen und dann senden/bzw nicht senden?
				// duplizieren: Methode 2x aufrufen
				//Paket l�schen
				//ByteFehler

				sending = !sendPackage(packages.get(i));

			}
		}
	}
	
	private byte[] getFileBytes(String filePath) throws IOException {
		return Files.readAllBytes(new File(filePath).toPath());	
	}
	
	private boolean finalPackage(byte[] fileBytes) {
		return fileBytes.length % PACKET_SIZE != 0;
	}
	
	public List<DatagramPacket> getDatagramPackets(byte[] fileBytes) {
		int packagesCount = fileBytes.length / PACKET_SIZE;
		boolean endPacket = finalPackage(fileBytes); 		
		List<byte[]> byteBlocks = new ArrayList<byte[]>();
		byte[] body;
		
		// BODYS F�R PACKAGES
		for (int i = 0; i < packagesCount; i++) {
			body = new byte[PACKET_SIZE];
			System.arraycopy(fileBytes, i*PACKET_SIZE, body, 0, PACKET_SIZE);
			byteBlocks.add(body);
		}
		
		if (endPacket) {
			int smallEndPackageSize = fileBytes.length % PACKET_SIZE; 
			body = new byte[smallEndPackageSize];
			System.arraycopy(fileBytes, fileBytes.length - smallEndPackageSize, body, 0, smallEndPackageSize);
			byteBlocks.add(body);
			packagesCount++;
		}
		
		List<Packet> datagrams = new ArrayList<Packet>();

		for (int i = 0; i < packagesCount; i++) {
			byte[] bodyData = byteBlocks.get(i);
			if (i == 0) {
				datagrams.add(new Packet(i % 2 == 0, false, true, false, bodyData));
			} else {
				datagrams.add(new Packet(i % 2 == 0, false, false, false, bodyData));
			}
		}
		
		List<DatagramPacket> datagramPackets = new ArrayList<DatagramPacket>();

		for (int i = 0; i < packagesCount; i++) {
			byte[] pkt = datagrams.get(i).serialize();
			datagramPackets.add(new DatagramPacket(pkt, pkt.length));
		}
		
		return datagramPackets;		
	}
	
}
