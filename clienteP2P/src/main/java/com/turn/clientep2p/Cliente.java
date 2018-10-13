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
package com.turn.clientep2p;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

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
public class Cliente {

    private static final Logger logger
            = LoggerFactory.getLogger(Cliente.class);

    /**
     * Default data output directory.
     */
    private static final String DEFAULT_OUTPUT_DIRECTORY = "/tmp";

    /**
     * Bytes en un MegaByte
     */
    public static final double MB = Math.pow(1024, 2);

    private static String start_date;
    private static String fileName;
    private static String outputDir;
    static boolean finish = false;
    static boolean first = true;

    private static void startLog() {
        String logName = "log-" + fileName + "_" + start_date + ".txt";
        File logPath = new File(outputDir, logName);
        String date = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date());
        try {
            final BufferedWriter writer = new BufferedWriter(new FileWriter(logPath.getPath()));
            writer.write("Archivo solicitado: " + fileName);
            writer.newLine();
            writer.write("Comienzo proceso: " + date);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void appendToLog(String line) {

        try {
            String logName = "log-" + fileName + "_" + start_date + ".txt";
            File logPath = new File(outputDir, logName);
            BufferedWriter bw = new BufferedWriter(new FileWriter(logPath.getPath(), true));
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

            fileName = torrent.getName();
            if (fileName.indexOf(".") > 0) {
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
            }
            start_date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());

            outputDir = outputDirFile.getAbsolutePath();

            startLog();

            final long startTime = System.currentTimeMillis();

            c.addObserver(new Observer() {
                public void update(Observable observable, Object data) {
                    Client client = (Client) observable;
                    if (client.getTorrent().isInitialized()) {
                        if (first) {
                            appendToLog("Size: " + roundTwo(client.getTorrent().getSize() / MB) + " MB");
                            appendToLog("Chunks: " + client.getTorrent().getPieceCount());
                            first = false;
                        }
                        int current_pieces = client.getTorrent().getCompletedPieces().cardinality();
                        int asking_pieces = client.getTorrent().getRequestedPieces().cardinality();
                        double bytes_complete = client.getTorrent().getDownloaded() / MB;
                        double bytes_uploaded = client.getTorrent().getUploaded() / MB;
                        int peers = client.getPeers().size();
                        long currentTime = System.currentTimeMillis();
                        long elapsedTime = currentTime - startTime;

                        if (!finish) {
                            appendToLog("Peers: " + peers + " - Chunks completados: " + current_pieces + " - Chunks pedidos: " + asking_pieces + " - MB descargados: " + roundTwo(bytes_complete) + " - MB subidos: " + roundTwo(bytes_uploaded) + " - Tiempo transmision: " + (elapsedTime / 1000) + " segundos");
                        }

                        if (client.getTorrent().isComplete() && !finish) {
                            appendToLog("Termino proceso - Chunks completados: " + current_pieces + " - MB bajados: " + roundTwo(bytes_complete) + " - MB subidos: " + roundTwo(bytes_uploaded) + " - Tiempo transmision: " + (elapsedTime / 1000) + " segundos");
                            finish = true;
                        }
                    }
                }
            });

            c.share(120);

        } catch (Exception e) {
            logger.error("Fatal error: {}", e.getMessage(), e);
            System.exit(2);
        }
    }

}
