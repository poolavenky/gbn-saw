


import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JLabel;

public class SAWInterface extends Frame implements ActionListener {

	private Label fSize;
	private TextField fSValue;
	private Label sentPacket;
	private TextField sentPacketValue;
	private Label actualSent;
	private TextField actualSentValue;
	private Label percenrageLoss;
	private TextField percentageLossValue;
	private Label lostPacketCount;
	private TextField lostPacketCountValue;
	private Label totalTimeTaken;
	private TextField totalTimeTakenValue;
	private ProtocolStats stats = new ProtocolStats(0L, 0L, 0L,0L);

	public SAWInterface() {
		setLayout(new FlowLayout());
		setTitle("Stop and Wait Runner");
		setSize(700, 500);

		recHostLabel = new Label("Reciever host");
		recHostValue = new TextField("localhost", 10);
		recHostValue.setEditable(true);
		add(recHostLabel);
		add(recHostValue);

		sPortLabel = new Label("Sender listening");
		sPortNoValue = new TextField("8889", 4);
		sPortNoValue.setEditable(true);
		add(sPortLabel);
		add(sPortNoValue);

		pLLabel = new Label("Packet loss [0-99]");
		pLValue = new TextField("80", 3);
		pLValue.setEditable(true);
		add(pLLabel);
		add(pLValue);

		rPortLabel = new Label("Reciever listening");
		rPortValue = new TextField("8889", 4);
		rPortValue.setEditable(true);
		add(rPortLabel);
		add(rPortValue);

		sFileLabel = new Label("Send File Path");
		sFileLocValue = new TextField("sample.txt", 30);
		recHostValue.setEditable(true);
		add(sFileLabel);
		add(sFileLocValue);

		rFileLabel = new Label("Save File Path");
		rFileLocValue = new TextField("sample.txt", 30);
		rFileLocValue.setEditable(true);
		add(rFileLabel);
		add(rFileLocValue);

		Label statistics = new Label(
				"-----------------------------------Sender Statistic----------------------------------");
		add(statistics);
		add(new JLabel(""), "span");
		fSize = new Label("File size in KB");
		add(fSize);
		fSValue = new TextField("", 8);
		fSValue.setEditable(false);
		add(fSValue);

		sentPacket = new Label("Total Packets");
		add(sentPacket);
		sentPacketValue = new TextField("", 8);
		sentPacketValue.setEditable(false);
		add(sentPacketValue);

		actualSent = new Label("Total Packet sent");
		add(actualSent);
		actualSentValue = new TextField("", 8);
		actualSentValue.setEditable(false);
		add(actualSentValue);

		percenrageLoss = new Label("Packet loss %");
		add(percenrageLoss);
		percentageLossValue = new TextField("", 3);
		percentageLossValue.setEditable(false);
		add(percentageLossValue);
       
		lostPacketCount = new Label("Lost Packet Count");
		add(lostPacketCount);
		lostPacketCountValue = new TextField("", 6);
		lostPacketCountValue.setEditable(false);
		add(lostPacketCountValue);
		
		totalTimeTaken = new Label("Total time in execution(in ms) : ");
		add(totalTimeTaken);
		totalTimeTakenValue = new TextField("", 6);
		totalTimeTakenValue.setEditable(false);
		add(totalTimeTakenValue);
		startButton = new Button("Start");
		startButton.addActionListener(this);
		add(startButton);

		setVisible(true);
	}

	public static void main(String[] args) {
		SAWInterface app = new SAWInterface();
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		try {
			int sport = Integer.parseInt(sPortNoValue.getText());
			int rport = Integer.parseInt(rPortValue.getText());
			int ploss = Integer.parseInt(pLValue.getText());
			SAWReciever reciever = new SAWReciever(rport, rFileLocValue.getText());
			SAWSen sender = new SAWSen(recHostValue.getText(), sport, sFileLocValue.getText(), ploss);
			Thread receiverThread = new Thread(reciever);
			receiverThread.start();
			Thread.sleep(1000);
			ProtocolStats stats = sender.senderExecuton();
			if (stats.getTotalPacket() != 0) {
				fSValue.setText(stats.getFileSize() + "");
				actualSentValue.setText(stats.getTotalSent() + "");
				sentPacketValue.setText("" + stats.getTotalPacket());
				percentageLossValue
						.setText(((stats.getTotalPacket() - stats.getTotalSent()) / stats.getTotalPacket()) * 100 + "");
				totalTimeTakenValue.setText(""+stats.getTotalTime());
				lostPacketCountValue.setText(""+(stats.getTotalPacket() - stats.getTotalSent()));
			}
		} catch (Exception exception) {
			System.out.println("Exception occured :" + exception);
		}
	}

	private TextField recHostValue;
	private Label sPortLabel;
	private Label sFileLabel;
	private TextField sPortNoValue;
	private Label rPortLabel;
	private TextField rPortValue;
	private Label recHostLabel;
	private TextField sFileLocValue;
	private Label rFileLabel;
	private Label pLLabel;
	private TextField pLValue;
	private TextField rFileLocValue;
	private Button startButton;
}

class ProtocolStats {

	private Long fileSize;
	private Long totalSent;
	private Long totalPacket;
	private Long totalTime;
	
	
	public ProtocolStats(Long fileSize, Long totalSent, Long totalPacket,
			Long totalTime) {
		super();
		this.fileSize = fileSize;
		this.totalSent = totalSent;
		this.totalPacket = totalPacket;
		this.totalTime = totalTime;
	}
	public Long getTotalTime() {
		return totalTime;
	}
	public void setTotalTime(Long totalTime) {
		this.totalTime = totalTime;
	}
	public Long getFileSize() {
		return fileSize;
	}
	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}
	public Long getTotalSent() {
		return totalSent;
	}
	public void setTotalSent(Long totalSent) {
		this.totalSent = totalSent;
	}
	public Long getTotalPacket() {
		return totalPacket;
	}
	public void setTotalPacket(Long totalPacket) {
		this.totalPacket = totalPacket;
	}
	
	
}
class SAWReciever implements Runnable {
	
	public SAWReciever(int port, String path) {
		this.toSendFilePath = path;
		this.recPort = port;
	}

	/**
	 * Receiver method to receive content from sender
	 */
	public void execute() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			serverSocket = new ServerSocket(recPort, 10);
			System.out.println("waiting for connection(Receiver)");
			socketFromReceiver = serverSocket.accept();
			seq = 0;
			System.out.println("Sender-Receiver Connection established");
			objOutStream = new ObjectOutputStream(socketFromReceiver.getOutputStream());
			objOutStream.flush();
			obInputStream = new ObjectInputStream(socketFromReceiver.getInputStream());
			objOutStream.writeObject("connected    .");
			do {
				try {
					packetContent = (String) obInputStream.readObject();
					if (Integer.valueOf(packetContent.substring(0, 1)) == seq) {
						packetContent = packetContent.substring(1);
						appendToFile(packetContent, toSendFilePath);
						content += packetContent.substring(1);
						objOutStream.writeObject(String.valueOf(seq));
						seq = (seq == 0) ? 1 : 0;
						System.out.println("Received data : " + packetContent);
					} else {
						System.out.println("Received data : " + packetContent + " and is duplicate data");
					}				
						
				} catch (Exception e) {
					System.out.println("execption occured while getting signals :" + e);
				}
			} while (!packetContent.equals(EOF));
			objOutStream.writeObject("connection ended    .");
		} catch (Exception e) {
		} finally {
			try {
				serverSocket.close();
				obInputStream.close();
				objOutStream.close();
			} catch (Exception e) {
			}
		}
	}

	public void run() {
		while (true) {
			execute();
		}
	}

	private void appendToFile(String msg, String path) throws IOException {
		FileWriter fw = null;
		try {
			fw = new FileWriter(path, true); 
			fw.write(msg);

		} finally {
			if (fw != null)
				fw.close();
		}
	}
	
	private ServerSocket serverSocket;
	private Socket socketFromReceiver = null;
	private String packetContent, acknow, content = "";
	int i = 0, seq = 0;
	private String toSendFilePath;
	private int recPort;
	private ObjectOutputStream objOutStream;
	private ObjectInputStream obInputStream;
	private final String EOF = "END_OF_THE_FILE";
}
class SAWSen {

	private ObjectOutputStream oos;
	private String rHost;
	private int sPort;
	private int n, i = 0, sequence = 0;
	private Socket soc;
	Long fileSize = 0l;
	Long totalPacket = 0L;
	Long totalSent = 0L;
	Long startTime = System.currentTimeMillis();

	public ProtocolStats senderExecuton() {
		try {

			File file = new File(path);
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			br = new BufferedReader(new FileReader(file));
			soc = new Socket(rHost, sPort);
			System.out.println("Waiting for Connection (Receiver)");

			sequence = 0;
			acklmt="1";
			oos = new ObjectOutputStream(soc.getOutputStream());
			oos.flush();
			ois = new ObjectInputStream(soc.getInputStream());
			str = (String) ois.readObject();
			String line = br.readLine();
			fileSize = file.length();
			while (line != null) {
				boolean isRepeat = true;
				while(isRepeat){
					if (canSend()) {
						totalPacket++;
						totalSent++;
						mstr = String.valueOf(sequence);
						oos.writeObject(mstr + line + "\n");
						oos.flush();
						System.out.println("sender is sending data : " + mstr);
						acklmt = (String) ois.readObject();
						System.out.println("sender is waiting for ackwlgmnt");
					} else {
						totalPacket++;
						System.out.println("packet loss occured based on precondition");
					}
					if (acklmt.equals(String.valueOf(sequence))) {
						sequence = (sequence == 0) ? 1 : 0;
						isRepeat = false;
						System.out.println("acknowledgment received from receiver: " + " packet recieved");
					} else {						
						System.out.println("time out happened resending data");
						
					}
				}
				line = br.readLine();
			}
			if (line == null) {
				System.out.println("Sender has finished sending data to specified receiver");
				oos.writeObject(EOM);
			}

		} catch (Exception e) {
		} finally {
			try {
				ois.close();
				oos.close();
				soc.close();
			} catch (Exception e) {
			}

		}
		Long endTime = System.currentTimeMillis();
		return new ProtocolStats(fileSize, totalSent, totalPacket,(endTime-startTime));
	}

	private boolean canSend() {
		//int rand = (int) (Math.random() % 100);
		//return rand < percentageLoss;
		float fNum = ((float)percentageLoss)/100;
		boolean sendValue = (Math.random() > fNum);		
		return sendValue;
	}

	public SAWSen(String host, int port, String file, int ploss) {
		this.rHost = host;
		this.sPort = port;
		this.path = file;
		this.percentageLoss = ploss;
	}

	private final String EOM = "END_OF_THE_FILE";
	private ObjectInputStream ois;
	private String packetData, acklmt, str, mstr;
	private int percentageLoss = 0;
	private String path;
}