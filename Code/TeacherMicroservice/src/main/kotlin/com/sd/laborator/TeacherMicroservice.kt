package com.sd.laborator

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.system.exitProcess
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TeacherMicroservice
{
    private lateinit var messageManagerSocket: Socket
    private lateinit var messageManagerSocket1: Socket
    private lateinit var teacherMicroserviceServerSocket: ServerSocket
    private var questionDatabase: MutableList<Pair<String, String>>
    private var gradeCatalogue: MutableList<Pair<String, String>>
    private lateinit var clientConnection:Socket

    companion object Constants
    {
        // pentru testare, se foloseste localhost. pentru deploy, server-ul socket (microserviciul MessageManager) se identifica dupa un "hostname"
        // acest hostname poate fi trimis (optional) ca variabila de mediu
        val HEARTBEAT_HOST = System.getenv("HEARTBEAT_HOST") ?: "localhost"
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1900
        const val TEACHER_PORT = 1600
        const val HEARTBEAT_PORT = 1800
    }

    init
    {
        val databaseLines: List<String> = File("questions_database.txt").readLines()
        val databaseLines1: List<String> = File("grade.txt").readLines()
        questionDatabase = mutableListOf()
        gradeCatalogue = mutableListOf()
        println("Initializare TeacherMicroservice")
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
        for(i in 0 .. (databaseLines1.size-1))
        {
            val line = databaseLines1[i].split(":")
            gradeCatalogue.add(Pair(line[0], line[1]))
        }
    }

    private fun subscribeToMessageManager()
    {
        try
        {
            messageManagerSocket = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            println("1$MESSAGE_MANAGER_HOST\t$MESSAGE_MANAGER_PORT")
            messageManagerSocket.getOutputStream().write((
                    this.getImageNameWithTag(this.getContainerName()) + "\n").toByteArray())
            println("2$MESSAGE_MANAGER_HOST\t$MESSAGE_MANAGER_PORT")
            messageManagerSocket1 = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            println(this.getImageNameWithTag(this.getContainerName()))
            messageManagerSocket1.getOutputStream().write(("noName -1 -1\n").toByteArray())
            println("3$MESSAGE_MANAGER_HOST\t$MESSAGE_MANAGER_PORT")
            messageManagerSocket.soTimeout = 3100
            println("M-am conectat la AssistantMicroservice!")
            val socket = Socket(HEARTBEAT_HOST, HEARTBEAT_PORT)
            socket.getOutputStream().write(
                (this.getImageNameWithTag(this.getContainerName()) + "\n").toByteArray())
            println("M-am conenctat la heartbeatMicroservice")
        }
        catch (e: Exception)
        {
            println("Nu ma pot conecta la MessageManager! ${e.message}")
            exitProcess(1)
        }
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

    fun receiveQuestion()
    {
        val bufferReader = BufferedReader(InputStreamReader(messageManagerSocket1.inputStream))
        while(true)
        {
            val receivedQuestion = bufferReader.readLine()
            if (receivedQuestion != null)
            {
                val (messageType, messageDestination, messageBody) = receivedQuestion.split(" ", limit = 3)
                when(messageType)
                {
                    "intrebare"-> {
                        println("!!Am primit o intrebare de la $messageDestination: \"${messageBody}\"")
                        var responseToQuestion = respondToQuestion(messageBody)

                        responseToQuestion?.let {
                            responseToQuestion = "raspuns $messageDestination $it"
                            println("Trimit raspunsul: \"${responseToQuestion}\"")
                            messageManagerSocket1.getOutputStream().write((responseToQuestion + "\n").toByteArray())
                        }
                    }
                    "raspuns"->{
                        println("Am primit raspunsul $messageBody")
                        clientConnection.getOutputStream().write((messageBody + "\n").toByteArray())
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    public fun run()
    {
        // microserviciul se inscrie in lista de "subscribers" de la MessageManager prin conectarea la acesta
        subscribeToMessageManager()

        // se porneste un socket server TCP pe portul 1600 care asculta pentru conexiuni
        teacherMicroserviceServerSocket = ServerSocket(TEACHER_PORT)
        GlobalScope.launch{this@TeacherMicroservice.receiveQuestion()}.start()

        println("TeacherMicroservice se executa pe portul: ${teacherMicroserviceServerSocket.localPort}")
        println("Se asteapta cereri (intrebari)...")

        while (true)
        {
            // se asteapta conexiuni din partea clientilor ce doresc sa puna o intrebare
            // (in acest caz, din partea aplicatiei client GUI)
            clientConnection = teacherMicroserviceServerSocket.accept()

            // se foloseste un thread separat pentru tratarea fiecarei conexiuni client
            GlobalScope.launch {
                println("S-a primit o cerere de la: ${clientConnection.inetAddress.hostAddress}:${clientConnection.port}")

                // se citeste intrebarea dorita
                val clientBufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))
                val receivedQuestion = clientBufferReader.readLine()
                if (receivedQuestion.contains("grade:"))
                {
                    println("Trimit catre MessageManager: ${"grade " + 
                        "${messageManagerSocket.localPort} $receivedQuestion\n"}")
                    messageManagerSocket.getOutputStream()
                        .write(("grade ${messageManagerSocket.localPort} $receivedQuestion\n").toByteArray())
                    val file = File("grade.txt")
                    var lines = file.readLines().toMutableList()
                    val sp = receivedQuestion.split(":")
                    when(sp[1])
                    {
                        "tip1"->{lines[0] = lines[0] + ", ${sp[2]}"}
                        "tip2"->{lines[1] = lines[1] + ", ${sp[2]}"}
                        "tip3"->{lines[2] = lines[2] + ", ${sp[2]}"}
                    }
                    println(lines.joinToString ("\n" ))
                    file.writeText(lines.joinToString ("\n" ))
                }
                else
                {
                    // intrebarea este redirectionata catre microserviciul MessageManager
                    println("Trimit catre MessageManager: ${"intrebare " +
                            "${messageManagerSocket.localPort} $receivedQuestion\n"}")
                    messageManagerSocket.getOutputStream()
                        .write(("intrebare ${messageManagerSocket.localPort} $receivedQuestion\n").toByteArray())

                    // se asteapta raspuns de la MessageManager
                    val messageManagerBufferReader =
                        BufferedReader(InputStreamReader(messageManagerSocket.inputStream))
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
                        clientConnection.getOutputStream().write(
                            "Nu a raspuns nimeni la intrebare\n".toByteArray())
                    }
                    finally
                    {
                        // se inchide conexiunea cu clientul
                        clientConnection.close()
                    }
                }
            }
        }
    }
}

fun main()
{
    val teacherMicroservice = TeacherMicroservice()
    teacherMicroservice.run()
}