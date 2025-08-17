# Collaborative Text Editor with Real-Time Synchronization 

A real-time collaborative text editor built in Java with JavaFX for GUI and TCP sockets for networking. Multiple users can edit the same document simultaneously over a **Local Area Network (LAN)** with integrated chat, session management, and real-time synchronization.

# Features

- **Multi-Session Support:** Multiple collaborative sessions with unique session IDs.
- **Real-Time Editing:** Simultaneous text editing with operational transformation to handle concurrent changes.
- **Chat System:** Real-time chat with multiple themes for each session.
- **User Management:** See the total number of connected users in each session.
- **Document Saving:** Save the session document locally in .txt format.
- **Responsive UI:** Modern interface built with JavaFX.
- **Threading & Concurrency:** Server handles up to 100 clients efficiently.

# Technologies Used

- **Java** – Core programming language
- **JavaFX** – GUI framework
- **TCP Sockets** – Client-server communication
- **Concurrency & Threads** – ThreadPool for server, dedicated client threads
- **URL Encoding** – Safe transmission of special characters
  

# Collaborative Text Editor - VS Code Setup Guide

## Prerequisites
  1. Install Java JDK 11 or above
     - Download from [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
     - Set JAVA_HOME environment variable and add bin to PATH.
     - Test in terminal:
       ```powershell
       java -version
       javac -version
       ```
  2. Install [JavaFX SDK](https://openjfx.io/)
     Extract to here (For Windows)
    ```
    C:/Software/JavaFx/javafx-sdk-21.0.2/
    ```
  3. Install VS Code and Extensions
     - VS Code: https://code.visualstudio.com/
     - Extensions:
         - Java Extensions Pack (Includes Language Support for Java, Debugger for Java)
          

   ## Project Folder Structure ##
   Make sure Project is structured like this:
   ```arduino
Collab Text Editor/
├─ Client/
│  ├─ TextEditorClient.java
│  ├─ UIManager.java
│  └─ ClientNetwork.java
├─ Server/
│  ├─ ServerMain.java
│  ├─ ClientHandler.java
│  ├─ SessionManager.java
│  └─ Session.java
└─ README.md
   ```
   Each .java file should have a package declaration matching its folder:
```java
// In TextEditorClient.java
package Client;

// In ServerMain.java
package Server;
 ```
## Installation ##

### Step 1: Open Project in VS Code ###

- Open the project root folder (Collab Text Editor) in VS Code.
- Ensure VS Code detects the Java project ( you should see “Java Projects” in Explorer ).

### Step 2: Configure JavaFX in VS Code ###
  1. Create a file named launch.json in .vscode/ folder ( if it doesn’t exist ).
  2. Add configuration for running JavaFX client:

```json
{
    // Use IntelliSense to learn about possible attributes.
    // Hover to view descriptions of existing attributes.
    // For more information, visit: https://go.microsoft.com/fwlink/?linkid=830387
    "version": "0.2.0",
    "configurations": [

        {
            "type": "java",
            "name": "Current File",
            "request": "launch",
            "mainClass": "${file}"
        },
        {
            "type": "java",
            "name": "TextEditorClient",
            "request": "launch",
            "mainClass": "Client.TextEditorClient",
            "projectName": "Collab Text Editor_16e0cf98"
        },
        {
            "type": "java",
            "name": "ServerMain",
            "request": "launch",
            "mainClass": "Server.ServerMain",
            "projectName": "Collab Text Editor_16e0cf98",
            "vmArgs": "--module-path \"C:/Software/JavaFx/javafx-sdk-21.0.2/lib\" --add-modules javafx.controls,javafx.fxml"
        }
    ]
}

```

  3. Add all .jar of javafx sdk to java project Referenced Library
     - Follow this tutorial : [Setup JavaFX and java project ](https://youtu.be/AubJaosfI-0?si=tbC_FGxb0eWVhtcP)

### Step 3: Start the Server ###
- Go to the root folder ( Collaborative Text Editor ) of the project:
  ``` PowerShell
  # go to Server directory
  cd Server
  # compile all java files
  javac *.java
  # back to root folder
  cd ..
  # run the server
  java Server.ServerMain
  ```

### Step 4: Start the Client ###
  - Go to the root folder ( Collaborative Text Editor ) of the project:
    ```PowerShell
    #go to Client directory
    cd Client
    #compile the project
    javac --module-path "C:/Software/JavaFx/javafx-sdk-21.0.2/lib" --add-modules javafx.controls,javafx.fxml *.java
    #go back to the root folder
    cd ..
    # run the Client Text editor file
    java --module-path "C:/Software/JavaFx/javafx-sdk-21.0.2/lib" --add-modules javafx.controls,javafx.fxml -cp . Client.TextEditorClient    
    ```
    
### Step 5: Connect the Client ###
- Enter :
    - Server IP: 127.0.0.1 ( or LAN IP for other machines )
    - Session ID: Any String ( e.g. ABCDE )
    - Username : Any name
-Click **Connect** - the text editor will open.

### Step 6: Optional Settings ###
- Adjust font size, save file, toggle chat, select theme from toolbar.
- Multiple clients can connect to the same session on LAN.

### Contact ###
Md. Shahriar Kabir
Email: shahriarkabir280@gmail.com





     
