package com.cyecize.domainrouter;

import com.cyecize.domainrouter.api.connection.ConnectionHandler;
import com.cyecize.domainrouter.constants.General;
import com.cyecize.ioc.MagicInjector;
import com.cyecize.ioc.annotations.Service;
import com.cyecize.ioc.annotations.StartUp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppStartUp {

    public static void main(String[] args) {
        MagicInjector.run(AppStartUp.class);
    }

    private final ConnectionHandler connectionHandler;

    @StartUp
    public void startUp() {
        final ExecutorService pool = Executors.newFixedThreadPool(10);
        final int port = getPort();

        new Thread(() -> {
            log.info("Try port {}.", port);
            try (final ServerSocket server = new ServerSocket(port)) {
                log.info("Start listening for connections!");

                while (true) {
                    final Socket client = server.accept();
                    pool.submit(() -> this.connectionHandler.process(client));
                }
            } catch (IOException e) {
                log.error("Error while initializing server socket.", e);
            }
        }).start();
    }

    private static int getPort() {
        if (System.getenv("port") != null) {
            return Integer.parseInt(System.getenv("port").trim());
        } else {
            return General.DEFAULT_PORT;
        }
    }
}
