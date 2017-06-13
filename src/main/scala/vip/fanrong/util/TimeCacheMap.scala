package vip.fanrong.util

import java.util.LinkedList

import scala.collection.mutable.HashMap

// 伴生对象
object TimeCacheMap {
  val DEFAULT_NUM_BUCKETS = 3
  val _lock: AnyRef = new Object()
}

/**
 * 时间缓存容器
 * 保存近期活跃对象，自动删除过期对象
 */
class TimeCacheMap[K, V](_callback: ExpiredCallback[K, V]) {

  var _buckets = new LinkedList[HashMap[K, V]]()
  var _cleaner: Thread = null

  def this(expirationSecs: Int, numBuckets: Int, callback: ExpiredCallback[K, V]) {
    this(callback)

    if (numBuckets < 2) throw new IllegalArgumentException("numBuckets must be >= 2")

    for (i <- 1 to numBuckets)
      _buckets.add(new HashMap[K, V])

    val expirationMillis = expirationSecs * 1000L
    val sleepTime = expirationMillis / (numBuckets - 1)

    _cleaner = new Thread(new Cleaner(sleepTime));
    _cleaner.setDaemon(true);
    _cleaner.start;

  }

  class Cleaner(sleepTime: Long) extends Runnable {
    override def run() {
      while (true) {
        var dead: HashMap[K, V] = null
        Thread.sleep(sleepTime)

        TimeCacheMap._lock.synchronized {
          dead = _buckets.removeLast()
          _buckets.addFirst(new HashMap[K, V])
        }

        if (_callback != null)
          for ((k, v) <- dead) {
            _callback.expire(k, v)
          }
      }
    }
  }

  def this(expirationSecs: Int, callback: ExpiredCallback[K, V]) {
    this(expirationSecs, TimeCacheMap.DEFAULT_NUM_BUCKETS, callback)
  }

  def this(expirationSecs: Int) {
    this(expirationSecs, TimeCacheMap.DEFAULT_NUM_BUCKETS, null)
  }

  def this(expirationSecs: Int, numBuckets: Int) {
    this(expirationSecs, numBuckets, null)
  }

  def containsKey(key: K): Boolean = {
    TimeCacheMap._lock.synchronized {
      val iter = _buckets.iterator()
      while (iter.hasNext()) {
        if (iter.next().contains(key))
          true
      }
      false
    }
  }

  def get(key: K): Option[V] = {
    TimeCacheMap._lock.synchronized {
      val iter = _buckets.iterator()
      while (iter.hasNext()) {
        val bucket = iter.next()
        if (bucket.contains(key))
          bucket.get(key)
      }
      None
    }
  }

  def put(key: K, value: V) = {
    TimeCacheMap._lock.synchronized {
      val iter = _buckets.iterator()
      var bucket = iter.next()
      bucket.put(key, value);
      while (iter.hasNext()) {
        bucket = iter.next()
        bucket.remove(key);
      }
    }
  }

  def remove(key: K): Option[V] = {
    TimeCacheMap._lock.synchronized {
      val iter = _buckets.iterator()
      while (iter.hasNext()) {
        val bucket = iter.next()
        if (bucket.contains(key))
          bucket.remove(key)
      }
      None
    }
  }

  def size(): Int = {
    TimeCacheMap._lock.synchronized {
      var size = 0
      val iter = _buckets.iterator()
      while (iter.hasNext()) {
        val bucket = iter.next()
        size += bucket.size
      }
      size
    }
  }

  def keySet: scala.collection.mutable.HashSet[K] = {
    TimeCacheMap._lock.synchronized {
      val set = new scala.collection.mutable.HashSet[K]()
      val iter = _buckets.iterator()
      while (iter.hasNext()) {
        iter.next().keySet.foreach(key => set.add(key))
      }
      set
    }
  }

  def cleanup() = {
    _cleaner.interrupt();
  }
}