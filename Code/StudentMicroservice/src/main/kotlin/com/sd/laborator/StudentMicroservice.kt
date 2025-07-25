package com.sd.laborator


import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.system.exitProcess

class StudentMicroservice
{
    // intrebarile si raspunsurile sunt mentinute intr-o lista de perechi de forma:
    // [<INTREBARE 1, RASPUNS 1>, <INTREBARE 2, RASPUNS 2>, ... ]
    private lateinit var questionDatabase: MutableList<Pair<String, String>>
    private lateinit var messageManagerSocket: Socket
    private lateinit var messageManagerSocket1: Socket
    private lateinit var interfaceMessage: ServerSocket

    init
    {
        val databaseLines: List<String> = File("questions_database.txt").readLines()
        questionDatabase = mutableListOf()
        println("Initializare StudentMicroservice")
        /*
         "baza de date" cu intrebari si raspunsuri este de forma:

         <INTREBARE_1>\n
         <RASPUNS_INTREBARE_1>\n
         <INTREBARE_2>\n
         <RASPUNS_INTREBARE_2>\n
         ...
         */
        for (i in 0..(databaseLines.size - 1) step 2) {
            questionDatabase.add(Pair(databaseLines[i], databaseLines[i + 1]))
        }
    }

    companion object Constants
    {
        // pentru testare, se foloseste localhost. pentru deploy, server-ul socket (microserviciul MessageManager) se identifica dupa un "hostname"
        // acest hostname poate fi trimis (optional) ca variabila de mediu
        val HEARTBEAT_HOST = System.getenv("HEARTBEAT_HOST") ?: "localhost"
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
        const val INTERFACE_MESSAGE_PORT = 1700
        const val HEARTBEAT_PORT = 1800
    }

    fun getImageNameWithTag(containerId: String): String
    {
        val process = ProcessBuilder("docker", "inspect", "--format", "{{.Config.Image}}", containerId).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        return reader.readLine()
    }

    fun getContainerName(): String
    {
        val process = ProcessBuilder("hostname").start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        return reader.readLine()
    }

    private fun subscribeToMessageManager()
    {
        try
        {
            messageManagerSocket = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            messageManagerSocket.getOutputStream().write((
                    this.getImageNameWithTag(this.getContainerName()) + "\n").toByteArray())
            println("Mesaj trensmis ${this.getImageNameWithTag(this.getContainerName())}")
            messageManagerSocket1 = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            messageManagerSocket1.soTimeout = 3000
            messageManagerSocket1.getOutputStream().write(("noName -1 -1\n").toByteArray())
            println("M-am conectat la MessageManager!")
            val socket = Socket(HEARTBEAT_HOST, HEARTBEAT_PORT)
            socket.getOutputStream().write(
                (this.getImageNameWithTag(this.getContainerName()) + "\n").toByteArray())
            println("M-am conectat la heartbeatMicroservice")
        }
        catch (e: Exception)
        {
            println("Nu ma pot conecta la MessageManager!")
            e.printStackTrace()
            exitProcess(1)
        }
    }

    private fun subscribeToMessageInterface()
    {
        try
        {
            interfaceMessage = ServerSocket(INTERFACE_MESSAGE_PORT)
            println("M-am conectat la interfata !")
        }
        catch (e: Exception)
        {
            println("Nu ma pot conecta la interfata grafica!")
            exitProcess(1)
        }
    }

    private fun respondToQuestion(question: String): String?
    {
        questionDatabase.forEach {
            // daca se gaseste raspunsul la intrebare, acesta este returnat apelantului
            if (it.first == question)
            {
                return it.second
            }
        }
        return null
    }

    @OptIn(DelicateCoroutinesApi::class)
    public fun speakWithStudents()
    {
        subscribeToMessageInterface()
        println("StudentMicroservice va comunica cu ceilalti pe portul: ${interfaceMessage.localPort}")
        println("Se asteapta mesaje...")
        while(true)
        {
            val clientConnection = interfaceMessage.accept()
            GlobalScope.launch {
                println("S-a primit o cerere de la: ${clientConnection.inetAddress.hostAddress}:${clientConnection.port}")
                val clientBufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))
                val response = clientBufferReader.readLine().split("~")
                println("Am primit cererea $response")
                if(response[0] == "tip2")
                {
                    println("tip2 ")
                    messageManagerSocket1.getOutputStream().write(("intrebTip2 " +
                            "${messageManagerSocket.localPort} " +
                            "${response[1]}~${this@StudentMicroservice.getImageNameWithTag(
                                this@StudentMicroservice.getContainerName())}\n").toByteArray())
                }
                else if(response[0] == "tip3")
                {
                    println("tip 3")
                    messageManagerSocket1.getOutputStream().write(("intrebTip3 " +
                            "${messageManagerSocket.localPort} " +
                            "${response[1]}~${this@StudentMicroservice.getImageNameWithTag(
                                this@StudentMicroservice.getContainerName())}\n").toByteArray())
                }
                else if(response[0] == "teacher")
                {
                    println("teacher")
                    messageManagerSocket1.getOutputStream().write(("intrebTeacher " +
                            "${messageManagerSocket.localPort} " +
                            "${response[1]}~" +
                            "${this@StudentMicroservice.getImageNameWithTag(
                                this@StudentMicroservice.getContainerName())}\n").toByteArray())
                }
                else
                {
                    println("toti ${messageManagerSocket1.localPort}")
                    messageManagerSocket1.getOutputStream().write(
                        ("intrebStudent ${messageManagerSocket1.localPort} " + "${response[1]}~" +
                                "${this@StudentMicroservice.getImageNameWithTag(
                                    this@StudentMicroservice.getContainerName())}\n").toByteArray()
                    )
                }
                val messageManagerBufferReader = BufferedReader(InputStreamReader(messageManagerSocket1.inputStream))
                try
                {
                    val receivedResponse = messageManagerBufferReader.readLine()

                    // se trimite raspunsul inapoi clientului apelant
                    println("Am primit raspunsul: \"$receivedResponse\"")
                    clientConnection.getOutputStream().write((receivedResponse + "\n").toByteArray())
                }
                catch (e: SocketTimeoutException)
                {
                    println("Nu a venit niciun raspuns in timp util.")
                    clientConnection.getOutputStream().write("Nu a raspuns nimeni la intrebare\n".toByteArray())
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    public fun run()
    {
        // microserviciul se inscrie in lista de "subscribers" de la MessageManager prin conectarea la acesta
        subscribeToMessageManager()

        println("StudentMicroservice se executa pe portul: ${messageManagerSocket.localPort}")
        println("Se asteapta mesaje...")

        val bufferReader = BufferedReader(InputStreamReader(messageManagerSocket.inputStream))

        GlobalScope.launch{this@StudentMicroservice.speakWithStudents()}

        while (true)
        {
            // se asteapta intrebari trimise prin intermediarul "MessageManager"
            val response = bufferReader.readLine()

            if (response == null)
            {
                // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                println("Microserviciul MessageService (${messageManagerSocket.port}) a fost oprit.")
                bufferReader.close()
                messageManagerSocket.close()
                break
            }

            // se foloseste un thread separat pentru tratarea intrebarii primite
            GlobalScope.launch {

                println(response)
                val (messageType, messageDestination, messageBody) = response.split(" ", limit = 3)
                println(messageBody)
                when(messageType)
                {
                    // tipul mesajului cunoscut de acest microserviciu este de forma:
                    // intrebare <DESTINATIE_RASPUNS> <CONTINUT_INTREBARE>
                    "intrebare" -> {
                        println("!!Am primit o intrebare de la $messageDestination: \"${messageBody}\"")
                        var responseToQuestion = respondToQuestion(messageBody)
                        responseToQuestion?.let {
                            responseToQuestion = "raspuns $messageDestination $it~" +
                                    this@StudentMicroservice.getImageNameWithTag(
                                        this@StudentMicroservice.getContainerName())
                            println("Trimit raspunsul: \"${response}\"")
                            messageManagerSocket.getOutputStream().write((responseToQuestion + "\n").toByteArray())
                        }
                    }
                    "grade"->
                    {
                        println("Ai primit nota $messageBody")
                    }
                    "Prezenta"->
                    {
                        messageManagerSocket1.getOutputStream().write(("Prezent $messageDestination " +
                                this@StudentMicroservice.getImageNameWithTag(
                                    this@StudentMicroservice.getContainerName()) + "\n").toByteArray())
                    }
                }
            }
        }
    }
}

fun main()
{
    val studentMicroservice = StudentMicroservice()
    studentMicroservice.run()
}