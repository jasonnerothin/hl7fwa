package com.epam

/**
  */
trait NapTime {

  private val rand = scala.util.Random

  def zzz(maxMillis: Int = 0): Unit = {
    if (maxMillis == 0) Thread.sleep(rand.nextInt(256))
    else Thread.sleep(rand.nextInt(maxMillis))
  }

}
