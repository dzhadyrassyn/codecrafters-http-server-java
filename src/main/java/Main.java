import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        String OK_RESPONSE = "HTTP/1.1 200 OK\r\n\r\n";
        String NOT_FOUND_RESPONSE = "HTTP/1.1 404 Not Found\r\n\r\n";

        // Uncomment this block to pass the first stage
        try (ServerSocket serverSocket = new ServerSocket(4221)) {

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);

            Socket session = serverSocket.accept();// Wait for connection from client.
            System.out.println("accepted new connection");

            InputStream inputStream = session.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String[] requestLine = bufferedReader.readLine().split(" ");
            String requestTarget = requestLine[1];
            if (requestTarget.equals("/")) {
                session.getOutputStream().write(OK_RESPONSE.getBytes());
            } else if (requestTarget.startsWith("/echo/")) {
                String body = requestTarget.substring("/echo/".length());
                String response = String.format(
                        "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
                        body.length(),
                        body);
                session.getOutputStream().write(response.getBytes());
            } else {
                session.getOutputStream().write(NOT_FOUND_RESPONSE.getBytes());
            }

            bufferedReader.close();
            session.close();

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
