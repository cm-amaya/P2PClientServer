/*
 * Copyright 2018 cristian.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.turn.servidorp2p;

import java.io.File;
import java.io.FilenameFilter;
import java.net.InetSocketAddress;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;


/**
 * Clase Tracker
 * @author cristian
 */
public class TrackerP2P {
    private static final Logger logger =
			LoggerFactory.getLogger(Tracker.class);
	

	public static void main(String[] args) {
		
		BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d [%-25t] %-5p: %m%n")));
		
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
			    return name.endsWith(".torrent");
			 }
		};
		
		try {
			Tracker tracker = new Tracker(new InetSocketAddress(8082));
			
			for (File f : new File(".").listFiles(filter)) {
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
