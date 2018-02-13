package cn.itcast.akka.rpc

import java.util.UUID

import akka.actor.{Actor, ActorSelection, ActorSystem, Props}
import cn.itcast.akka.model.{Heartbeat, RegisterWorker, RegisteredWorker}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps

class Worker(val masterHost: String, val masterPort: Int, val memory: Int, val cores: Int) extends Actor {

  var master: ActorSelection = _
  val workID = UUID.randomUUID().toString()
  val Check_Interval: Int = 10000

  //在preStart方法中向Master建立链接
  override def preStart(): Unit = {
    //与Master建立连接
    master = context.actorSelection(s"akka.tcp://masterSystem@$masterHost:$masterPort/user/Master")
    //向Master发送注册消息
    master ! RegisterWorker(workID, memory, cores)
  }

  override def receive: Receive = {
    case RegisteredWorker(masterUrl) => {
      println(masterUrl)
      //启动定时器发送心跳
      import context.dispatcher
      //导入隐式转换
      context.system.scheduler.schedule(0 millis, Check_Interval millis, self, Heartbeat(workID))
    }
    case Heartbeat(workID) => {
      println("send heartbeat to master")
      master ! Heartbeat(workID)
    }
  }
}

object Worker {
  def main(args: Array[String]): Unit = {
    val host = args(0)
    val port = args(1).toInt
    val masterHost = args(2)
    val masterPort = args(3).toInt
    val memory = args(4).toInt
    val cores = args(5).toInt
    //准备配置
    val configStr =
      s"""
         |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname = "$host"
         |akka.remote.netty.tcp.port = "$port"
       """.stripMargin
    val config = ConfigFactory.parseString(configStr)
    //ActorSystem是老大，负责创建和监控下面的Actor，并且他是单例的
    val actorSystem = ActorSystem("WorkerSystem", config)
    actorSystem.actorOf(Props(new Worker(masterHost, masterPort, memory, cores)), "Worker")
    actorSystem.awaitTermination()
  }
}

