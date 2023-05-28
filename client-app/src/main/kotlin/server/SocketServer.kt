import ServerProtocol.SendEvent
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.DataInputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class SocketServer(val port: Int=PORT) {
    val serverSocket = ServerSocket(port)

    suspend fun runServer()= withContext(Dispatchers.IO){
        suspendCancellableCoroutine<Unit> {
            it.invokeOnCancellation { serverSocket.close() }
            runCatching {
                while (it.isActive){
                    val socket=serverSocket.accept()
                    serverThreads+=ServerThread(socket).apply {
                        start()
                    }
                }
            }.onFailure { throwable ->
                if (throwable is SocketException){
                    throwable.printStackTrace()
                }else throw throwable
            }
            println("Server Shutdown")
        }

    }

    private val callbacks= mutableListOf<suspend (event:Event,id:Long)->Unit>()
    fun onEventReceive( block:suspend (event:Event,id:Long)->Unit){
        callbacks.add(block)
    }
    val serverThreads= mutableListOf<ServerThread>()
    fun stop() {
        serverThreads.forEach { it.interrupt() }
        serverSocket.close()
    }

    val coroutineScope= CoroutineScope(Dispatchers.Default)
    inner class ServerThread(private val socket: Socket) : Thread() {
        var timeoutJob:Job=timeout(TIMEOUT){
            interrupt()
        }
        @OptIn(ExperimentalSerializationApi::class)
        override fun run() {
            println("Ready")
            val sin = DataInputStream(socket.getInputStream())
            println("Connected")
            while (this.isAlive) {
                if (sin.available() >= HeaderSize) {
                    val size =sin.readInt()
                    print("Size=$size:")
                    val data=ProtoBuf.decodeFromByteArray<ServerProtocol>(sin.readNBytes(size))
                    if (data is SendEvent){

                        callbacks.forEach { coroutineScope.launch { it(data.event,id) } }
                    }else if(data is ServerProtocol.End){
                        interrupt()
                    }
                    timeoutJob.cancel()
                    timeoutJob=timeout(TIMEOUT){
                        socket.close()
                    }
                    println(data)
                }
                if (socket.isClosed){
                    break
                }
            }
            println("End Socket")
        }
        inline fun timeout(timeoutMillis:Long, crossinline block:()->Unit): Job {
            return coroutineScope.launch {
                delay(timeoutMillis)
                block()
            }
        }

        override fun interrupt() {
            timeoutJob.cancel()
            socket.getOutputStream().apply {
                write(convertByteArray(ServerProtocol.End))
                flush()
            }
            socket.close()
            callbacks.forEach { coroutineScope.launch { it(Event.CloseProject,id) } }
            super.interrupt()
        }
    }
}

const val TIMEOUT:Long=5000*60


private fun main() {
    runBlocking {
        SocketServer().runServer()
    }
}