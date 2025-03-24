import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        // Uncomment this block to pass the first stage
        try (ServerSocket serverSocket = new ServerSocket(4221)) {

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("accepted new connection");
                executorService.submit(() -> handleRequest(socket, args));
            }

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleRequest(Socket session, String[] args) {

        String OK_RESPONSE = "HTTP/1.1 200 OK\r\n\r\n";
        String CREATED_RESPONSE = "HTTP/1.1 201 Created\r\n\r\n";
        String NOT_FOUND_RESPONSE = "HTTP/1.1 404 Not Found\r\n\r\n";

        try {
            InputStream inputStream = session.getInputStream();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String[] requestLine = bufferedReader.readLine().split(" ");
            String requestTarget = requestLine[1];
            if (requestTarget.equals("/")) {
                session.getOutputStream().write(OK_RESPONSE.getBytes());
            } else if (requestTarget.startsWith("/echo/")) {
                String body = requestTarget.substring("/echo/".length());
                String line;
                while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
                    if (line.startsWith("Accept-Encoding: ")) {
                        break;
                    }
                }
                String contentEncoding = Optional.ofNullable(line).filter(it -> it.contains("gzip")).map(l -> "gzip").orElse("");
                if (contentEncoding.equals("gzip")) {
                    String response = String.format(
                            "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Encoding: gzip\r\nContent-Length: %d\r\n\r\n%s",
                            body.length(),
                            body);
                    session.getOutputStream().write(response.getBytes());
                } else {
                    String response = String.format(
                            "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
                            body.length(),
                            body);
                    session.getOutputStream().write(response.getBytes());
                }
            } else if (requestTarget.startsWith("/user-agent")) {
                String userAgent = "";
                String line;

                while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
                    if (line.startsWith("User-Agent: ")) {
                        userAgent = line.substring("User-Agent: " .length());
                        break;
                    }
                }

                String response = String.format(
                        "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
                        userAgent.length(),
                        userAgent);
                session.getOutputStream().write(response.getBytes());
            } else if (requestTarget.startsWith("/files")) {
                String methodType = requestLine[0];
                String filename = requestTarget.substring("/files/".length());
                File file = new File(args[1] + "/" + filename);
                if (methodType.equals("GET")) {
                    if (!file.exists()) {
                        session.getOutputStream().write(NOT_FOUND_RESPONSE.getBytes());
                    } else {
                        String response = getString(file);
                        session.getOutputStream().write(response.getBytes());
                    }
                } else {
                    String line;
                    int size = 0;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.startsWith("Content-Length: ")) {
                            size = Integer.parseInt(line.substring("Content-Length: ".length()));
                        }
                        if (line.isEmpty()) {
                            break;
                        }
                    }

                    char[] buffer = new char[size];
                    bufferedReader.read(buffer, 0, buffer.length);

                    try(BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
                        bufferedWriter.write(buffer);
                    }

                    session.getOutputStream().write(CREATED_RESPONSE.getBytes());
                }

            }
            else {
                session.getOutputStream().write(NOT_FOUND_RESPONSE.getBytes());
            }

            bufferedReader.close();
            session.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getString(File file) throws IOException {
        BufferedReader fileReader = new BufferedReader(new FileReader(file));
        String line;
        StringBuilder lines = new StringBuilder();
        while ((line = fileReader.readLine()) != null) {
            lines.append(line);
        }
        return String.format(
                "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: %d\r\n\r\n%s",
                file.length(),
                lines
        );
    }
}
