package com.cyecize.domainrouter;

import com.cyecize.domainrouter.api.connection.ConnectionHandler;
import com.cyecize.domainrouter.constants.General;
import com.cyecize.domainrouter.util.PoolService;
import com.cyecize.ioc.MagicInjector;
import com.cyecize.ioc.annotations.Service;
import com.cyecize.ioc.annotations.StartUp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppStartUp {

    public static void main(String[] args) {
        MagicInjector.run(AppStartUp.class);
    }

    private final ConnectionHandler connectionHandler;

    private final PoolService poolService;

    @StartUp
    public void startUp() {
        final int port = getPort();

        new Thread(() -> {
            log.info("Try port {}.", port);
            try (final ServerSocket server = new ServerSocket(port)) {
                log.info("Start listening for connections!");

                while (true) {
                    final Socket client = server.accept();
                    this.poolService.submit(() -> this.connectionHandler.process(client));
                }
            } catch (IOException e) {
                log.error("Error while initializing server socket.", e);
            }
        }).start();
    }

    private static int getPort() {
        if (System.getenv().containsKey(General.ENV_VAR_PORT_NAME)) {
            return Integer.parseInt(System.getenv(General.ENV_VAR_PORT_NAME).trim());
        } else {
            return General.DEFAULT_PORT;
        }
    }
}
