

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

public class GoBackInterface extends Frame implements ActionListener {

	private TextField recPortNo;
	private Label sendFile;
	private TextField sendFileLoc;
	private Label recieveFile;
	private TextField recieveFileLoc;
	private Label hostName;
	private TextField hostValue;
	private Button start;
	private Label senderPort;
	private TextField senderPortNo;
	private Label recPort;
	private TextArea stats;
	private Label packetLoss;
	private TextField pLoss;

	// Constructor to setup GUI components and event handlers
	public GoBackInterface() {
		setLayout(new FlowLayout());

		hostName = new Label("Reciever Host Name");
		packetLoss = new Label("Packet loss");
		senderPort = new Label("Sender Port No");
		recPort = new Label("Reciever Port No");
		sendFile = new Label("Send File Path");
		recieveFile = new Label("Recieve File Path");

		hostValue = new TextField("localhost", 10);
		pLoss = new TextField("70", 3);
		senderPortNo = new TextField("8889", 4);
		recPortNo = new TextField("8889", 4);
		sendFileLoc = new TextField("sample.txt", 25);
		recieveFileLoc = new TextField("sample.txt", 25);
		hostValue.setEditable(true);
		pLoss.setEditable(true);
		hostValue.setEditable(true);
		recieveFileLoc.setEditable(true);
		Label statistics = new Label(
				"-----------------------------------Sender Statistic----------------------------------");
		add(statistics);
		stats = new TextArea(8, 100);
		stats.setEditable(false);
		start = new Button("Start");

		add(hostName);
		add(hostValue);
		add(packetLoss);
		add(pLoss);

		add(senderPort);
		add(senderPortNo);
		add(recPort);
		add(recPortNo);

		add(sendFile);
		add(sendFileLoc);
		add(recieveFile);
		add(recieveFileLoc);
		add(stats);
		add(start);

		start.addActionListener(this);
		setTitle("Go Back N Networking");
		setSize(500, 500);
		setVisible(true);
	}

	public static void main(String[] args) {
		GoBackInterface app = new GoBackInterface();
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		try {
			int senderPortInt = Integer.parseInt(senderPortNo.getText());
			int receiverPortInt = Integer.parseInt(recPortNo.getText());
			int packetLossValue = Integer.parseInt(pLoss.getText());
			GoBackSenderThread sender = new GoBackSenderThread(hostValue.getText(), senderPortInt,
					sendFileLoc.getText(), packetLossValue);
			GoBackReceiverThread receiver = new GoBackReceiverThread(receiverPortInt, recieveFileLoc.getText());
			Thread receiverThread = new Thread(receiver);
			receiverThread.start();
			Thread.sleep(1000);
			String stat = sender.executeSender();
			stats.setText(stat);
		} catch (Exception exception) {
			System.out.println("Error occured while starting sender/receiver");
		}
	}

	class GoBackTimerLog {

		int cMilliseconds;
		int timeTaken;
		int sMilliseconds;
		int tMilliseconds;

		/**
		 * 
		 * @param timeoutInMseconds
		 */
		GoBackTimerLog(int timeoutInMseconds) {
			// work out current time in seconds and
			Calendar cal = new GregorianCalendar();
			int sec = cal.get(Calendar.SECOND);
			int min = cal.get(Calendar.MINUTE);
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			int milliSec = cal.get(Calendar.MILLISECOND);
			sMilliseconds = milliSec + (sec * 1000) + (min * 60000) + (hour * 3600000);
			tMilliseconds = (timeoutInMseconds);
		}

		/**
		 * 
		 * @return
		 */
		int timeUsed() {
			Calendar cal = new GregorianCalendar();
			int hourElapsed = cal.get(Calendar.HOUR_OF_DAY);
			int secElapsed = cal.get(Calendar.SECOND);
			int minElapsed = cal.get(Calendar.MINUTE);
			int milliSecElapsed = cal.get(Calendar.MILLISECOND);
			cMilliseconds = milliSecElapsed + (secElapsed * 1000) + (minElapsed * 60000) + (hourElapsed * 3600000);
			timeTaken = cMilliseconds - sMilliseconds;
			return timeTaken;
		}

		boolean timeout() {
			timeUsed();
			if (timeTaken >= tMilliseconds) {
				return true;
			} else {
				return false;
			}
		}
	}

	class GoBackSenderThread {

		private int senderPortNumber;
		private String recHostAddress;
		private String localFilePath;
		private int packetLossPercentage;

		/**
		 * Execute sender
		 * 
		 * @throws IOException
		 */
		public String executeSender() throws IOException {
			String stats = "";
			File toSendFile = new File(localFilePath);
			InputStream fileStream = new FileInputStream(toSendFile);
			System.out.println("Sending file : " + localFilePath);
			DatagramSocket datagramSocket = new DatagramSocket();
			InetAddress address = InetAddress.getByName(recHostAddress);

			byte[] fileArray = new byte[(int) toSendFile.length()];
			fileStream.read(fileArray);
			Long startTime = System.currentTimeMillis();

			int sequenceNumber = 0;
			boolean islastMessage = false;
			int ackSequence = 0;
			int lastAckedSequence = 0;
			boolean lastAcknowledgedFlag = false;
			int retransmissionCounter = 0;
			int windowSize = 16;
			Vector<byte[]> sentMessageList = new Vector<byte[]>();

			int i = 0;
			while (i < fileArray.length) {
				sequenceNumber += 1;
				byte[] message = new byte[1024];
				message[0] = (byte) (sequenceNumber >> 8);
				message[1] = (byte) (sequenceNumber);
				if ((i + 1021) >= fileArray.length) {
					islastMessage = true;
					message[2] = (byte) (1);
				} else {
					islastMessage = false;
					message[2] = (byte) (0);
				}
				if (!islastMessage) {
					for (int j = 0; j != 1021; j++) {
						message[j + 3] = fileArray[i + j];
					}
				} else if (islastMessage) {
					for (int j = 0; j < (fileArray.length - i); j++) {
						message[j + 3] = fileArray[i + j];
					}
				}
				DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, senderPortNumber);
				sentMessageList.add(message);

				while (true) {
					if ((sequenceNumber - windowSize) > lastAckedSequence) {
						boolean ackPacketReceived = false;
						boolean correctAckRecieved = false;
						while (!correctAckRecieved) {
							byte[] ack = new byte[2];
							DatagramPacket datagramAckPacket = new DatagramPacket(ack, ack.length);

							try {
								datagramSocket.setSoTimeout(50);
								datagramSocket.receive(datagramAckPacket);
								ackSequence = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
								ackPacketReceived = true;
							} catch (SocketTimeoutException e) {
								ackPacketReceived = false;
							}
							if (ackPacketReceived) {
								if (ackSequence >= (lastAckedSequence + 1)) {
									lastAckedSequence = ackSequence;
								}
								correctAckRecieved = true;
								System.out.println("Sequence Number = " + ackSequence + " Ack recieved from receiver");
								break;
							} else {
								System.out.println("Resending: " + sequenceNumber + "Sequence Number to sender = ");
								for (int y = 0; y < (sequenceNumber - lastAckedSequence); y++) {
									byte[] resendMessage = new byte[1024];
									resendMessage = sentMessageList.get(y + lastAckedSequence);

									DatagramPacket resendPacket = new DatagramPacket(resendMessage,
											resendMessage.length, address, senderPortNumber);
									datagramSocket.send(resendPacket);
									retransmissionCounter += 1;
								}
							}
						}
					} else {
						break;
					}
				}
				if (checkToSend()) {
					datagramSocket.send(sendPacket);
				}else {
					retransmissionCounter += 1;
					datagramSocket.send(sendPacket);
				}
				System.out.println(
						"Sequence number = " + sequenceNumber + "is sent and is last message :" + islastMessage);
				while (true) {
					boolean isAckPacketReceived = false;
					byte[] ack = new byte[2];
					DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

					try {
						datagramSocket.setSoTimeout(10);
						datagramSocket.receive(ackpack);
						ackSequence = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
						isAckPacketReceived = true;
					} catch (SocketTimeoutException e) {
						isAckPacketReceived = false;
						break;
					}
					if (isAckPacketReceived) {
						if (ackSequence >= (lastAckedSequence + 1)) {
							lastAckedSequence = ackSequence;
							System.out.println("Ack recieved: " + ackSequence + " :Sequence number");
						}
					}
				}
				i = i + 1021;
			}
			while (true) {
					if (sequenceNumber > lastAckedSequence) {
						boolean ackPacketReceived = false;
						boolean correctAckRecieved = false;
						while (!correctAckRecieved) {
							byte[] ack = new byte[2];
							DatagramPacket datagramAckPacket = new DatagramPacket(ack, ack.length);

							try {
								datagramSocket.setSoTimeout(50);
								datagramSocket.receive(datagramAckPacket);
								ackSequence = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
								ackPacketReceived = true;
							} catch (SocketTimeoutException e) {
								ackPacketReceived = false;
							}
							if (ackPacketReceived) {
								if (ackSequence >= (lastAckedSequence + 1)) {
									lastAckedSequence = ackSequence;
								}
								correctAckRecieved = true;
								System.out.println("Sequence Number = " + ackSequence + " Ack recieved from receiver");
								break;
							} else {
								System.out.println("Resending: " + sequenceNumber + "Sequence Number to sender = ");
								for (int y = 0; y < (sequenceNumber - lastAckedSequence); y++) {
									byte[] resendMessage = new byte[1024];
									resendMessage = sentMessageList.get(y + lastAckedSequence);

									DatagramPacket resendPacket = new DatagramPacket(resendMessage,
											resendMessage.length, address, senderPortNumber);
									datagramSocket.send(resendPacket);
									retransmissionCounter += 1;
								}
							}
						}
					} else {
						break;
					}
				}
			while (!lastAcknowledgedFlag) {

				boolean ackRecievedCorrect = false;
				boolean ackPacketReceived = false;
				while (!ackRecievedCorrect) {
					byte[] ack = new byte[2];
					DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

					try {
						datagramSocket.setSoTimeout(50);
						datagramSocket.receive(ackpack);
						ackSequence = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
						ackPacketReceived = true;
					} catch (SocketTimeoutException e) {
						ackPacketReceived = false;
					}
					if (islastMessage) {
						lastAcknowledgedFlag = true;
						break;
					}
					if (ackPacketReceived) {
						System.out.println("Ack recieved: Sequence number = " + ackSequence);
						if (ackSequence >= (lastAckedSequence + 1)) {
							lastAckedSequence = ackSequence;
						}
						ackRecievedCorrect = true;
					} else { // Resend the packet
						for (int j = 0; j != (sequenceNumber - lastAckedSequence); j++) {
							byte[] resendMessage = new byte[1024];
							resendMessage = sentMessageList.get(j + lastAckedSequence);
							DatagramPacket resendPacket = new DatagramPacket(resendMessage, resendMessage.length,
									address, senderPortNumber);
							datagramSocket.send(resendPacket);
							System.out.println("Resending: Sequence Number = " + lastAckedSequence);
							retransmissionCounter += 1;
						}
					}
				}
			}
			Long endTime = System.currentTimeMillis();
			Long totalTime = (endTime-startTime);
			stats += "File : " + localFilePath + "\n";
			stats += "File size (Bytes):" + toSendFile.length() + "\n";
			stats += "Time taken to send file is (in ms): " + totalTime+ "\n";
			stats += "Number of packets lost in complete prcess : " + retransmissionCounter;
			datagramSocket.close();
			System.out.println("File" + localFilePath + " has been sent");
			System.out.println("Number of retransmissions: " + retransmissionCounter);
			return stats;
		}

		/**
		 * Go back sender constructor method
		 * 
		 * @param shost
		 * @param port
		 * @param sfile
		 * @param ploss
		 */
		public GoBackSenderThread(String shost, int port, String sfile, int ploss) {
			this.recHostAddress = shost;
			this.localFilePath = sfile;
			this.senderPortNumber = port;
			this.packetLossPercentage = ploss;
		}

		private boolean checkToSend() {
			//int rand = (int) (Math.random() % 100);
			//return rand < packetLossPercentage;
			float fNum = ((float)packetLossPercentage)/100;
		    boolean sendCheck = (Math.random() > fNum);		
		    return sendCheck;
		}
	}

	class GoBackReceiverThread implements Runnable {

		/**
		 * Constructor for receiver thread
		 * 
		 * @param file
		 * @param port
		 */

		public GoBackReceiverThread(int port, String file) {
			this.receivedFileName = file;
			this.rPort = port;
		}

		/**
		 * Receiver function waits until complete file is sent
		 * 
		 * @throws IOException
		 */
		public void goBackReceiver() throws IOException {
			InetAddress senderAddress;
			File file = new File(receivedFileName);
			DatagramSocket datasocket = new DatagramSocket(rPort);
			FileOutputStream outToFile = new FileOutputStream(file);

			int lastRecSeqNumber = 0;
			boolean isLMessage = false;
			int curSeqNumber = 0;

			while (!isLMessage) {
				byte[] mByteArray = new byte[1024];
				byte[] fileByteArr = new byte[1021];

				datasocket.setSoTimeout(0);
				DatagramPacket receivedDataPacket = new DatagramPacket(mByteArray, mByteArray.length);
				datasocket.receive(receivedDataPacket);
				mByteArray = receivedDataPacket.getData();
				senderAddress = receivedDataPacket.getAddress();
				rPort = receivedDataPacket.getPort();
				curSeqNumber = ((mByteArray[0] & 0xff) << 8) + (mByteArray[1] & 0xff);

				if ((mByteArray[2] & 0xff) == 1) {
					isLMessage = true;
				} else {
					isLMessage = false;
				}

				/*
				 * Check for last packet
				 */
				if (curSeqNumber == (lastRecSeqNumber + 1)) {
					lastRecSeqNumber = curSeqNumber;
					for (int i = 3; i < 1024; i++) {
						fileByteArr[i - 3] = mByteArray[i];
					}
					outToFile.write(fileByteArr);
					System.out.println(
							"Received: Sequence number = " + curSeqNumber + "from sender with, Flag = " + isLMessage);
					sendAcknowledgementToSender(lastRecSeqNumber, datasocket, senderAddress, rPort);
				} else {
					if (curSeqNumber < (lastRecSeqNumber + 1)) {
						sendAcknowledgementToSender(curSeqNumber, datasocket, senderAddress, rPort);
					} else {
						sendAcknowledgementToSender(lastRecSeqNumber, datasocket, senderAddress, rPort);
					}
				}
			}
			outToFile.close();
			datasocket.close();
			System.out.println(receivedFileName + " has been received");
		}

		public void sendAcknowledgementToSender(int lastSeqNum, DatagramSocket dataSocket, InetAddress address,
				int port) throws IOException {
			byte[] ackPac = new byte[2];
			ackPac[0] = (byte) (lastSeqNum >> 8);
			ackPac[1] = (byte) (lastSeqNum);
			DatagramPacket acknowledgementDatagramPacket = new DatagramPacket(ackPac, ackPac.length, address, port);
			dataSocket.send(acknowledgementDatagramPacket);
			System.out.println("Acknowledge has sent of sequence Number = " + lastSeqNum);
		}

		@Override
		public void run() {
			try {
				goBackReceiver();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private String receivedFileName;
		private int rPort;
	}
}
