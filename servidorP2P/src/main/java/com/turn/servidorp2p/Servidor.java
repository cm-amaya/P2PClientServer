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
import java.net.InetAddress;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;

/**
 *
 * @author cristian
 */
public class Servidor {

    /**
     * Directorio de salida por default.
     */
    private static final String DEFAULT_OUTPUT_DIRECTORY = "/tmp";

    private static final Logger logger
            = LoggerFactory.getLogger(Servidor.class);

    public static void main(String[] args) throws Exception {

        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d [%-25t] %-5p: %m%n")));

        InetAddress address = InetAddress.getByName(args[0]);

        System.out.println("Direccion: " + address.toString());

        File torrent = new File(args[1]);

        System.out.println("Torrent: " + torrent.getAbsolutePath());

        if (!torrent.isFile()) {
            System.out.println("No existe el archivo torrent");
            return;
        } else if (!torrent.getName().contains(".torrent")) {
            System.out.println("Ingrese torrent valido");
            return;
        }

        File outputDirFile = new File(args[2]);

        if (!outputDirFile.exists()) {
            outputDirFile = new File(DEFAULT_OUTPUT_DIRECTORY);
            if (!outputDirFile.exists()) {
                outputDirFile.mkdir();
            }
        }

        try {

            Client c = new Client(address, SharedTorrent.fromFile(torrent, outputDirFile));
            c.setMaxUploadRate(0.0);
            c.setMaxDownloadRate(0.0);
            c.share(-1);

        } catch (Exception e) {
            logger.error("Fatal error: {}", e.getMessage(), e);
            System.exit(2);
        }
    }
}
