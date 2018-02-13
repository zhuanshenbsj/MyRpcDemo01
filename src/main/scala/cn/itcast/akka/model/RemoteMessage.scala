package cn.itcast.akka.model

//传递如果走网络，需要继承序列化接口
trait RemoteMessage extends Serializable

case class RegisterWorker(id: String, memory: Int, cores: Int) extends RemoteMessage

case class RegisteredWorker(masterUrl: String) extends RemoteMessage

//Worker 2 Master，不通过网络不需要序列化
case object SendHeartbeat

//Worker 2 Master
case class Heartbeat(id:String) extends RemoteMessage

// Master 2 Master
case object CheckTimeOutWorker