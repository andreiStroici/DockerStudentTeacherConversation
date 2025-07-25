package com.sd.laborator

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

data class Subscriber(val socket: Socket, val name: String)

class MessageManagerMicroservice
{
    private val subscribers: HashMap<Int, Subscriber>
    private lateinit var messageManagerSocket: ServerSocket

    companion object Constants
    {
        const val MESSAGE_MANAGER_PORT = 1500
        const val HEARTBEAT_PORT = 1800
        var HEARTBEAT_HOST = System.getenv("HEARTBEAT_HOST") ?: "localhost"
    }

    init
    {
        subscribers = hashMapOf()

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

    private fun broadcastMessage(message: String, except: Int)
    {
        subscribers.forEach {
            it.takeIf { it.key != except }
                ?.value?.socket?.getOutputStream()?.write((message + "\n").toByteArray())
        }
    }

    private fun respondTo(destination: Int, message: String)
    {
        println(destination)
        subscribers[destination]?.socket?.getOutputStream()?.write((message + "\n").toByteArray())
    }

    @OptIn(DelicateCoroutinesApi::class)
    public fun run()
    {
        // se porneste un socket server TCP pe portul 1500 care asculta pentru conexiuni
        messageManagerSocket = ServerSocket(MESSAGE_MANAGER_PORT)
        println("MessageManagerMicroservice se executa pe portul: ${messageManagerSocket.localPort}")
        println("Se asteapta conexiuni si mesaje...")
        val socket = Socket(HEARTBEAT_HOST, HEARTBEAT_PORT)
        println(this.getImageNameWithTag(this.getContainerName()))
        socket.getOutputStream().write(
            (this.getImageNameWithTag(this.getContainerName()) + "\n").toByteArray())
        println("M-am conenctat la heartbeatMicroservice")
        while (true)
        {
            // se asteapta conexiuni din partea clientilor subscriberi
            val clientConnection = messageManagerSocket.accept()
            // se porneste un thread separat pentru tratarea conexiunii cu clientul
            GlobalScope.launch {
                println("Subscriber conectat: ${clientConnection.inetAddress.hostAddress}:${clientConnection.port}")


                // adaugarea in lista de subscriberi trebuie sa fie atomica!
                val bufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))
                var receivedMessage = bufferReader.readLine()
                synchronized(subscribers)
                {
                    if(receivedMessage != "noName")
                    {
                        subscribers[clientConnection.port] = Subscriber(clientConnection, receivedMessage)
                        println("Mai exact $receivedMessage")
                    }

                }

                while (true)
                {
                    // se citeste raspunsul de pe socketul TCP
                    receivedMessage = bufferReader.readLine()

                    // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                    if (receivedMessage == null)
                    {
                        // deci subscriber-ul respectiv a fost deconectat
                        println("Subscriber-ul ${clientConnection.port} a fost deconectat.")
                        synchronized(subscribers) {
                            subscribers.remove(clientConnection.port)
                        }
                        bufferReader.close()
                        clientConnection.close()
                        break
                    }

                    println("Primit mesaj: $receivedMessage")
                    val (messageType, messageDestination, messageBody) = receivedMessage.split(" ", limit = 3)

                    when (messageType) {
                        "intrebare" -> {
                            // tipul mesajului de tip intrebare este de forma:
                            // intrebare <DESTINATIE_RASPUNS> <CONTINUT_INTREBARE>
                            broadcastMessage(
                                "intrebare ${clientConnection.port} $messageBody",
                                except = clientConnection.port
                            )
                        }

                        "raspuns" -> {
                            // tipul mesajului de tip raspuns este de forma:
                            // raspuns <CONTINUT_RASPUNS>
                            respondTo(messageDestination.toInt(), "raspuns " +
                                    "$messageDestination $messageBody")
                        }

                        "intrebStudent" -> {
                            broadcastMessage(
                                "intrebare ${clientConnection.port} $messageBody",
                                except = clientConnection.port
                            )
                        }

                        "intrebTip1" -> {
                            respondTo(
                                subscribers.values.find {
                                    it.name == "localhost:5000/student_microservice:tip1"
                                }!!.socket.port,
                                "intrebare ${clientConnection.port} $messageBody"
                            )
                        }

                        "intrebTip2" -> {
                            respondTo(
                                subscribers.values.find {
                                    it.name == "localhost:5000/student_microservice:tip2"
                                }!!.socket.port,
                                "intrebare ${clientConnection.port} $messageBody"
                            )
                        }

                        "intrebTip3" -> {
                            respondTo(
                                subscribers.values.find {
                                    it.name == "localhost:5000/student_microservice:tip3"
                                }!!.socket.port,
                                "intrebare ${clientConnection.port} $messageBody"
                            )
                        }

                        "intrebTeacher" -> {
                            respondTo(
                                subscribers.values.find {
                                    it.name == "localhost:5000/assistant_microservice:v1"
                                }!!.socket.port,
                                "intrebare ${clientConnection.port} $messageBody"
                            )
                        }

                        "grade" -> {
                            val (op, st, gr) = messageBody.split(":", limit = 3)
                            when (st) {
                                "tip1" -> {
                                    print("Tip 1")
                                    respondTo(
                                        subscribers.values.find {
                                            it.name == "localhost:5000/student_microservice:tip1"
                                        }!!.socket.port,
                                        "grade ${clientConnection.port} $gr"
                                    )
                                }

                                "tip2" -> {
                                    print("tip 2")
                                    respondTo(
                                        subscribers.values.find {
                                            it.name == "localhost:5000/student_microservice:tip2"
                                        }!!.socket.port,
                                        "grade ${clientConnection.port} $gr"
                                    )
                                }

                                "tip3" -> {
                                    print("tip 3")
                                    respondTo(
                                        subscribers.values.find {
                                            it.name == "localhost:5000/student_microservice:tip3"
                                        }!!.socket.port,
                                        "grade ${clientConnection.port} $gr"
                                    )
                                }
                            }
                        }

                        "Prezenta" ->
                        {
                            println("Cerere prezenta")
                            broadcastMessage("Prezenta $messageDestination ?", clientConnection.port)
                        }

                        "Prezent"->
                        {
                            respondTo(subscribers.values.find {
                                it.name == "localhost:5000/assistant_microservice:v1"
                            }!!.socket.port, "Prezent $messageDestination $messageBody")
                        }
                    }
                }
            }
        }
    }
}

fun main()
{
    val messageManagerMicroservice = MessageManagerMicroservice()
    messageManagerMicroservice.run()
}
