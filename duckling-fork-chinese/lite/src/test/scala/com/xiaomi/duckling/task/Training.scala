package com.xiaomi.duckling.task

import java.io.File

import com.xiaomi.duckling.ranking.NaiveBayesRank

object Training {
  def main(args: Array[String]): Unit = {
    println(new File("").getAbsolutePath)
    NaiveBayesRank.main(Array("src/main/resources/naive_bayes.json"))
  }
}
