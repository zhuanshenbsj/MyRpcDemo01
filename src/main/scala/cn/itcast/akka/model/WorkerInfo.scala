package cn.itcast.akka.model

private[akka] class WorkerInfo(val id: String, val memory: Int, val cores: Int) {
  var lastHeartBeatTime: Long = _
}
