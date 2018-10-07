package com.uniandes.labredes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;

@SuppressWarnings("deprecation")
public class Cliente {
	
	/**
	 * Bytes en un MegaByte
	 */
	public final static double MB = Math.pow(1024,2);

	private static final Logger logger =
			LoggerFactory.getLogger(Cliente.class);

	private static String start_date;
	private static String fileName;
	private static String outputDir;
	static boolean finish = false;
	
	private static Inet4Address getIPv4Address(String iface)
			throws SocketException, UnsupportedAddressTypeException,
			UnknownHostException {
			if (iface != null) {
				Enumeration<InetAddress> addresses =
					NetworkInterface.getByName(iface).getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					if (addr instanceof Inet4Address) {
						return (Inet4Address)addr;
					}
				}
			}

			InetAddress localhost = InetAddress.getLocalHost();
			if (localhost instanceof Inet4Address) {
				return (Inet4Address)localhost;
			}

			throw new UnsupportedAddressTypeException();
		}

	private static void startLog() {
		String logName = "log-"+fileName+"_"+start_date+".txt";
		File logPath = new File(outputDir,logName);
		try {
			final BufferedWriter writer = new BufferedWriter(new FileWriter(logPath.getPath()));
			writer.write("Archivo solicitado: "+fileName);
			writer.newLine();
			writer.write("Comienzo proceso: "+start_date);
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void appendToLog(String line) {

		try {
			String logName = "log-"+fileName+"_"+start_date+".txt";
			File logPath = new File(outputDir,logName);
			BufferedWriter bw = new BufferedWriter( new FileWriter(logPath.getPath(), true));
			bw.write(line + System.getProperty("line.separator"));
			bw.close();
			logger.info(line);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String roundTwo(double number) {
		 return Double.toString((double) Math.round(number * 100) / 100);
	}

	/**
	 * Default data output directory.
	 */
	private static final String DEFAULT_OUTPUT_DIRECTORY = "/tmp";


	public static void main(String[] args) throws UnknownHostException, UnsupportedAddressTypeException, SocketException {

		BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d [%-25t] %-5p: %m%n")));

		Inet4Address address = getIPv4Address(args[0]);
		
		System.out.println("Direccion: "+address.toString());

		File torrent = new File(args[1]);
		
		System.out.println("Torrent: "+torrent.getAbsolutePath());

		if(!torrent.isFile()) {
			System.out.println("No existe el archivo torrent");
			return;
		} else if(!torrent.getName().contains(".torrent")) {
			System.out.println("Ingrese torrent valido");
			return;
		}

		File outputDirFile = new File(args[2]);

		if(!outputDirFile.exists()) {
			outputDirFile = new File(DEFAULT_OUTPUT_DIRECTORY);
			if(!outputDirFile.exists()) {
				outputDirFile.mkdir();
			}
		}
		try {

			Client c = new Client(address,SharedTorrent.fromFile(torrent,outputDirFile));
			
			c.setMaxUploadRate(0.0);
			c.setMaxDownloadRate(0.0);

			fileName =torrent.getName();
			if (fileName.indexOf(".") > 0) {
				fileName = fileName.substring(0, fileName.lastIndexOf("."));
			}
			start_date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
			
			outputDir= outputDirFile.getAbsolutePath();

			startLog();

			final long startTime = System.currentTimeMillis();

			c.addObserver(new Observer() {
				public void update(Observable observable, Object data) {
					Client client = (Client) observable;
					if(client.getTorrent().isInitialized()) {
						int current_pieces = client.getTorrent().getCompletedPieces().cardinality();
						int asking_pieces = client.getTorrent().getRequestedPieces().cardinality();
						double bytes_complete =  client.getTorrent().getDownloaded()/MB;
						double bytes_left = client.getTorrent().getLeft()/MB; 
						int peers = client.getPeers().size();	
						long currentTime = System.currentTimeMillis();
						long elapsedTime = currentTime - startTime;
						appendToLog("Peers: "+peers+" - Chunks completados: "+current_pieces+" - MB descargados: "+roundTwo(bytes_complete)+" - Tiempo transmision: "+(elapsedTime/1000)+" segundos");

						if(client.getTorrent().isComplete() && !finish) {
							appendToLog("Termino proceso - Chunks completados: "+current_pieces+" - Tiempo transmision: "+(elapsedTime/1000)+" segundos");
							finish = true;
						}
					}
				}
			});


			c.share(1200);

		} catch (Exception e) {
			logger.error("Fatal error: {}", e.getMessage(), e);
			System.exit(2);
		}
	}

}
