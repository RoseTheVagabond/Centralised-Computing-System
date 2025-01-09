import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.Enumeration;

public class CCSTestClient {
    private final String broadcastAddress;
    private final int serverPort;
    private final Random random;
    private Socket tcpSocket;
    private PrintWriter writer;
    private BufferedReader reader;

    public CCSTestClient(String broadcastAddress, int serverPort) {
        this.broadcastAddress = broadcastAddress;
        this.serverPort = serverPort;
        this.random = new Random();
    }

    // Find the broadcast address for the default network interface
    private static String findBroadcastAddress() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress broadcast = interfaceAddress.getBroadcast();
                if (broadcast != null) {
                    return broadcast.getHostAddress();
                }
            }
        }
        // Fallback to local broadcast if no specific broadcast address is found
        return "255.255.255.255";
    }

    // Discover the CCS server using UDP broadcast
    public InetAddress discoverServer() throws IOException {
        DatagramSocket socket = new DatagramSocket();
        socket.setBroadcast(true);

        // Prepare the discovery message
        byte[] discoveryMessage = "CCS DISCOVER".getBytes();
        InetAddress broadcastAddr = InetAddress.getByName(broadcastAddress);
        DatagramPacket discoveryPacket = new DatagramPacket(
                discoveryMessage,
                discoveryMessage.length,
                broadcastAddr,
                serverPort
        );

        // Send discovery packet and wait for response
        byte[] buffer = new byte[1024];
        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

        int maxAttempts = 3;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                System.out.println("Sending discovery packet (attempt " + (attempt + 1) + ")...");
                socket.send(discoveryPacket);

                // Set timeout for response
                socket.setSoTimeout(2000);
                socket.receive(responsePacket);

                String response = new String(
                        responsePacket.getData(),
                        0,
                        responsePacket.getLength()
                );

                // Print the received message
                System.out.println("Received message from server: " + response);

                if (response.equals("CCS FOUND")) {
                    System.out.println("Server found at: " + responsePacket.getAddress());
                    socket.close();
                    return responsePacket.getAddress();
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout on attempt " + (attempt + 1));
            }
        }

        socket.close();
        throw new IOException("Failed to discover server after " + maxAttempts + " attempts");
    }

    // Connect to the server using TCP
    public void connectToServer(InetAddress serverAddress) throws IOException {
        tcpSocket = new Socket(serverAddress, serverPort);
        writer = new PrintWriter(tcpSocket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
        System.out.println("Connected to server via TCP");
    }

    // Send a request and get response
    public String sendRequest(String operation, int arg1, int arg2) throws IOException {
        String request = String.format("%s %d %d", operation, arg1, arg2);
        writer.println(request);
        String response = reader.readLine();
        System.out.printf("Sent: %s, Received: %s%n", request, response);
        return response;
    }

    // Run automated tests
    public void runTests() throws IOException, InterruptedException {
        String[] operations = {"ADD", "SUB", "MUL", "DIV"};

        // Test each operation with valid inputs
        for (String op : operations) {
            int arg1 = random.nextInt(100);
            int arg2 = op.equals("DIV") ? random.nextInt(10) + 1 : random.nextInt(100);
            sendRequest(op, arg1, arg2);
            TimeUnit.MILLISECONDS.sleep(random.nextInt(1000) + 500);
        }

        // Test error cases
        System.out.println("\nTesting error cases:");

        // Division by zero
        sendRequest("DIV", 10, 0);

        // Invalid operation
        sendRequest("INVALID", 10, 20);

        // Invalid number format (handled by server)
        writer.println("ADD 10 abc");
        System.out.println("Sent: ADD 10 abc, Received: " + reader.readLine());
    }

    // Close connection
    public void close() throws IOException {
        if (writer != null) writer.close();
        if (reader != null) reader.close();
        if (tcpSocket != null) tcpSocket.close();
    }

    public void startInteractiveMode() throws IOException {
        System.out.println("\nEntering interactive mode.");
        System.out.println("Type your commands in format: OPERATION ARG1 ARG2");
        System.out.println("Example: ADD 5 6");
        System.out.println("Type 'exit' to quit");
        System.out.println("----------------------------------------");

        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        String input;

        while ((input = consoleReader.readLine()) != null) {
            if (input.equalsIgnoreCase("exit")) {
                break;
            }

            if (!input.trim().isEmpty()) {
                writer.println(input);  // Send to server
                String response = reader.readLine();  // Get server's response
                System.out.println("Server response: " + response);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java CCSTestClient <port>");
            System.err.println("Example: java CCSTestClient 8080");
            System.exit(1);
        }

        int port;
        String broadcastAddress;

        try {
            port = Integer.parseInt(args[0]);
            broadcastAddress = findBroadcastAddress();
            System.out.println("Using broadcast address: " + broadcastAddress);
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid port number");
            System.exit(1);
            return;
        } catch (SocketException e) {
            System.err.println("Error: Unable to find broadcast address: " + e.getMessage());
            System.exit(1);
            return;
        }

        CCSTestClient client = new CCSTestClient(broadcastAddress, port);

        try {
            // Discover server
            InetAddress serverAddress = client.discoverServer();

            // Connect to server
            client.connectToServer(serverAddress);

            // Start interactive mode instead of automated tests
            client.startInteractiveMode();

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}
