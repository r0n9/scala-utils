package vip.fanrong.util

// 回调接口
trait ExpiredCallback[K, V] {
  def expire(k: K, v: V);
}