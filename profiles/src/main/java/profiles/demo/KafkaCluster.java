package profiles.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class KafkaCluster {

    // Commands

    private static final String COMMAND_ENTER_KAFKA_DIR = "cd kafka_2.12-2.4.0";
    private static final String COMMAND_START_ZOOKEEPER = "bin/zookeeper-server-start.sh config/zookeeper.properties";
    private static final String COMMAND_START_KAFKA = "bin/kafka-server-start.sh config/server.properties";

    public static void main(String[] args) {

        ProcessBuilder processBuilder = new ProcessBuilder();
        // linux only
        processBuilder.command(
                "bash", "-c",
                String.format(
                        "%s && %s && %s",
                        COMMAND_ENTER_KAFKA_DIR,
                        COMMAND_START_ZOOKEEPER,
                        COMMAND_START_KAFKA
                )
        );

        try {

            Process process = processBuilder.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}