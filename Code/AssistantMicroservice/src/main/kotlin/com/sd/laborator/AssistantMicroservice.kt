package com.sd.laborator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.net.ServerSocket
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class AssistantMicroservice
{

    private lateinit var  studentList: ConcurrentHashMap<String, String>
    private lateinit var messageManagerSocket: Socket
    private lateinit var messageManagerSocket1: Socket
    private lateinit var teacherSocket: ServerSocket
    companion object Constants
    {
        val HEARTBEAT_HOST = System.getenv("HEARTBEAT_HOST") ?: "localhost"
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
        const val HEARTBEAT_PORT = 1800
        const val TEACHER_PORT = 1900
    }

    private fun getImageNameWithTag(containerId: String): String
    {
        val process = ProcessBuilder("docker", "inspect", "--format", "{{.Config.Image}}", containerId).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        return reader.readLine()
    }

    private fun getContainerName(): String
    {
        val process = ProcessBuilder("hostname").start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        return reader.readLine()
    }

    private fun subscribeToTopics()
    {
        messageManagerSocket = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
        messageManagerSocket.getOutputStream().write((
                this.getImageNameWithTag(this.getContainerName()) + "\n").toByteArray())

        messageManagerSocket1 = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
        messageManagerSocket1.getOutputStream().write(("noName -1 -1\n").toByteArray())
        println("M-am conectat la MessageManagerMicroservice!")
        teacherSocket = ServerSocket(TEACHER_PORT)
        val socket = Socket(HEARTBEAT_HOST, HEARTBEAT_PORT)
        socket.getOutputStream().write(
            (this.getImageNameWithTag(this.getContainerName()) + "\n").toByteArray())
        println("M-am conectat la heartbeatMicroservice")
        studentList = ConcurrentHashMap<String, String>()
    }

    private fun receiveMessageFromTeacher(clientConnection:Socket)
    {

        val bufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))
        while(true)
        {
            val receivedMessage = bufferReader.readLine().split(" ", limit=3)
            println(receivedMessage.joinToString(" "))
            when(receivedMessage[0])
            {
                "intrebare"->
                {
                    println("Profesorul intreaba ${receivedMessage[2]}")
                    messageManagerSocket.getOutputStream().write(("intrebare ${receivedMessage[1]} " +
                            "${receivedMessage[2]}\n").toByteArray())
                }
                "raspuns"->
                {
                    println("Profesorul a raspuns la intrebare: ${receivedMessage[2]}")
                    messageManagerSocket.getOutputStream().write(("raspuns ${receivedMessage[1]} " +
                            "${receivedMessage[2]}\n").toByteArray())
                }
            }
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun receiveMessageFromMessageManager(teacherConnection:Socket)
    {
        println("Trimit cerere pentru prezenta")
        messageManagerSocket1.getOutputStream().write(("Prezenta ${this.messageManagerSocket1.localPort} " +
                "?\n").toByteArray())
        println("Cerere trimisa")

        val bufferReader = BufferedReader(InputStreamReader(messageManagerSocket.inputStream))
        while(true)
        {
            val receivedMessage = bufferReader.readLine().split(" ", limit=3)
            println(receivedMessage.joinToString(" ") + "!!!!!")
            when(receivedMessage[0])
            {
                "Prezent"->
                {
                    println("A respuns studentul ${receivedMessage[2]}")
                    if(receivedMessage[2].contains("student"))
                    {
                        synchronized(studentList)
                        {
                            studentList[receivedMessage[2]] = receivedMessage[2]
                        }
                    }
                }
                "intrebare"->
                {
                    println("A venit o intrebare de la un student ${receivedMessage[2]}")
                    val text = receivedMessage[2].split("~")
                    if(studentList.contains(text[1]))
                    {
                        println("aici")
                        teacherConnection.getOutputStream().write(("${receivedMessage[0]} " +
                                "${receivedMessage[1]} ${text[0]}\n").toByteArray())
                        teacherConnection.getOutputStream().flush()
                    }
                    else
                    {
                        println(studentList.size)
                        for(it in studentList)
                        {
                            println(it)
                        }
                        println("Studentul ${text[1]} nu face parte din lista de prezenti!!!")
                    }
                }
                "raspuns"->
                {
                    println("A venit un raspuns de la un student ${receivedMessage[2]}")
                    val text = receivedMessage[2].split("~")
                    if(studentList.contains(text[1]))
                    {
                        teacherConnection.getOutputStream().write(("${receivedMessage[0]} " +
                                "${receivedMessage[1]} ${text[0]}\n").toByteArray())
                        teacherConnection.getOutputStream().flush()
                    }
                    else
                    {
                        println("Studentul ${text[1]} nu face parte din lista de prezenti!")
                        println(studentList.size)
                        for(it in studentList)
                        {
                            println(it)
                        }
                    }
                }
            }
        }
    }

    public fun run()
    {

        this.subscribeToTopics()
        val clientConnection = teacherSocket.accept()
        val teacherConnection = teacherSocket.accept()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            this@AssistantMicroservice.receiveMessageFromTeacher(teacherConnection) }
        scope.launch {
            this@AssistantMicroservice.receiveMessageFromTeacher(clientConnection) }
        scope.launch {
            this@AssistantMicroservice.receiveMessageFromMessageManager(teacherConnection) }
        while(true)
        {

        }
    }
}

fun main(args: Array<String>)
{
    val assistantMicroservice = AssistantMicroservice()
    assistantMicroservice.run()
}

