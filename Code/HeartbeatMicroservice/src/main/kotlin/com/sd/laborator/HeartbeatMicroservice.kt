package com.sd.laborator

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

data class Subscriber(val socket: Socket, val name: String)

class HeartbeatMicroservice
{
    private val subscribers: ConcurrentHashMap<Int, Subscriber> = ConcurrentHashMap<Int, Subscriber>()
    private lateinit var heartbeatSocket: ServerSocket

    companion object Constants
    {
        const val HEARTBEAT_PORT = 1800
    }

    private suspend fun checkHealth()
    {
        while(true)
        {
            for(it in subscribers)
            {
                val process = ProcessBuilder("docker", "inspect", "-f", "{{.State.Running}}", it.value.name)
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().readText().trim()
                if(output != "true")
                {
                    ProcessBuilder("docker", "restart", it.value.name)
                        .redirectErrorStream(true)
                        .start()
                }
            }
            delay(5000)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    public fun run()
    {
        heartbeatSocket = ServerSocket(HEARTBEAT_PORT)
        println("HeartbeatMicroservice se executa pe portul: ${heartbeatSocket.localPort}")
        println("Se asteapta conexiuni si mesaje...")
        GlobalScope.launch { this@HeartbeatMicroservice.checkHealth() }
        while (true)
        {
            val clientConnection = heartbeatSocket.accept()
            GlobalScope.launch{
                println("Subscriber conectat: ${clientConnection.inetAddress.hostAddress}:${clientConnection.port}")
                val bufferReader = BufferedReader(InputStreamReader(clientConnection.inputStream))
                val receivedMessage = bufferReader.readLine()
                synchronized(subscribers)
                {
                    if(receivedMessage != null)
                    {
                        println("Subscriber conectat: " +
                                "${clientConnection.inetAddress.hostAddress}:${clientConnection.port}, " +
                                "cu numele $receivedMessage")
                        subscribers[clientConnection.port] = Subscriber(clientConnection, receivedMessage)
                    }
                    else
                    {
                        println("Subscriber conectat: " +
                                "${clientConnection.inetAddress.hostAddress}:${clientConnection.port}")
                        subscribers.remove(clientConnection.port)
                    }
                }
            }
        }
    }
}

fun main(args: Array<String>)
{
    val heartbeatMicroservice = HeartbeatMicroservice()
    heartbeatMicroservice.run()
}

