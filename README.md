# PubSub_with_Raft_in_Scala
CS 6376 Foundations of Hybrid and Embedded Systems Final Project

## About Member

#### Young-jae Moon
* M.Sc. in computer science and Engineering Graduate Fellowship recipient at Vanderbilt University (January 2023 - December 2024).
* Email: youngjae.moon@Vanderbilt.Edu

## Advisor

#### Professor Abhishek Dubey
* Associate Professor in the Computer Science department and an affiliated faculty member in the Electrical and Computer Engineering department at Vanderbilt University
* Email: abhishek.dubey@Vanderbilt.edu

## Acknowledgements
* Thanks to Professor Aniruddha Gokhale for teaching me CS 6381 Distributed Systems Principles.
* Thanks to Professor Taylor Johnson for teaching me CS 6315 Automated Verification.
* Thanks to Professor Abhishek Dubey for teaching me CS 6376 Foundations of Hybrid and Embedded Systems.

## About the project
### Original goals of the project
1. Define the Raft consensus algorithm for distributed systems in Petri-nets, establishing a robust framework for system interactions.
2. Generate the unoptimized code in Scala.
3. Perform code verification and optimization using the Stainless tool, enhancing system efficiency and reliability.
4. Compare the performance of the unoptimized Scala code with optimized versions.
5. Describe the testing methodology employed to ensure the correctness and robustness of the system.
6. Highlight the potential for horizontal scaling through Raft’s leader election mechanism and log replication.


### Modified goals of the project
1. Develop and implement a Publish/Subscribe architecture utilizing Petri nets and the Scala programming language. This architecture leverages the Raft algorithm for the consensus protocol to achieve robust and scalable message delivery.
2. Model a portion of the software system as Petri nets. This will enable the verification of the model's correctness, ensuring the system's functionality.
3. Generate the initial, unoptimized Scala code in the RPC design pattern. This approach promotes a cleaner separation of concerns, improves anonymity and fault tolerance, and ultimately makes the system more robust and scalable.

### Future work
* The 3-6th original goal will be done as future work (extension) during the Winter 2024 break with Professor Abhishek Dubey.
* More complex testing methods (e.g., writing unit tests, setting up Docker, Kubernetes) can also be implemented during the winter break.
* Thus, another project - "Automatic Scala code generation from Petri-Nets" - will be initiated in Spring 2024 as my CS 5278 Principles of Software Engineering (taught by Professor Yu Huang) final project.
* I have realised that I have initially proposed a project that can be divided into three sub-projects that are still very heavy, so I have split it up into three.

## Motivation of the project
* Programming in Java takes a lot of time than other JVM languages.
* I have realized that it is possible to code differently, resulting in a code that is as efficient as Java.
* My SWE methodology is different, though it is inspired from Xiaoming Liu et al.'s "Building reliable, high-performance communication systems from components" paper.
* Have chosen the Raft consensus algorithm in distributed systems, as implementing consensus algorithms for distributed systems is difficult in programming languages such as Java.

## Technologies Used
1. Petri-nets
2. Scala
3. RabbitMQ
4. Protobuf
5. IntelliJ IDEA

## Instructions for checking out the latest stable version

### Method 1:
1. Open the main page for our GitHub repository: https://github.com/Pingumaniac/PubSub_with_Raft_in_Scala
2. Click the following button: <img src = "https://user-images.githubusercontent.com/63883314/115416097-69ade280-a232-11eb-8401-8c41362ab4c2.png" width="44" height="14">
3. Click the 'Download ZIP' option.
4. Unzip the folder.

### Method 2:
1.  Copy the web URL to your clipboard.
2.  Open 'Git Bash' from your local computer. You must have installed Git from the following page to do this: https://git-scm.com/downloads
3.  Move to the preferred directory.
4.  Next, type the following:
```
git clone https://github.com/Pingumaniac/PubSub_with_Raft_in_Scala.git
```
5. All the codes and documents in the repository can be accessed.

Note: For Method 2, if you have already cloned the repository before, you can skip the first two steps. And type this instead for step 4:
```
git type
```

## How to set up for running this software

### 1. Please make sure you have downloaded Scala.

* Here is the URL for downloading the Scala installer for Windows, macOS, Linux, and others:
https://www.scala-lang.org/download/
* To install Linux on Windows so that you can execute Scala through Linux on Windows, please download Windows Subsystem for Linux (WSL):
https://docs.microsoft.com/en‐us/windows/wsl/install

### 2. Install Erlang on your computer.
* As RabbitMQ is implemented in Erlang, you must download Erlang to your computer.
* Please download the latest stable version of Erlang from the following website: https://www.erlang.org/downloads

### 3. Install RabbitMQ on your computer.

#### Windows
* First, install Chocolatey from the following website: https://chocolatey.org/install
* Or you can type the following in the Powershell:
```
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```
* Second, install RabbitMQ by typing the following command in the Powershell:
```
choco install rabbitmq
```
* Third, visit this site: https://www.rabbitmq.com/install-windows.html#installer. Then, install the RabbitMQ server by downloading the following file:
```
rabbitmq-server-3.12.10.exe
```
This will install the RabbitMQ server on Windows.

#### macOS
* Follow the instructions from the following website: https://www.rabbitmq.com/install-homebrew.html
* Or open the terminal and enter the following commands:
```
brew update
brew upgrade
brew install rabbitmq
```
If that directory is not in PATH, it is recommended to append it.
* For macOS Intel, enter the following command in the terminal:
```
export PATH=$PATH:/usr/local/sbin
```
* For Apple Silicon, enter the following command in the terminal:
```
export PATH=$PATH:/opt/homebrew/sbin
```
### 4. Install the following Scala packages.

#### For Windows, run the following commands in the CLI, Powershell or Git Bash terminal:
```
sbt addSbtPlugin "com.thesamet" % "sbt-protoc" % "1.6"
```
#### For macOS, run the following commands in the Terminal:
```
sbt addSbtPlugin "com.thesamet" % "sbt-protoc" % "1.6"
```

### 5. Install IntelliJ with the Scala plugin.
* Please visit the following website for more detailed instructions: https://www.jetbrains.com/help/idea/discover-intellij-idea-for-scala.html

## Bug tracking

* All users can view and report a bug in the "GitHub Issues" of our repository.
* Here is the URL for viewing and reporting a list of bugs: https://github.com/Pingumaniac/PubSub_with_Raft_in_Scala/issues
