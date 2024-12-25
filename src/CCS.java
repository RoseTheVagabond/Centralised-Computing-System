import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class CCS {
    private final int port;
    private final ExecutorService executorService;
    private final ConcurrentHashMap<Socket, ClientHandler> activeClients;

    // Statistics counters using atomic variables for thread safety
    private final AtomicInteger totalClients;
    private final AtomicInteger totalRequests;
    private final AtomicInteger addOperations;
    private final AtomicInteger subOperations;
    private final AtomicInteger mulOperations;
    private final AtomicInteger divOperations;
    private final AtomicInteger errorOperations;
    private final AtomicLong totalSum;

    // Periodic statistics
    private final AtomicInteger periodicClients;
    private final AtomicInteger periodicRequests;
    private final AtomicInteger periodicAddOps;
    private final AtomicInteger periodicSubOps;
    private final AtomicInteger periodicMulOps;
    private final AtomicInteger periodicDivOps;
    private final AtomicInteger periodicErrorOps;
    private final AtomicLong periodicSum;

    public CCS(int port) {
        this.port = port;
        this.executorService = Executors.newCachedThreadPool();
        this.activeClients = new ConcurrentHashMap<>();

        // Initialize statistics counters
        this.totalClients = new AtomicInteger(0);
        this.totalRequests = new AtomicInteger(0);
        this.addOperations = new AtomicInteger(0);
        this.subOperations = new AtomicInteger(0);
        this.mulOperations = new AtomicInteger(0);
        this.divOperations = new AtomicInteger(0);
        this.errorOperations = new AtomicInteger(0);
        this.totalSum = new AtomicLong(0);

        // Initialize periodic statistics
        this.periodicClients = new AtomicInteger(0);
        this.periodicRequests = new AtomicInteger(0);
        this.periodicAddOps = new AtomicInteger(0);
        this.periodicSubOps = new AtomicInteger(0);
        this.periodicMulOps = new AtomicInteger(0);
        this.periodicDivOps = new AtomicInteger(0);
        this.periodicErrorOps = new AtomicInteger(0);
        this.periodicSum = new AtomicLong(0);
    }

    public void start() {
        // Start UDP discovery service
        executorService.submit(this::runDiscoveryService);

        // Start TCP client service
        executorService.submit(this::runClientService);

        // Start statistics reporting
        executorService.submit(this::runStatisticsReporting);
    }

    private void runDiscoveryService() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[1024];
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.startsWith("CCS DISCOVER")) {
                    // Send response
                    byte[] response = "CCS FOUND".getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                            response,
                            response.length,
                            packet.getAddress(),
                            packet.getPort()
                    );
                    socket.send(responsePacket);
                }
            }
        } catch (IOException e) {
            System.err.println("Discovery service error: " + e.getMessage());
        }
    }

    private void runClientService() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                totalClients.incrementAndGet();
                periodicClients.incrementAndGet();

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                activeClients.put(clientSocket, clientHandler);
                executorService.submit(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("Client service error: " + e.getMessage());
        }
    }

    private void runStatisticsReporting() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(10000); // 10 seconds
                printStatistics();
                resetPeriodicStatistics();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void printStatistics() {
        System.out.println("\n=== Statistics Report ===");
        System.out.println("Total since start:");
        System.out.println("- Connected clients: " + totalClients.get());
        System.out.println("- Total requests: " + totalRequests.get());
        System.out.println("- ADD operations: " + addOperations.get());
        System.out.println("- SUB operations: " + subOperations.get());
        System.out.println("- MUL operations: " + mulOperations.get());
        System.out.println("- DIV operations: " + divOperations.get());
        System.out.println("- Error operations: " + errorOperations.get());
        System.out.println("- Sum of computed values: " + totalSum.get());

        System.out.println("\nLast 10 seconds:");
        System.out.println("- New clients: " + periodicClients.get());
        System.out.println("- Requests: " + periodicRequests.get());
        System.out.println("- ADD operations: " + periodicAddOps.get());
        System.out.println("- SUB operations: " + periodicSubOps.get());
        System.out.println("- MUL operations: " + periodicMulOps.get());
        System.out.println("- DIV operations: " + periodicDivOps.get());
        System.out.println("- Error operations: " + periodicErrorOps.get());
        System.out.println("- Sum of computed values: " + periodicSum.get());
    }

    private void resetPeriodicStatistics() {
        periodicClients.set(0);
        periodicRequests.set(0);
        periodicAddOps.set(0);
        periodicSubOps.set(0);
        periodicMulOps.set(0);
        periodicDivOps.set(0);
        periodicErrorOps.set(0);
        periodicSum.set(0);
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final BufferedReader reader;
        private final PrintWriter writer;

        public ClientHandler(Socket socket) throws IOException {
            this.clientSocket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    totalRequests.incrementAndGet();
                    periodicRequests.incrementAndGet();

                    String result = processRequest(line);
                    writer.println(result);
                    System.out.println("Request: " + line + " | Result: " + result);
                }
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private String processRequest(String request) {
            String[] parts = request.split(" ");
            if (parts.length != 3) {
                errorOperations.incrementAndGet();
                periodicErrorOps.incrementAndGet();
                return "ERROR";
            }

            try {
                String operation = parts[0];
                int arg1 = Integer.parseInt(parts[1]);
                int arg2 = Integer.parseInt(parts[2]);

                int result;
                switch (operation) {
                    case "ADD":
                        addOperations.incrementAndGet();
                        periodicAddOps.incrementAndGet();
                        result = arg1 + arg2;
                        break;
                    case "SUB":
                        subOperations.incrementAndGet();
                        periodicSubOps.incrementAndGet();
                        result = arg1 - arg2;
                        break;
                    case "MUL":
                        mulOperations.incrementAndGet();
                        periodicMulOps.incrementAndGet();
                        result = arg1 * arg2;
                        break;
                    case "DIV":
                        if (arg2 == 0) {
                            errorOperations.incrementAndGet();
                            periodicErrorOps.incrementAndGet();
                            return "ERROR";
                        }
                        divOperations.incrementAndGet();
                        periodicDivOps.incrementAndGet();
                        result = arg1 / arg2;
                        break;
                    default:
                        errorOperations.incrementAndGet();
                        periodicErrorOps.incrementAndGet();
                        return "ERROR";
                }

                totalSum.addAndGet(result);
                periodicSum.addAndGet(result);
                return String.valueOf(result);

            } catch (NumberFormatException e) {
                errorOperations.incrementAndGet();
                periodicErrorOps.incrementAndGet();
                return "ERROR";
            }
        }

        private void cleanup() {
            try {
                reader.close();
                writer.close();
                clientSocket.close();
                activeClients.remove(clientSocket);
            } catch (IOException e) {
                System.err.println("Error during cleanup: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar CCS.jar <port>");
            System.exit(1);
        }

        try {
            int port = Integer.parseInt(args[0]);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("Port number must be between 1 and 65535");
            }

            CCS server = new CCS(port);
            server.start();

        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid port number - " + e.getMessage());
            System.exit(1);
        }
    }
}