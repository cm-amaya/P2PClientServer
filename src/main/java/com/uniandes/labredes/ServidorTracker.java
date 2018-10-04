package com.uniandes.labredes;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;


public class ServidorTracker {
	
	private static final Logger logger =
			LoggerFactory.getLogger(ServidorTracker.class);
	

	public static void main(String[] args) {
		
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
			    return name.endsWith(".torrent");
			 }
		};
		
		try {
			Tracker tracker = new Tracker(new InetSocketAddress(6969));
			
			for (File f : new File("/path/to/torrent/files").listFiles(filter)) {
				  tracker.announce(TrackedTorrent.load(f));
			}
			
			logger.info("Starting tracker with {} announced torrents...",tracker.getTrackedTorrents().size());
			tracker.start();
			
		} catch (Exception e) {
			logger.error("{}", e.getMessage(), e);
			System.exit(2);
		}

	}
}
