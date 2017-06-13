package vip.fanrong.util

import org.junit._
import Assert._
@Test
class TimeCacheMapTest {
  @Test def test() = {
    val map = new TimeCacheMap[String, String](1, 2, new ExpiredCallback[String, String]() {
      override def expire(k: String, v: String) = {
        println("[" + k + "] expired.")
      }
    })

    map.put("A", "aaaa")
    assertEquals(1, map.size())
    Thread sleep 3000
    println("main thread sleeped 3000")

    assertEquals(0, map.size())

  }
}