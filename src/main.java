import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

public class main {


	
	
	public static void main(String[] args) throws InterruptedException {		
		Thread server = new Thread(() -> {
			try {
				FileReceiver.main(null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
		Thread client = new Thread(() -> {
			try {
				FileSender.main(null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
		server.start();
		Thread.sleep(2000);
		client.start();
		server.join();
	}

}
