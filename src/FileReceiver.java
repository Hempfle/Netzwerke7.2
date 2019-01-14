import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public class FileReceiver {
    public static int PORT = 5000;
    public static int SOCKET_TIMEOUT = 10000;
    public static String targetPath = "C:\\Users\\chris\\Desktop\\OperationArrowhead\\OperationArrowhead\\src\\";
    private DatagramSocket socket;
    public boolean ack = true;
    private State currentState;
    // 2D array defining all transitions that can occur
    private Transition[][] transition;
    List<byte[]> bodys = new ArrayList<byte[]>();


    enum State {
        WFOR_ZERO, WFOR_ONE
    }

    enum Msg {
        SEND_ACK_ZERO, SEND_ACK_ONE
    }

    abstract class Transition {
        abstract public State execute(Msg input, Packet packet);
    }


    public FileReceiver() throws SocketException, UnknownHostException {
        this.socket = new DatagramSocket(PORT);
        socket.setSoTimeout(SOCKET_TIMEOUT);
        //InetAddress IPAddress = InetAddress.getByName("localhost");
        //socket.connect(IPAddress, 8888);


        currentState = State.WFOR_ZERO;


        transition = new Transition[State.values().length][Msg.values().length];
        transition[State.WFOR_ZERO.ordinal()][Msg.SEND_ACK_ZERO.ordinal()] = new SendAckZero();
        transition[State.WFOR_ONE.ordinal()][Msg.SEND_ACK_ONE.ordinal()] = new SendAckOne();

        System.out.println("New current state: " + currentState);
    }

    public byte[] getBodyData(Packet pkt) {
        byte[] body = new byte[pkt.header.bodylength];
        System.arraycopy(pkt.body, 0, body, 0, pkt.header.bodylength);
        return body;
    }

    public boolean sendPackage(DatagramPacket pkt) {
        try {
            String ack = currentState.equals(State.WFOR_ZERO) ? "ACK0" : "ACK1";
            System.out.println("R: SENT " + ack);
            socket.send(pkt);
            return true;
        } catch (SocketTimeoutException e) {
            System.out.println("TIMEOUT!");
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        FileReceiver r = new FileReceiver();
        r.start();
    }

    public void start() throws IOException {
        Instant firstTime = Instant.now();
        try {
            while (true) {
                DatagramPacket datagram = new DatagramPacket(new byte[Short.MAX_VALUE], Short.MAX_VALUE);
                this.socket.receive(datagram);
                System.out.println("R: RECEIVE PKT");


                Packet packet = Packet.deserialize(datagram.getData());

                if (packet.header.syn) {
                    datagram.getAddress();
                    socket.connect(datagram.getSocketAddress());
                }

                short recChecksum = packet.header.checksum;
                packet.header.checksum = 0;
                short calcChecksum = packet.header.createChecksum(packet.serialize());

                // Falls ein Packet doppelt kommt muss hier geprüft werden,
                //true = 0

                if (recChecksum == calcChecksum && packet.header.zeroOrOne && currentState.equals(State.WFOR_ZERO)) {
                    processMsg(Msg.SEND_ACK_ZERO, packet);
                } else if (recChecksum == calcChecksum && !packet.header.zeroOrOne && currentState.equals(State.WFOR_ONE)) {
                    processMsg(Msg.SEND_ACK_ONE, packet);
                } else {
                    continue;
                }

                // da sonst der body doppelt abgespeichert wird
                //bodys.add(getBodyData(packet));
/*
				// ACK SENDEN
				Packet ackPkt = new Packet(false, false, false, ack, new byte[0]);
				ack = !ack;
				byte[] pkt = ackPkt.serialize();
				sendPackage(new DatagramPacket(pkt, pkt.length));
*/
            }
        } catch (SocketTimeoutException ex) {
            System.out.println("ReciverTIMEOUT!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Instant end = Instant.now();
        Duration time = Duration.between(firstTime, end);

        int fileSize = 0;
        for (int i = 0; i < bodys.size(); i++) {
            fileSize += bodys.get(i).length;
        }

        byte[] fileBody = new byte[fileSize];

        int start = 0;
        for (int i = 0; i < bodys.size(); i++) {
            System.arraycopy(bodys.get(i), 0, fileBody, start, bodys.get(i).length);
            start += bodys.get(i).length;
        }


        File directory = new File(targetPath);
        directory.mkdirs();

        File file = new File(targetPath + "file.jpg");
        if (!file.exists()) {
            file.createNewFile();
        }

        FileOutputStream fileOut = new FileOutputStream(file);
        fileOut.write(fileBody);
        fileOut.flush();
        fileOut.close();

        System.out.println("Goodput is " + ((fileBody.length * 8)/(time.getSeconds() * 1024) - (SOCKET_TIMEOUT / 1000)) + " Mbit/s" );
    }

    public void processMsg(Msg input, Packet packet) {
        System.out.println("INFO Received " + input + " in state " + currentState);
        Transition trans = transition[currentState.ordinal()][input.ordinal()];
        if (trans != null) {
            currentState = trans.execute(input, packet);
        }
        System.out.println("INFO State: " + currentState);
    }

    class SendAckZero extends Transition {
        @Override
        public State execute(Msg input, Packet packet) {
            // ACK SENDEN
            Packet ackPkt = new Packet(false, false, false, ack, new byte[0]);
            ack = !ack;
            byte[] pkt = ackPkt.serialize();
            sendPackage(new DatagramPacket(pkt, pkt.length));
            bodys.add(getBodyData(packet));

            return State.WFOR_ONE;
        }
    }

    class SendAckOne extends Transition {
        @Override
        public State execute(Msg input, Packet packet) {
            // ACK SENDEN
            Packet ackPkt = new Packet(false, false, false, ack, new byte[0]);
            ack = !ack;
            byte[] pkt = ackPkt.serialize();
            sendPackage(new DatagramPacket(pkt, pkt.length));
            bodys.add(getBodyData(packet));

            return State.WFOR_ZERO;
        }
    }
}

