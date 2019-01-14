import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.zip.CRC32;

public class Packet {
    public Header header;
    public byte[] body;
	
    
    
    
    
    
    
	public Packet(boolean zeroOrOne, boolean fin, boolean syn, boolean ack, byte[] body) {
		this.body = body;
        header = new Header(zeroOrOne, fin, syn, ack, (short) body.length);
        byte[] checksumBytes = new byte[header.serialize().length + body.length];
        System.arraycopy(header.serialize(), 0, checksumBytes, 0, header.serialize().length);
        System.arraycopy(body, 0, checksumBytes, header.serialize().length, body.length);
        short checksum = header.createChecksum(checksumBytes);
        header = new Header(zeroOrOne, fin, syn, ack, (short) body.length, checksum);
    }
	
	public Packet(Header header, byte[] body) {
		this.header = header;
		this.body = body;
	}
	
	public byte[] serialize() {
		byte[] data = new byte[Header.HEADER_SIZE + body.length];
		byte[] headerData = header.serialize();
		System.arraycopy(headerData, 0, data, 0, Header.HEADER_SIZE);
		System.arraycopy(body, 0, data, Header.HEADER_SIZE, body.length);

		return data;		
	}
	
	public static Packet deserialize(byte[] data) {
		byte[] headerData = new byte[Header.HEADER_SIZE];
		System.arraycopy(data, 0, headerData, 0, Header.HEADER_SIZE);

		Header header = Header.deserialize(headerData);
		
		byte[] bodyData = new byte[header.bodylength];
		System.arraycopy(data, Header.HEADER_SIZE, bodyData, 0, header.bodylength);

		return new Packet(header, bodyData);
	}
	
	
	static class Header {
		public boolean zeroOrOne;
		public boolean fin;
		public boolean syn;
		public boolean ack;
		public short bodylength;
		public short checksum = 0;
		public static final int HEADER_SIZE = 7;

        public Header(boolean zeroOrOne, boolean fin, boolean syn, boolean ack, short bodylength) {
            super();
            this.zeroOrOne = zeroOrOne;
            this.fin = fin;
            this.syn = syn;
            this.ack = ack;
            this.bodylength = bodylength;

        }

		public Header(boolean zeroOrOne, boolean fin, boolean syn, boolean ack, short bodylength, short checksum) {
			super();
			this.zeroOrOne = zeroOrOne;
			this.fin = fin;
			this.syn = syn;
			this.ack = ack;
			this.bodylength = bodylength;
			this.checksum = checksum;
		}
		
		public static Header deserialize(byte[] header) {
			ByteBuffer bb = ByteBuffer.allocate(7);
	    	bb.put(header);
	    	bb.position(0);
	    	
	    	byte[] bitSetByte = new byte[1];
	    	bb.get(bitSetByte, 0, 1);
	    	BitSet bitSet = BitSet.valueOf(bitSetByte);

	    	boolean zeroOrOne = bitSet.get(0);
	    	boolean fin = bitSet.get(1);
	    	boolean syn = bitSet.get(2);
	    	boolean ack = bitSet.get(3);


			short bodyLength = bb.getShort();
			short checksum = bb.getShort();

			return new Header(zeroOrOne, fin, syn, ack, bodyLength, checksum);
		}

        public short createChecksum(byte[] input) {
            CRC32 crc = new CRC32();
            crc.update(input);
            long crcSum = crc.getValue();
            while (crcSum > Short.MAX_VALUE || crcSum < Short.MIN_VALUE) {
                crcSum = crcSum/10;
            }
            //checksum = (short) crcSum;
            return (short)crcSum;
        }
		
		public byte[] serialize() {			
			BitSet bs = new BitSet();
			bs.set(0, zeroOrOne);
			bs.set(1, fin);
			bs.set(2, syn);
			bs.set(3, ack);

	        byte[] bitSet = bs.toByteArray();
	        if (bitSet.length != 1) {
	        	bitSet = new byte[1];
	        }
			
	    	ByteBuffer bb = ByteBuffer.allocate(7);
	        bb.put(bitSet);
	        bb.putShort(bodylength);
	        bb.putShort(checksum);

	        return bb.array();
		}
	}
}
