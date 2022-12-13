package com.xiaomi.duckling.ranking

import java.io.{FileInputStream, FileOutputStream, InputStream}

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.io.{Input, Output}
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.SerializingInstantiatorStrategy

import com.xiaomi.duckling.ranking.Bayes.Classifier

object KryoSerde {
  private val kryo: Kryo = {
    val kryo = new Kryo()
    kryo.register(classOf[java.util.HashMap[String, Classifier]])
    kryo.register(classOf[com.xiaomi.duckling.ranking.Bayes.Classifier])
    kryo.register(classOf[com.xiaomi.duckling.ranking.Bayes.ClassData])
    kryo.setInstantiatorStrategy(new SerializingInstantiatorStrategy())
    kryo
  }

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
