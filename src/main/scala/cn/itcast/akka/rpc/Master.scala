package cn.itcast.akka.rpc

import akka.actor.{Actor, ActorSystem, Props}
import cn.itcast.akka.model._
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.concurrent.duration._

//注意导包是akka.actor.Actor
class Master(val host: String, val port: Int) extends Actor {

  val workersMap = new mutable.HashMap[String, WorkerInfo]()

  val workersSet = new mutable.HashSet[WorkerInfo]()
  //超时检测间隔
  val Check_Interval = 15000
  println("constractor invoked")

  override def preStart(): Unit = {
    println("preStart invoked")
    import context.dispatcher //导入隐式转换
    context.system.scheduler.schedule(0 millis, Check_Interval millis, self, CheckTimeOutWorker)
  }

  //用于接收消息（偏函数）
  override def receive: Receive = {
    case RegisterWorker(id, memory, cores) => {
      if (!workersMap.contains(id)) {
        //把worker的信息封装起来，保存到内存当中
        val workerInfo = new WorkerInfo(id, memory, cores)
        workersMap += ((id, workerInfo))
        workersSet += workerInfo
        sender ! RegisteredWorker(s"akka.tcp://masterSystem@$host:$port/user/Master")
      }
    }
    case Heartbeat(workerID) => {
      if (workersMap.contains(workerID)) {
        val workerInfo = workersMap(workerID)
        //报活
        val currentTime = System.currentTimeMillis()
        workerInfo.lastHeartBeatTime = currentTime
      }
    }
    case CheckTimeOutWorker => {
      val currentTime = System.currentTimeMillis()
      val toRemove = workersSet.filter(x => currentTime - x.lastHeartBeatTime > Check_Interval)
      for (w <- toRemove) {
        workersMap -= w.id
        workersSet -= w
      }
      println(workersSet.size)
    }

  }

}

object Master {
  def main(args: Array[String]): Unit = {

    val host = args(0)
    val port = args(1).toInt
    //准备配置
    val configStr =
      s"""
         |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname = "$host"
         |akka.remote.netty.tcp.port = "$port"
       """.stripMargin
    val config = ConfigFactory.parseString(configStr)
    //ActorSystem是老大，负责创建和监控下面的Actor，并且他是单例的
    val actorSystem = ActorSystem("masterSystem", config)
    //创建actor
    val master = actorSystem.actorOf(Props(new Master(host, port)), "Master")
    master ! "hello"
    actorSystem.awaitTermination()

  }
}
