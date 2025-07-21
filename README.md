# Multithreading Demo - Java Edition

## Overview
This project demonstrates multithreading and synchronization concepts using Java and JavaFX. It has been converted from the original .NET/WPF implementation [here](https://github.com/chittur/multithreading-demo) to showcase the same multithreading principles in a Java environment.

## Design
The application has a simple JavaFX-based GUI and supports sending and receiving messages through UDP.
- The application runs a JavaFX-based GUI.
- A worker thread is spawned to listen for messages on a UDP port.
- Another worker thread is spawned to summarize messages after a batch of messages has been transacted.
- CompletableFuture is used to send messages to a UDP port asynchronously.
- Synchronization primitives are used to make thread-safe access of shared resources.

## Concepts Demonstrated
This project demonstrates the following concepts:

### **Multithreading**
Multithreading allows increasing the responsiveness of an application and, if the application runs on a multiprocessor or multi-core system, increases its throughput.
- **Thread**: The application creates two threads, one to listen for messages on the UDP port, and another to generate a summary after a batch of messages has been transacted.
- **CompletableFuture**: The application uses CompletableFuture to send messages across the UDP port asynchronously. This ensures the UI remains responsive (as opposed to being blocked if the messages were sent on the UI thread).

### **Synchronization**
When multiple threads are accessing shared data, a program must provide for possible resource sharing/conflicts. This is called synchronization.
- **BlockingQueue**: The application uses a BlockingQueue to signal the summarizer thread from other threads.
- **synchronized blocks**: The application uses synchronized blocks to provide a thread with exclusive access to a shared resource for executing a particular code path.

### **GUI Programming**
A program can use FXML and JavaFX for a rich Graphical User Interface.
- **JavaFX/FXML**: The application demonstrates how to use a JavaFX-based GUI with FXML layout definition.
- **Platform.runLater()**: The application demonstrates scheduling callbacks on the UI thread from worker threads. Any UI element access/manipulation must happen only from the JavaFX Application Thread.

### **IPC (Inter-Process Communication)**
Inter-Process Communication mechanisms allow different processes to communicate and manage shared data.
- **UDP**: The application uses User Datagram Protocol (UDP) for IPC via Java's DatagramSocket API.

## Technology Stack
- **Java 17+**: Modern Java with latest language features
- **JavaFX 19**: Modern GUI framework for desktop applications
- **Maven**: Build automation and dependency management
- **JUnit 5**: Unit testing framework

## Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── multithreading/
│   │           └── demo/
│   │               ├── MultiThreadingDemoApp.java (JavaFX Application)
│   │               ├── MainController.java (FXML controller)
│   │               ├── UdpListener.java (listener thread)
│   │               ├── MessageSummarizer.java (summarizer thread)
│   │               └── UdpSender.java (message sender)
│   └── resources/
│       └── main-window.fxml (UI layout definition)
├── test/
│   └── java/
pom.xml
README.md
.gitignore
checkstyle.xml
```

## Environment Requirements
- **Java 17 or higher**: Required for JavaFX and modern Java features
- **Maven 3.6+**: For building and running the project
- **JavaFX Runtime**: Included via Maven dependencies

## Building and Running

### Prerequisites
Ensure you have Java 17+ and Maven installed on your system.

### Build the Project
```bash
mvn clean compile
```

### Run the Application
```bash
mvn javafx:run
```

### Run Tests
```bash
mvn test
```

### Check Code Style
```bash
mvn checkstyle:check
```

### Package the Application
```bash
mvn clean package
```

## Usage
1. **Start the Application**: Run the application using Maven as described above.
2. **Automatic Port Assignment**: The application automatically assigns a random port for listening (displayed in the "Receive Port" field).
3. **Send Messages**: Enter a port number and message in the "Send Port" and message fields, then click "Send".
4. **Receive Messages**: Messages received on the listening port will appear in the receive message field.
5. **Message Counting**: The total message count is updated automatically as messages are sent and received.
6. **Automatic Summarization**: After every 5 messages, the application generates a summary (currently finds the longest message).

## Comprehensive Test Suite

The project includes extensive unit and integration tests specifically focused on multithreading behavior:

### **Test Coverage**
- **UdpSenderTest**: Basic UDP message sending functionality
- **MessageSummarizerTest**: Multithreading behavior of the summarizer component
  - Summary generation with thread synchronization
  - Concurrent access to shared message collections
  - Multiple trigger handling
  - Empty message list scenarios
- **UdpListenerTest**: UDP listener threading and communication
  - Message reception and processing
  - Concurrent message handling
  - Summarizer trigger mechanisms
  - Thread lifecycle management
- **MultithreadingIntegrationTest**: End-to-end multithreading workflows
  - Complete workflow testing (UDP → Listener → Summarizer)
  - Stress testing with multiple concurrent senders
  - Thread safety validation and race condition detection

### **Multithreading Concepts Tested**
- **Thread Synchronization**: BlockingQueue, synchronized blocks, CountDownLatch
- **Concurrent Access**: Thread-safe operations on shared resources
- **Inter-thread Communication**: Message passing between listener and summarizer threads
- **Thread Lifecycle**: Proper thread startup, execution, and cleanup
- **Race Condition Prevention**: Atomic operations and proper synchronization
- **Stress Testing**: High-volume concurrent message processing

### **Running Tests**
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=MessageSummarizerTest

# Run tests with detailed output
mvn test -Dmaven.surefire.debug=true
```

All tests are designed to complete within reasonable timeouts and demonstrate the reliability of the multithreading implementation under various conditions.

## Key Java Concepts Demonstrated

### Thread Management
- **Background Threads**: UDP listener and message summarizer run as daemon threads
- **Thread Safety**: Synchronized access to shared message collection
- **Thread Communication**: BlockingQueue for inter-thread signaling

### JavaFX Integration
- **FXML**: Declarative UI definition separate from logic
- **Controller Pattern**: Clean separation of UI and business logic
- **Platform.runLater()**: Thread-safe UI updates from background threads

### Asynchronous Programming
- **CompletableFuture**: Non-blocking message sending
- **Callback Patterns**: Functional interfaces for event handling

### Network Programming
- **DatagramSocket**: UDP communication implementation
- **Exception Handling**: Robust error handling for network operations

## Comparison with Original .NET Version

| .NET Concept | Java Equivalent |
|--------------|----------------|
| WPF/XAML | JavaFX/FXML |
| Thread | Thread/Runnable |
| Task.Run() | CompletableFuture.supplyAsync() |
| AutoResetEvent | BlockingQueue |
| lock(this) | synchronized blocks |
| Dispatcher.InvokeAsync | Platform.runLater() |
| UdpClient | DatagramSocket |

## License
This project maintains the same license as the original .NET implementation.
