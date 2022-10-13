package com.cyecize.domainrouter.api.connection;

import com.cyecize.domainrouter.api.options.OptionsService;
import com.cyecize.domainrouter.api.options.RouteOption;
import com.cyecize.domainrouter.error.CannotParseRequestException;
import com.cyecize.domainrouter.util.HttpProtocolUtils;
import com.cyecize.domainrouter.util.PoolService;
import com.cyecize.ioc.annotations.PostConstruct;
import com.cyecize.ioc.annotations.Service;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cyecize.domainrouter.util.HttpProtocolUtils.transferHttpRequest;
import static com.cyecize.domainrouter.util.HttpProtocolUtils.transferHttpResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionHandlerImpl implements ConnectionHandler {

    private final OptionsService optionsService;

    private final PoolService poolService;

    /**
     * Mapping of host to the desired destination server.
     * <p>
     * Eg.
     * abc.com -> localhost:8080
     * www.abc.com -> localhost:8080
     * <p>
     * xyz.com -> localhost:5050
     * abc.xyz.com -> localhost:5060
     * <p>
     * yyy.com -> 192.168.0.1:80
     * 134.134.122.19 -> localhost:8090
     */
    private final Map<String, DestinationDto> domainsMap = new HashMap<>();

    @PostConstruct
    void init() {
        final List<RouteOption> options = this.optionsService.getOptions();

        for (RouteOption option : options) {
            final DestinationDto dest = new DestinationDto(option.getDestinationPort(), option.getDestinationHost());

            this.domainsMap.put(option.getHost(), dest);
            for (String subdomain : option.getSubdomains()) {
                this.domainsMap.put(subdomain + "." + option.getHost(), dest);
            }
        }
    }

    @Override
    public void process(Socket socket) {
        try {
            Thread.currentThread().setName("Client connection thread");
            final InputStream clientIn = socket.getInputStream();
            final OutputStream clientOut = socket.getOutputStream();

            final List<String> metadata;
            try {
                metadata = HttpProtocolUtils.parseMetadataLines(clientIn, false);
            } catch (CannotParseRequestException ex) {
                log.warn("Error while reading HTTP Request metadata. {}", ex.getMessage());
                socket.close();
                return;
            }

            final Map<String, String> headers = HttpProtocolUtils.getHeaders(metadata);
            final int contentLength = HttpProtocolUtils.getContentLength(clientIn, headers);
            final String host = HttpProtocolUtils.getHost(socket, headers);

            if (!this.domainsMap.containsKey(host)) {
                log.warn("No such host " + host);
                socket.close();
                return;
            }

            final DestinationDto server = this.domainsMap.get(host);
            Thread.currentThread().setName("Client connection thread " + server.getHost() + " " + server.getPort());
            final Socket serverConnection;
            try {
                serverConnection = new Socket(server.getHost(), server.getPort());
            } catch (IOException ex) {
                log.warn("Could not establish connection to server {}:{}, Host: {}. Message: {}",
                        server.getHost(), server.getPort(), headers.get("Host"), ex.getMessage()
                );
                socket.close();
                return;
            }

            this.asyncSocketConnection(
                    () -> {
                        Thread.currentThread().setName(String.format("Req transfer thread %s:%s",
                                server.getHost(), server.getPort())
                        );
                        transferHttpRequest(clientIn, metadata, contentLength, serverConnection.getOutputStream());
                    },
                    socket,
                    serverConnection,
                    false
            );

            this.asyncSocketConnection(
                    () -> {
                        Thread.currentThread().setName(String.format("Response transfer thread %s:%s",
                                server.getHost(), server.getPort())
                        );
                        transferHttpResponse(serverConnection.getInputStream(), clientOut);
                    },
                    socket,
                    serverConnection,
                    true
            );

        } catch (IOException ex) {
            log.error("Error while processing client request!", ex);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void asyncSocketConnection(RunnableThrowable runnable,
                                       Socket clientSocket,
                                       Socket destServerSocket,
                                       boolean closeOnFinish) {
        this.poolService.submit(() -> {
            try {
                runnable.run();
            } catch (IOException ignored) {
                this.closeConnections(clientSocket, destServerSocket);
                return;
            }
            if (closeOnFinish) {
                this.closeConnections(clientSocket, destServerSocket);
            }
        });
    }

    private void closeConnections(Socket clientSocket, Socket destServerSocket) {
        try {
            clientSocket.close();
        } catch (IOException e) {
            log.warn("Could not close client socket.", e);
        }

        try {
            destServerSocket.close();
        } catch (IOException e) {
            log.warn("Could not close dest server socket.", e);
        }
    }

    interface RunnableThrowable {
        void run() throws IOException;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class DestinationDto {
        private int port;
        private String host;
    }
}
