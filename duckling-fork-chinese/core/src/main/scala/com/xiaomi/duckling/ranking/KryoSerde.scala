package com.xiaomi.duckling.ranking

import java.io.{FileInputStream, FileOutputStream, InputStream}

import com.esotericsoftware.kryo.Kryo
import com.twitter.chill.{Input, KryoSerializer, Output}

import com.xiaomi.duckling.ranking.Bayes.Classifier

object KryoSerde {
  private val kryo: Kryo = KryoSerializer.registered.newKryo()

  def makeSerializedFile[T](o: T, file: String): Unit = {
    val output = new Output(new FileOutputStream(file))
    kryo.writeClassAndObject(output, o)
    output.close()
  }

  def loadSerializedFile[T](file: String, clazz: Class[T]): T = {
    val input = new Input(new FileInputStream(file))
    val out = kryo.readClassAndObject(input).asInstanceOf[T]
    input.close()
    out
  }

  def loadSerializedResource[T](in: InputStream, clazz: Class[T]): T = {
    val input = new Input(in)
    val out = kryo.readClassAndObject(input).asInstanceOf[T]
    input.close()
    out
  }
}
