package com.uniandes.labredes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;

@SuppressWarnings("deprecation")
public class Cliente {

	private static final Logger logger =
			LoggerFactory.getLogger(Cliente.class);

	/**
	 * Default data output directory.
	 */
	private static final String DEFAULT_OUTPUT_DIRECTORY = "/tmp";

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		try {

			//InetAddress address = InetAddress.getLocalHost();
			InetAddress address = new InetSocketAddress(0).getAddress();
			
			File torrent = new File(args[0]);

			Client c = new Client(address,SharedTorrent.fromFile(torrent,new File(args[1])));

			String fileName =torrent.getName();
			if (fileName.indexOf(".") > 0) {
				fileName = fileName.substring(0, fileName.lastIndexOf("."));
			}
			String start_date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
			String logName = "log-"+fileName+"_"+start_date+".txt";
			final BufferedWriter writer = new BufferedWriter(new FileWriter(logName));

			writer.write("Archivo solicitado: "+fileName);
			writer.newLine();
			writer.write("Comienzo proceso: "+start_date);
			writer.newLine();

			final long startTime = System.currentTimeMillis();
			final int chunk = 1;

			c.addObserver(new Observer() {
				public void update(Observable observable, Object data) {
					Client client = (Client) observable;
					float progress = client.getTorrent().getCompletion();
					System.out.println(progress);
					if(client.getTorrent().isInitialized()) {
						int current_pieces = client.getTorrent().getCompletedPieces().length();
						int peers = client.getPeers().size();	
						long currentTime = System.currentTimeMillis();
						long elapsedTime = currentTime - startTime;
						try {
							writer.write("Peers: "+peers+" - Chunks completados: "+current_pieces+" - Tiempo transmision: "+(elapsedTime/1000)+" segundos");
							writer.newLine();
						} catch (IOException e) {
							e.printStackTrace();
						}

						if(client.getTorrent().isComplete()) {
							try {
								writer.write("Termino proceso - Chunks completados: "+current_pieces+" - Tiempo transmision: "+(elapsedTime/1000)+" segundos");
								writer.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			});


			c.share(3600);

		} catch (Exception e) {
			logger.error("Fatal error: {}", e.getMessage(), e);
			System.exit(2);
		}
	}

}
